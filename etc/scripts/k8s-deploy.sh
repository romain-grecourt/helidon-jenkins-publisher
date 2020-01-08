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

DESCRIPTION: Deploy to k8s.

USAGE:

$(basename ${SCRIPT}) [OPTIONS] --tag=TAG

  --tag=TAG
          Image tag to use

OPTIONS:

  --namespace
          Image namespace (prefix)

  --image-pull-policy
          Image pull policy (e.g. Always)
EOF
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
        readonly IMAGES_NAMESPACE=$(remove_trailing_slashes ${ARG#*=})/
        ;;
    "--tag="*)
        readonly IMAGES_TAG=${ARG#*=}
        ;;
    "--image-pull-policy="*)
        readonly IMAGE_PULL_POLICY=${ARG#*=}
        ;;
    *)
        common_process_args ${ARG}
        ;;
  esac
}

if [ -z "${IMAGES_TAG}" ] ; then
    echo "ERROR: --tag option is required"
    exit 1
fi

common_init
if ! type kubectl > /dev/null 2>&1; then
    echo "ERROR: kubectl not found in PATH"
    exit 1
fi

export PATH=${PATH}:${SCRIPT_DIR}/../imagetool

substitute_image(){
    sed s@"^\(\ \{0,\}\)\(\-\ \{0,\}image\ \{0,\}\:\ \{0,\}\)${1}\(.*\)$"@"\1\2${2}\3"@g
}

image_pull_policy(){
    if [ ! -z "${IMAGE_PULL_POLICY}" ] ; then
        sed s@'\(imagePullPolicy: \)IfNotPresent'@"\1${IMAGE_PULL_POLICY}"@g
    fi
}

BACKEND_YAML=$(mktemp ${WORKDIR}/backendk8syaml.XXX)
echo "INFO: backend_yaml = ${BACKEND_YAML}"
cat ${WS_DIR}/k8s/backend.yaml \
    | substitute_image "helidon-build-publisher-backend:latest" "${IMAGES_NAMESPACE}helidon-build-publisher-backend:${IMAGES_TAG}" \
    | image_pull_policy \
    > ${BACKEND_YAML}

FRONTEND_YAML=$(mktemp ${WORKDIR}/frontendk8syaml.XXX)
echo "INFO: frontend_yaml = ${FRONTEND_YAML}"
cat ${WS_DIR}/k8s/frontend.yaml \
    | substitute_image "helidon-build-publisher-frontend-ui:latest" "${IMAGES_NAMESPACE}helidon-build-publisher-frontend-ui:${IMAGES_TAG}" \
    | substitute_image "helidon-build-publisher-frontend-api:latest" "${IMAGES_NAMESPACE}helidon-build-publisher-frontend-api:${IMAGES_TAG}" \
    | image_pull_policy \
    > ${FRONTEND_YAML}

echo "INFO: applying configuration..."
kubectl apply -f ${BACKEND_YAML} -f ${FRONTEND_YAML}

# TODO detect no config changed and kill pods to support incremental
