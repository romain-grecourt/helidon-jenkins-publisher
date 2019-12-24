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
    printf "[ERROR] Error(code=%s) occurred at %s:%s command: %s\n" \
        "${CODE}" "${BASH_SOURCE}" "${LINENO}" "${BASH_COMMAND}"
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

DESCRIPTION: Build a single layer image from a directory

USAGE:

$(basename ${SCRIPT}) [OPTIONS] --path=PATH --name=NAME

  --path=PATH
          Path to the source directory to create the image.

  --name=NAME
          Image name of the form repository:tag.

OPTIONS:

  --output-file=PATH
          Path of the image tar file to create.

  --comment=TEXT
          Comment text to include in the metatada.

  --includes=INCLUDES
          List of relative include paths patterns

  --target=PATH
          Target path of the included files in the created layer

  --base=BASE_IMAGE
          Base image

  --cmd=CMD
          Image command

  --end=KEY=VAL
          Image environment variable

  --load
          Load the created image to the Docker daemon.

  --debug
          Print debug output.

  --help
          Prints the usage and exits.

EOF
}

remove_leading_slashes() {
    local input=${1}
    local len=${#input} ; input=${input##/}
    while [ ${#input} -lt ${len} ] ; do len=${#input} ; input=${input##/} ; done
    echo ${input}
}

remove_trailing_slashes() {
    local input=${1}
    local len=${#input} ; input=${input%%/}
    while [ ${#input} -lt ${len} ] ; do len=${#input} ; input=${input%%/} ; done
    echo ${input}
}

# parse command line args
ARGS=( "${@}" )
ENV_ARRAY=( )
for ((i=0;i<${#ARGS[@]};i++))
{
    ARG=${ARGS[${i}]}
    case ${ARG} in
    "--help")
        usage
        exit 0
        ;;
    "--debug")
        readonly DEBUG=true
        ;;
    "--path="*)
        readonly SOURCE_PATH=$(cd ${ARG#*=} ; pwd -P)
        ;;
    "--name="*)
        readonly IMAGE_NAME=${ARG#*=}
        ;;
    "--comment="*)
        readonly COMMENT=${ARG#*=}
        ;;
    "--includes="*)
        readonly INCLUDES=${ARG#*=}
        ;;
    "--target="*)
        TPATH=${ARG#*=}
        TPATH=$(remove_leading_slashes ${TPATH})
        TPATH=$(remove_trailing_slashes ${TPATH})
        readonly TARGET_PATH=${TPATH}
        ;;
    "--base="*)
        readonly BASE_IMAGE=${ARG#*=}
        ;;
    "--env="*)
        ENV_ARRAY[${#ENV_ARRAY[*]}]=${ARG#*=}
        ;;
    "--cmd="*)
        readonly IMAGE_CMD=${ARG#*=}
        ;;
    "--load")
        readonly LOAD=true
        ;;
    "--output-file="*)
        readonly OUTPUT_FILE=${ARG#*=}
        ;;
    *)
        echo "ERROR: unkown option: ${ARG}"
        usage
        exit 1
        ;;
  esac
}

if [ -z "${IMAGE_NAME}" ] ; then
    echo "ERROR: --name option is required"
    usage
    exit 1
fi

if [ -z "${SOURCE_PATH}" ] ; then
    echo "ERROR: --path option is required"
    usage
    exit 1
fi

if [ -z "${LOAD}" ] ; then
    readonly LOAD=false
fi

if [ -z "${COMMENT}" ] ; then
    readonly COMMENT="created by ${SCRIPT}"
fi

if [ -z "${DEBUG}" ] ; then
    exec 2> /dev/null
    readonly DEBUG=false
fi

if ! type sha256sum > /dev/null 2>&1; then
    if ! type shasum > /dev/null 2>&1; then
        echo "ERROR: none of sha256sum shasum found in PATH"
        exit 1
    else
        # MacOS
        readonly SHASUM="shasum -a 256"
    fi
else
    # Linux
    readonly SHASUM="sha256sum"
fi

random_id(){
  local N B T
  for (( N=0; N < 32; ++N ))
  do
    B=$(( $RANDOM%255 ))
    if (( N == 6 ))
    then
        printf '4%x' $(( B%15 ))
    elif (( N == 8 ))
    then
        local C='89ab'
        printf '%c%x' ${C:$(( $RANDOM%${#C} )):1} $(( B%15 ))
    else
        printf '%02x' $B
    fi
  done
}

is_shell_attribute_set() { # attribute, like "e"
  case "$-" in
    *"$1"*) return 0 ;;
    *)    return 1 ;;
  esac
}

if is_shell_attribute_set x ; then
    BASH_OPTS="-x"
fi

if is_shell_attribute_set e ; then
    BASH_OPTS="${BASH_OPTS} -e"
fi

echo "INFO: building ${IMAGE_NAME} ..."

readonly IMAGE_REPO=$(echo ${IMAGE_NAME} | cut -d ':' -f1)
readonly IMAGE_TAG=$(echo ${IMAGE_NAME} | cut -d ':' -f2)
readonly WORKDIR=$(mktemp -d -t "XXX${SCRIPT}")
echo "INFO: source = ${SOURCE_PATH}"
if [ ! -z "${TARGET_PATH}" ] ; then
    echo "INFO: target_path = ${TARGET_PATH}"
fi
echo "INFO: name = ${IMAGE_NAME}"
echo "INFO: image_repo = ${IMAGE_REPO}"
echo "INFO: image_tag = ${IMAGE_TAG}"
echo "INFO: workdir = ${WORKDIR}"

# contains all layer cache ids
LAYERS=( )
# contains all layer digests
DIGESTS=( )

# contains the command
CMD_ARRAY=( )

process_base_layer() {
    local image_dir=${1}
    local digest=${2}

    # copy layer tar
    local layer_id=$(random_id)
    mkdir ${WORKDIR}/${layer_id}
    cp ${image_dir}/${digest##*:}/layer.tar ${WORKDIR}/${layer_id}/layer.tar
    DIGESTS[${#DIGESTS[*]}]="sha256:$(${SHASUM} ${WORKDIR}/${layer_id}/layer.tar | cut -d ' ' -f1)"

    # Add layer metadata
    echo "1.0" > ${WORKDIR}/${layer_id}/VERSION
    cat << EOF > ${WORKDIR}/${layer_id}/json
{
  "id": "${layer_id}",
  "architecture": "amd64",
  "os": "linux"
}
EOF
    LAYERS[${#LAYERS[*]}]=${layer_id}

    # get ENV from base image
    local base_config="${image_dir}/$(cat ${image_dir}/manifest.json | jq -r '.config.digest' | cut -d ':' -f2).json"
    for env in $(cat ${base_config} | jq -r '(.config.Env // [])[]')
    do
        found=false
        for ((i=0;i<${#ENV_ARRAY[@]};i++))
        {
            if [ "${ENV_ARRAY[${i}]%%=*}" = "${env%%=*}" ] ; then
                found=true
                break
            fi
        }
        if ! ${found} ; then
            ENV_ARRAY[${#ENV_ARRAY[*]}]=${env}
        fi
    done

    # get CMD from base image
    # override CMD if not empty
    if [ ${#CMD_ARRAY[*]} -gt 0 ] ; then
        CMD_ARRAY=( )
    fi
    for i in $(cat ${base_config} | jq -r '(.config.Cmd // [])|to_entries | .[].key')
    do
        CMD_ARRAY[${#CMD_ARRAY[*]}]=$(cat ${base_config} | jq -r ".config.Cmd[${i}]")
    done
}

# Process base image
if [ ! -z "${BASE_IMAGE}" ] ; then
    readonly CACHEDIR="${SCRIPT_DIR}/.imagecache"
    readonly BASE_IMGDIR="${CACHEDIR}/$(echo ${BASE_IMAGE} | ${SHASUM} | cut -d ' ' -f1)"
    if ${DEBUG} ; then
        PULL_EXTRA_OPTS="--debug"
    fi
    if [ ! -e ${BASE_IMGDIR} ] ; then
        mkdir -p ${BASE_IMGDIR}
        bash ${BASH_OPTS} ${SCRIPT_DIR}/pull-image.sh ${PULL_EXTRA_OPTS} \
            --output-dir=${BASE_IMGDIR} \
            --name=${BASE_IMAGE}
    fi
    readonly BASE_MANIFEST
    for i in `cat ${BASE_IMGDIR}/manifest.json | jq '.layers | to_entries | .[].key'`
    do
        process_base_layer ${BASE_IMGDIR} $(cat ${BASE_IMGDIR}/manifest.json | jq -r ".layers[${i}].digest")
    done
fi

# Add image cmd
if [ ! -z "${IMAGE_CMD}" ] ; then
    # override CMD if not empty
    if [ ${#CMD_ARRAY[*]} -gt 0 ] ; then
        CMD_ARRAY=( )
    fi
    for cmd in ${IMAGE_CMD[*]}
    do
        CMD_ARRAY[${#CMD_ARRAY[*]}]=${cmd}
    done
fi

# Create a layer
readonly LAYER_ID=$(random_id)
mkdir -p ${WORKDIR}/${LAYER_ID}

# List all files to include
readonly TAR_MANIFEST=$(mktemp -t "XXXtar-manifest")
echo "INFO: tar_manifest = ${TAR_MANIFEST}"
if [ -z "${INCLUDES}" ] ; then
    find ${SOURCE_PATH} -type f | sed s@"${SOURCE_PATH}/"@@g > ${TAR_MANIFEST}
else
    for include in ${INCLUDES} ; do
        if [ -e "${SOURCE_PATH}/${include}" ] ; then
            find ${SOURCE_PATH}/${include} -type f | sed s@"${SOURCE_PATH}/"@@g >> ${TAR_MANIFEST}
        else
            for elt in `ls ${SOURCE_PATH}/${include} 2> /dev/null` ; do
                if [ -f ${elt} ] ; then
                    echo ${elt} | sed s@"${SOURCE_PATH}/"@@g >> ${TAR_MANIFEST}
                else
                    find ${SOURCE_PATH}/${elt} -type f | sed s@"${SOURCE_PATH}/"@@g >> ${TAR_MANIFEST}
                fi
            done
        fi
    done
fi

# Create the layer.tar file
readonly LAYER_TAR="${WORKDIR}/${LAYER_ID}/layer.tar"
echo "INFO: creating ${LAYER_TAR}"
readonly TARVIEW=$(mktemp -d -t "XXX${SCRIPT}")/view
mkdir -p $(dirname ${TARVIEW}/${TARGET_PATH})
if [ -z "${TARGET_PATH}" ] ; then
    ln -fs ${SOURCE_PATH} ${TARVIEW}
else 
    ln -fs ${SOURCE_PATH} ${TARVIEW}/${TARGET_PATH}
fi
while read f ; do
    if [ -z "${TARGET_PATH}" ] ; then
        echo "${f}"
    else
        echo "${TARGET_PATH}/${f}"
    fi
done < ${TAR_MANIFEST} | tar -cvf ${LAYER_TAR} --files-from - -C ${TARVIEW}

# Add layer metadata
echo "1.0" > ${WORKDIR}/${LAYER_ID}/VERSION
cat << EOF > ${WORKDIR}/${LAYER_ID}/json
{
  "id": "${LAYER_ID}",
  "architecture": "amd64",
  "os": "linux"
}
EOF

readonly CREATED_DATE=$(date -u +"%Y-%m-%dT%H:%M:%SZ")
readonly LAYER_DIGEST=$(${SHASUM} ${WORKDIR}/${LAYER_ID}/layer.tar | cut -d ' ' -f1)
LAYERS[${#LAYERS[*]}]=${LAYER_ID}
DIGESTS[${#DIGESTS[*]}]="sha256:${LAYER_DIGEST}"

cat << EOF > ${WORKDIR}/config.json
{
  "architecture": "amd64",
  "created": "${CREATED_DATE}",
  "comment": "${COMMENT}",
  "os": "linux",
  "config": {
     "Env": [
$(for ((i=0;i<${#ENV_ARRAY[@]};i++))
{
    printf "      \"${ENV_ARRAY[${i}]}\""
    if [ "${i}" = "$((${#ENV_ARRAY[@]}-1))" ] ; then printf "\n" ; else printf ",\n" ; fi
})
     ],
     "Cmd": [
$(for ((i=0;i<${#CMD_ARRAY[@]};i++))
{
    printf "      \"${CMD_ARRAY[${i}]}\""
    if [ "${i}" = "$((${#CMD_ARRAY[@]}-1))" ] ; then printf "\n" ; else printf ",\n" ; fi
})
     ]
  },
  "rootfs": {
    "type": "layers",
    "diff_ids": [
$(for ((i=0;i<${#DIGESTS[@]};i++))
{
    printf "      \"${DIGESTS[${i}]}\""
    if [ "${i}" = "$((${#DIGESTS[@]}-1))" ] ; then printf "\n" ; else printf ",\n" ; fi
})
    ]
  }
}
EOF

readonly IMAGE_ID=$(${SHASUM} ${WORKDIR}/config.json | cut -d ' ' -f1)
mv ${WORKDIR}/config.json ${WORKDIR}/${IMAGE_ID}.json

cat << EOF > ${WORKDIR}/repositories
{
  "${IMAGE_REPO}": {
    "${IMAGE_TAG}": "${CACHE_ID}"
  }
}
EOF

cat << EOF > ${WORKDIR}/manifest.json
[
  {
    "Config": "${IMAGE_ID}.json",
    "RepoTags": [
      "${IMAGE_REPO}:${IMAGE_TAG}"
    ],
    "Layers": [
$(for ((i=0;i<${#LAYERS[@]};i++))
{
    printf "      \"${LAYERS[${i}]}/layer.tar\""
    if [ "${i}" = "$((${#LAYERS[@]}-1))" ] ; then printf "\n" ; else printf ",\n" ; fi
})
    ]
  }
]
EOF

if [ ! -z "${OUTPUT_FILE}" ] ; then
    readonly IMAGE_TAR="${OUTPUT_FILE}"
else
    readonly IMAGE_TAR="$(mktemp -t XXX${SCRIPT}).tar"
fi

echo "INFO: creating ${IMAGE_TAR}"
tar -cvf ${IMAGE_TAR} -C ${WORKDIR} .

# load the image
if ${LOAD} ; then
    echo "INFO: loading the image to Docker"
    if ${DEBUG} ; then
        docker load -i ${IMAGE_TAR}
    else
        docker load -i ${IMAGE_TAR} 1> /dev/null
    fi
fi

if ${LOAD} && [ -z "${OUTPUT_FILE}" ] ; then
    echo "INFO: cleaning up image tar..."
    rm -f ${IMAGE_TAR}
fi