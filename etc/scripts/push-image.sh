#!/bin/bash -e
#
# Copyright (c) 2020 Oracle and/or its affiliates. All rights reserved.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -o pipefail || true  # trace ERR through pipes
set -o errtrace || true # trace ERR through commands and functions
set -o errexit || true  # exit the script if any statement returns a non-true return value

on_error(){
    if [ ! -z "${STDERR}" ] && [ -e ${STDERR} ] && [ $(wc -l ${STDERR} | awk '{print $1}') -gt 0 ] ; then
        echo "---------------------------------------"
        tail -100 ${STDERR}
        echo "---------------------------------------"
    fi
}
trap on_error ERR

# Path to this script
if [ -h "${0}" ] ; then
    readonly SCRIPT_PATH="$(readlink "${0}")"
else
    readonly SCRIPT_PATH="${0}"
fi
readonly SCRIPT_DIR=$(dirname ${SCRIPT_PATH})
readonly SCRIPT=$(basename ${SCRIPT_PATH})

usage(){
  cat <<EOF

DESCRIPTION: Push an image tar to a remote registry

USAGE:

$(basename ${SCRIPT}) [OPTIONS] --registry-URL=URL --image=PATH

  --registry-url=URL
          Registry URL to push to.

  --name=NAME
          Qualified image name (with namespace).

  --tag=TAG
          Image tag.

  --image=PATH
          Path to the image tar to be pushed.


OPTIONS:

  --user=USERNAME
          Registry username

  --password=PASSWORD
          Registry password

  --v
          Print debug output.

  --vv
          Print debug output and set -x

  --help
          Prints the usage and exits.

EOF
}

# parse command line args
ARGS=( "${@}" )
for ((i=0;i<${#ARGS[@]};i++))
{
    ARG=${ARGS[${i}]}
    case ${ARG} in
    "--help")
        usage
        exit 0
        ;;
    "--v")
        readonly DEBUG=true
        ;;
    "--vv")
        readonly DEBUG2=true
        ;;
    "--registry-url="*)
        readonly REGISTRY_URL=${ARG#*=}
        ;;
    "--name="*)
        readonly IMAGE_NAME=${ARG#*=}
        ;;
    "--tag="*)
        readonly IMAGE_TAG=${ARG#*=}
        ;;
    "--image="*)
        readonly IMAGE_TAR=${ARG#*=}
        ;;
    "--user="*)
        readonly UNAME=${ARG#*=}
        ;;
    "--password="*)
        readonly UPASSWD=${ARG#*=}
        ;;
    *)
        echo "ERROR: unknown option: ${ARG}"
        usage
        exit 1
        ;;
  esac
}

if [ -z "${IMAGE_TAR}" ] ; then
    echo "ERROR: --image option is required"
    usage
    exit 1
elif [ -z "${IMAGE_NAME}" ] ; then
    echo "ERROR: --name option is required"
    usage
    exit 1
elif [ -z "${IMAGE_TAG}" ] ; then
    echo "ERROR: --tag option is required"
    usage
    exit 1
elif [ ! -f "${IMAGE_TAR}" ] ; then
    echo "ERROR: ${IMAGE_TAR} is not a valid file"
    exit 1
fi

if [ -z "${REGISTRY_URL}" ] ; then
    echo "ERROR: --registry-url option is required"
    usage
    exit 1
fi

if ! type jq > /dev/null 2>&1; then
    echo "ERROR: jq not found in PATH"
    exit 1
fi

if ! type curl > /dev/null 2>&1; then
    echo "ERROR: curl not found in PATH"
    exit 1
fi

readonly STDERR=$(mktemp -t XXX-stderr)

if [ -z "${DEBUG}" ] ; then
    if [ -z "${DEBUG2}" ] ; then
        exec 2> ${STDERR}
    fi
    readonly DEBUG=false
fi

if [ ! -z "${DEBUG2}" ] ; then
    set -x
else
    exec 2> ${STDERR}
    readonly DEBUG2=false
fi

echo "INFO: pushing image..."

readonly API_URL_BASE="${REGISTRY_URL}/${IMAGE_NAME}"
echo "INFO: image_name=${IMAGE_NAME}"
echo "INFO: image_tag=${IMAGE_TAG}"

echo "INFO: authenticating..."
if ${DEBUG} ; then
    AUTH_EXTRA_OPTS="--v"
elif ${DEBUG2} ; then
    AUTH_EXTRA_OPTS="--vv"
fi
if [ ! -z "${UNAME}" ] && [ ! -z "${UPASSWD}" ] ; then
    AUTH_EXTRA_OPTS="${AUTH_EXTRA_OPTS} --user=${UNAME} --password=${UPASSWD}"
fi
readonly TOKEN_FILE=$(mktemp -t XXX-token})
bash ${BASH_OPTS} ${SCRIPT_DIR}/registry-auth.sh ${AUTH_EXTRA_OPTS} \
    --registry-url=${REGISTRY_URL} \
    --repository=${IMAGE_NAME} \
    --scopes="push,pull" \
    --output-file=${TOKEN_FILE}

readonly TOKEN=$(cat ${TOKEN_FILE})
readonly AUTH_HEADER="Authorization: Bearer ${TOKEN}"

get_header(){
    grep -i "${1}: " ${2} | awk '{print $2}' | tr -d '\r'
}

get_location() {
    local location=$(get_header "Location" ${1})
    if ! [[ ${location} =~ ^${REGISTRY_URL} ]] ; then
        if [[ ${REGISTRY_URL} =~ /v2$ ]] ; then
            location=${REGISTRY_URL:0:$((${#REGISTRY_URL}-3))}${location}
        else
            location=${REGISTRY_URL}${location}
        fi
    fi
    echo ${location}
}

create_blob(){
    local responseHeaders=$(mktemp -t XXX})
    curl -v -X POST \
        -D ${responseHeaders} \
        -H "${AUTH_HEADER}" \
       "${API_URL_BASE}/blobs/uploads/"
    local status=$(head -1 ${responseHeaders})
    if [[ ${status} =~ ^HTTP/1.1\ 202 ]] ; then
        UPLOAD_LOCATION=$(get_location ${responseHeaders})
    else
        echo "ERROR: ${status}"
        return 1
    fi
}

blob_size(){
    local blobLocation=$(get_location ${RESPONSE_HEADERS})
    curl -v -L --head -H "${AUTH_HEADER}" "${blobLocation}" > ${RESPONSE_HEADERS}
    BLOB_SIZE=$(get_header "Content-Length" ${RESPONSE_HEADERS})
}

readonly IMAGE_MANIFEST="$(mktemp -t XXX}).json"
tar --to-stdout -xf ${IMAGE_TAR} manifest.json > ${IMAGE_MANIFEST}
echo "INFO: image_manifest=${IMAGE_MANIFEST}"

readonly IMAGE_CONFIG="$(mktemp -t XXX}).json"
readonly IMAGE_CONFIG_ENTRY=$(jq -r '.[0].Config' ${IMAGE_MANIFEST})
tar --to-stdout -xf ${IMAGE_TAR} ${IMAGE_CONFIG_ENTRY} > ${IMAGE_CONFIG}
echo "INFO: image_config=${IMAGE_CONFIG}"

readonly IMAGE_CONFIG_DIGEST="sha256:${IMAGE_CONFIG_ENTRY%%.json}"
readonly LAYER_DIGEST=$(jq -r '.rootfs.diff_ids[0]' ${IMAGE_CONFIG})
echo "INFO: image_config_digest=${IMAGE_CONFIG_DIGEST}"
echo "INFO: layer_digest=${LAYER_DIGEST}"

RESPONSE_HEADERS=$(mktemp -t XXX})

# upload layer
create_blob
if [[ "${UPLOAD_LOCATION}" =~ \? ]] ; then
    UPLOAD_URL="${UPLOAD_LOCATION}&digest=${LAYER_DIGEST}"
else
    UPLOAD_URL="${UPLOAD_LOCATION}?digest=${LAYER_DIGEST}"
fi
tar --to-stdout -xf "${IMAGE_TAR}" $(jq -r '.[0].Layers[0]' ${IMAGE_MANIFEST}) | \
    curl -v -X PUT --data-binary @- \
        -D ${RESPONSE_HEADERS} \
        -H "${AUTH_HEADER}" \
        -H "Content-Type: application/octet-stream" \
        "${UPLOAD_URL}"

STATUS=$(grep 'HTTP/1.1 ' ${RESPONSE_HEADERS} | tail -1)
if ! [[ ${STATUS} =~ ^HTTP/1.1\ 201 ]] ; then
    echo "ERROR: ${status}"
    exit 1
fi

blob_size
readonly LAYER_SIZE=${BLOB_SIZE}
echo "INFO: layer_size=${LAYER_SIZE}"

# upload image config
create_blob
if [[ "${UPLOAD_LOCATION}" =~ \? ]] ; then
    UPLOAD_URL="${UPLOAD_LOCATION}&digest=${IMAGE_CONFIG_DIGEST}"
else
    UPLOAD_URL="${UPLOAD_LOCATION}?digest=${IMAGE_CONFIG_DIGEST}"
fi
curl -v -X PUT --data-binary @${IMAGE_CONFIG} \
    -D ${RESPONSE_HEADERS} \
    -H "${AUTH_HEADER}" \
    -H "Content-Type: application/vnd.docker.container.image.v1+json" \
    "${UPLOAD_URL}"

STATUS=$(grep 'HTTP/1.1 ' ${RESPONSE_HEADERS} | tail -1)
if ! [[ ${STATUS} =~ ^HTTP/1.1\ 201 ]] ; then
    echo "ERROR: ${status}"
    return 1
fi

blob_size
readonly CONFIG_SIZE=${BLOB_SIZE}
echo "INFO: config_size=${CONFIG_SIZE}"

# create distribution manifest
readonly DISTRIBUTION_MANIFEST="$(mktemp -t XXX}).json"
cat << EOF > ${DISTRIBUTION_MANIFEST}
{
    "schemaVersion": 2,
    "mediaType": "application/vnd.docker.distribution.manifest.v2+json",
    "config": {
        "mediaType": "application/vnd.docker.container.image.v1+json",
        "size": ${CONFIG_SIZE},
        "digest": "${IMAGE_CONFIG_DIGEST}"
    },
    "layers": [
        {
            "mediaType": "application/vnd.docker.image.rootfs.diff.tar.gzip",
            "size": ${LAYER_SIZE},
            "digest": "${LAYER_DIGEST}"
        }
    ]
}
EOF

# upload manifest
curl -v -X PUT --data-binary @${DISTRIBUTION_MANIFEST} \
    -H "${AUTH_HEADER}" \
    -D ${RESPONSE_HEADERS} \
    -H "Content-Type: application/vnd.docker.distribution.manifest.v2+json" \
    "${API_URL_BASE}/manifests/${IMAGE_TAG}"
STATUS=$(grep 'HTTP/1.1 ' ${RESPONSE_HEADERS} | tail -1)
if ! [[ ${STATUS} =~ ^HTTP/1.1\ 201 ]] ; then
    echo "ERROR: ${status}"
    exit 1
fi

echo "INFO: image push completed"