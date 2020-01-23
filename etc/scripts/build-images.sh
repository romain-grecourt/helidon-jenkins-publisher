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

# Path to this script
if [ -h "${0}" ] ; then
    readonly SCRIPT_PATH="$(readlink "${0}")"
else
    readonly SCRIPT_PATH="${0}"
fi
readonly SCRIPT_DIR=$(dirname ${SCRIPT_PATH})
readonly SCRIPT=$(basename ${SCRIPT_PATH})
source ${SCRIPT_DIR}/../imagetool/_common.sh

usage(){
  cat <<EOF

DESCRIPTION: Build all images

USAGE:

$(basename ${SCRIPT}) [OPTIONS] --tag=TAG

  --tag=TAG
          Image tag to use

OPTIONS:

  --namespace
          Image namespace (prefix)

  --load
          Load the created image to the Docker daemon.

  --push
          Push the images

  --registry-url=URL
          Registry URL to push to.

EOF
  common_user_password_usage
  common_usage
}

# parse command line args
ARGS=( "${@}" )
ENV_ARRAY=( )
for ((i=0;i<${#ARGS[@]};i++))
{
    ARG=${ARGS[${i}]}
    case ${ARG} in
    "--namespace="*)
        readonly IMAGE_NAMESPACE=$(remove_trailing_slashes ${ARG#*=})/
        ;;
    "--tag="*)
        readonly IMAGE_TAG=${ARG#*=}
        ;;
    "--push"*)
        readonly PUSH=true
        ;;
    "--registry-url="*)
        readonly REGISTRY_URL=${ARG#*=}
        ;;
    "--load")
        readonly LOAD=true
        ;;
    *)
        common_process_user_password_args ${ARG} || common_process_args ${ARG}
        ;;
  esac
}

if [ -z "${IMAGE_TAG}" ] ; then
    echo "ERROR: --tag option is required"
    exit 1
fi

if [ -z "${PUSH}" ] ; then
    readonly PUSH=false
fi

if ${PUSH} && [ -z "${REGISTRY_URL}" ] ; then
    echo "ERROR: --registry-url option is required with --push"
    exit 1
fi

if [ -z "${LOAD}" ] ; then
    readonly LOAD=false
fi

common_init

if ${LOAD} ; then
    BUILD_OPTS="--load"
fi

EXTRA_OPTS="--workdir=${WORKDIR}"
if ${DEBUG} ; then
    EXTRA_OPTS="${EXTRA_OPTS} --v"
elif ${DEBUG2} ; then
    EXTRA_OPTS="${EXTRA_OPTS} --vv"
fi
if [ ! -z "${STDERR}" ] ; then
    EXTRA_OPTS="${EXTRA_OPTS} --stderr-file=${STDERR}"
fi

# base image versions
readonly NGINX_VERSION="1.17.6-alpine"
readonly OPENJDK_VERSION="8-jre-slim"

echo "INFO: building frontend-ui image"
build ${EXTRA_OPTS} ${BUILD_OPTS} \
    --name="${IMAGE_NAMESPACE}helidon-build-publisher-frontend-ui:${IMAGE_TAG}" \
    --base-registry-url="https://registry-1.docker.io/v2" \
    --base="library/nginx:${NGINX_VERSION}" \
    --path="${WS_DIR}/frontend-ui/dist" \
    --target="/usr/share/nginx/html" \
    --path="frontend-ui/nginx.conf" \
    --target="/etc/nginx/conf.d/default.conf" \
    --output-file="${WORKDIR}/frontend-ui-image.tar"

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
build ${EXTRA_OPTS} ${BUILD_OPTS} \
    --name="${IMAGE_NAMESPACE}helidon-build-publisher-frontend-api:${IMAGE_TAG}" \
    --base-registry-url="https://registry-1.docker.io/v2" \
    --base="library/openjdk:${OPENJDK_VERSION}" \
    --path="${WS_DIR}/frontend-api/target" \
    --target="/app" \
    --includes="*.jar libs" \
    --cmd="java ${JAVA_CMD_OPTS} -jar /app/helidon-build-publisher-frontend-api.jar" \
    --output-file="${WORKDIR}/frontend-api-image.tar"

echo "INFO: building backend image"
build ${EXTRA_OPTS} ${BUILD_OPTS} \
    --name="${IMAGE_NAMESPACE}helidon-build-publisher-backend:${IMAGE_TAG}" \
    --base-registry-url="https://registry-1.docker.io/v2" \
    --base="library/openjdk:${OPENJDK_VERSION}" \
    --path="${WS_DIR}/backend/target" \
    --target="/app" \
    --includes="*.jar libs" \
    --cmd="java ${JAVA_CMD_OPTS} -jar /app/helidon-build-publisher-backend.jar" \
    --output-file="${WORKDIR}/backend-image.tar"

if ${PUSH} ; then
    PUSH_OPTS="--registry-url=${REGISTRY_URL}"
    if [ ! -z "${STDERR}" ] ; then
        PUSH_OPTS="${PUSH_OPTS} --stderr-file=${STDERR}"
    fi
    if [ ! -z "${UNAME}" ] && [ ! -z "${UPASSWD}" ] ; then
        PUSH_OPTS="${PUSH_OPTS} --user=${UNAME} --password=${UPASSWD}"
    fi

    echo "INFO: pushing frontend-ui image"
    push ${EXTRA_OPTS} ${PUSH_OPTS} \
        --name="${IMAGE_NAMESPACE}helidon-build-publisher-frontend-ui" \
        --tag="${IMAGE_TAG}" \
        --image="${WORKDIR}/frontend-ui-image.tar"

    echo "INFO: pushing frontend-api image"
    push ${EXTRA_OPTS} ${PUSH_OPTS} \
        --name="${IMAGE_NAMESPACE}helidon-build-publisher-frontend-api" \
        --tag="${IMAGE_TAG}" \
        --image="${WORKDIR}/frontend-api-image.tar"

    echo "INFO: pushing backend image"
    push ${EXTRA_OPTS} ${PUSH_OPTS} \
        --name="${IMAGE_NAMESPACE}helidon-build-publisher-backend" \
        --tag="${IMAGE_TAG}" \
        --image="${WORKDIR}/backend-image.tar"
fi