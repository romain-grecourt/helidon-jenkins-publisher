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

DESCRIPTION: Build all images

USAGE:

$(basename ${SCRIPT}) [OPTIONS] --tag=TAG

  --tag=TAG
          Image tag to use

OPTIONS:

  --load
          Load the created image to the Docker daemon.

  --debug
          Print debug output.

  --help
          Prints the usage and exits.

EOF
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
    "--tag="*)
        readonly IMAGES_TAG=${ARG#*=}
        ;;
    "--debug")
        readonly DEBUG=true
        ;;
    "--load")
        readonly LOAD=true
        ;;
    *)
        echo "ERROR: unkown option: ${ARG}"
        usage
        exit 1
        ;;
  esac
}

if [ -z "${IMAGES_TAG}" ] ; then
    echo "ERROR: --tag option is required"
    usage
    exit 1
fi

if [ -z "${LOAD}" ] ; then
    readonly LOAD=false
fi

if [ -z "${DEBUG}" ] ; then
    exec 2> /dev/null
    readonly DEBUG=false
fi

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

if ${LOAD} ; then
    EXTRA_OPTS="--load"
fi

if ${DEBUG} ; then
    EXTRA_OPTS="${EXTRA_OPTS} --debug"
fi

# base image versions
readonly NGINX_VERSION="1.17.6-alpine"
readonly OPENJDK_VERSION="8-jre-slim"

echo "INFO: building frontend-ui image"
bash ${BASH_OPTS} ${SCRIPT_DIR}/build-image.sh ${EXTRA_OPTS} \
    --path=frontend-ui/dist \
    --target=/usr/share/nginx/html \
    --name=helidon-build-publisher-frontend-ui:${IMAGES_TAG} \
    --base=library/nginx:${NGINX_VERSION}

readonly JAVA_CMD_OPTS=`cat << EOF > /dev/stdout
    -server \
    -Djava.awt.headless=true \
    -XX:+UnlockExperimentalVMOptions \
    -XX:+UseCGroupMemoryLimitForHeap \
    -XX:InitialRAMFraction=2 \
    -XX:MinRAMFraction=2 \
    -XX:MaxRAMFraction=2 \
    -XX:+UseG1GC
EOF`

echo "INFO: building frontend-api image"
bash ${BASH_OPTS} ${SCRIPT_DIR}/build-image.sh ${EXTRA_OPTS} \
    --path=frontend-api/target \
    --target=/app \
    --includes="*.jar libs" \
    --cmd="java ${JAVA_CMD_OPTS} -jar /app/helidon-build-publisher-frontend-api.jar" \
    --name=helidon-build-publisher-frontend-api:${IMAGES_TAG} \
    --base=library/openjdk:${OPENJDK_VERSION}

echo "INFO: building backend image"
bash ${BASH_OPTS} ${BASH_OPTS} ${SCRIPT_DIR}/build-image.sh ${EXTRA_OPTS} \
    --path=backend/target \
    --target=/app \
    --includes="*.jar libs" \
    --cmd="java ${JAVA_CMD_OPTS} -jar /app/helidon-build-publisher-backend.jar" \
    --name=helidon-build-publisher-backend:${IMAGES_TAG} \
    --base=library/openjdk:${OPENJDK_VERSION}