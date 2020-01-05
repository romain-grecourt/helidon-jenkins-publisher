#!/bin/bash -e
#
# Copyright (c) 2019 Oracle and/or its affiliates. All rights reserved.
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
    CODE="${?}" && \
    set +x && \
    printf "ERROR: code=%s occurred at %s:%s command: %s\n" \
        "${CODE}" "${BASH_SOURCE}" "${LINENO}" "${BASH_COMMAND}"
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

DESCRIPTION: Pull an image from a remote registry

USAGE:

$(basename ${SCRIPT}) [OPTIONS] --name=NAME --registry-url=URL

  --registry-url=URL
          URL of the registry to pull from.

  --name=NAME
          Name of the image to be pulled.

OPTIONS:

  --ouput-dir=DIR
          Path of the output directory.

  --user=USERNAME
          Registry user.

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
        readonly IMAGE_FULL_NAME=${ARG#*=}
        ;;
    "--output-dir="*)
        readonly OUTPUT_DIR=${ARG#*=}
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

if [ -z "${IMAGE_FULL_NAME}" ] ; then
    echo "ERROR: --name option is required"
    usage
    exit 1
elif [ -z "${REGISTRY_URL}" ] ; then
    echo "ERROR: --registry-url option is required"
    usage
    exit 1
elif [ -z "${OUTPUT_DIR}" ] ; then
    readonly OUTPUT_DIR=$(mktemp -d -t "XXX${SCRIPT}")
elif [ ! -d "${OUTPUT_DIR}" ] ; then
    echo "ERROR: ${OUTPUT_DIR} is not a valid directory"
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

readonly IMAGE_NAME=${IMAGE_FULL_NAME%%:*}
readonly IMAGE_TAG=${IMAGE_FULL_NAME##*:}
readonly API_URL_BASE="${REGISTRY_URL}/${IMAGE_NAME}"

echo "INFO: pulling image ${IMAGE_NAME}:${IMAGE_TAG} ..."
echo "INFO: outputdir=${OUTPUT_DIR}"

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
    --scopes="pull" \
    --output-file=${TOKEN_FILE}

readonly TOKEN=$(cat ${TOKEN_FILE})
readonly AUTH_HEADER="Authorization: Bearer ${TOKEN}"

readonly IMAGE_MANIFEST="${OUTPUT_DIR}/manifest.json"
RESPONSE_HEADERS=$(mktemp -t XXX})

# download manifest
curl -v -X GET \
    -o ${IMAGE_MANIFEST} \
    -D ${RESPONSE_HEADERS} \
    -H "${AUTH_HEADER}" \
    -H "Accept: application/vnd.docker.distribution.manifest.v2+json" \
    "${API_URL_BASE}/manifests/${IMAGE_TAG}"

STATUS=$(grep 'HTTP/1.1 ' ${RESPONSE_HEADERS} | tail -1)
if ! [[ ${STATUS} =~ ^HTTP/1.1\ 200 ]] ; then
    echo "ERROR: ${STATUS}"
    exit 1
fi

# download image config
readonly IMAGE_CONFIG="$(cat ${IMAGE_MANIFEST} | jq -r '.config.digest' | cut -d ':' -f2).json"
curl -v -L -X GET \
    -o ${OUTPUT_DIR}/${IMAGE_CONFIG} \
    -D ${RESPONSE_HEADERS} \
    -H "${AUTH_HEADER}" \
    -H "Accept: $(cat ${IMAGE_MANIFEST} | jq -r '.config.mediaType')" \
    "${API_URL_BASE}/blobs/$(cat ${IMAGE_MANIFEST} | jq -r '.config.digest')"
STATUS=$(grep 'HTTP/1.1 ' ${RESPONSE_HEADERS} | tail -1)
if ! [[ ${STATUS} =~ ^HTTP/1.1\ 200 ]] ; then
    echo "ERROR: ${STATUS}"
    exit 1
fi

get_layer() {
    local digest=${1}
    local media_type=${2}
    local layer_dir=${digest##*:}
    mkdir ${OUTPUT_DIR}/${layer_dir}
    curl -v -L -X GET \
        -o ${OUTPUT_DIR}/${layer_dir}/layer.tar.gz \
        -D ${RESPONSE_HEADERS} \
        -H "${AUTH_HEADER}" \
        -H "Accept: ${media_type}" \
        "${API_URL_BASE}/blobs/${digest}"
    STATUS=$(grep 'HTTP/1.1 ' ${RESPONSE_HEADERS} | tail -1)
    if ! [[ ${STATUS} =~ ^HTTP/1.1\ 200 ]] ; then
        echo "ERROR: ${STATUS}"
        return 1
    fi
    gunzip ${OUTPUT_DIR}/${layer_dir}/layer.tar.gz
}

# download layers
for i in `cat ${IMAGE_MANIFEST} | jq '.layers | to_entries | .[].key'`
do
    get_layer $(cat ${IMAGE_MANIFEST} | jq -r ".layers[${i}].digest") \
              $(cat ${IMAGE_MANIFEST} | jq -r ".layers[${i}].mediaType")
done

echo "INFO: image pull completed"