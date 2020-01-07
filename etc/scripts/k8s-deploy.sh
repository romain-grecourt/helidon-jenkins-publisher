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

  --backend-public-key-file=FILE
          Backend RSA PKCS8 public key file.

OPTIONS:

  --namespace
          Image namespace (prefix)

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
        readonly IMAGES_NAMESPACE=$(remove_trailing_slashes ${ARG#*=})/
        ;;
    "--tag="*)
        readonly IMAGES_TAG=${ARG#*=}
        ;;
    "--registry-url="*)
        readonly REGISTRY_URL=${ARG#*=}
        ;;
    "--backend-public-key-file="*)
        readonly BACKEND_PUBLIC_KEY=${ARG#*=}
        ;;
    *)
        common_process_user_password_args ${ARG} || common_process_args ${ARG}
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

create_docker_registry_secret(){
    echo "INFO: creating docker-registry ${1}"
    kubectl create secret docker-registry ${1} \
        --docker-server="${REGISTRY_URL}" \
        --docker-username="${UNAME}" \
        --docker-password="${UPASSWD}"
}

if [ ! -z "${REGISTRY_URL}" ] && [ ! -z "${UNAME}" ] && [ ! -z "${UPASSWD}" ] ; then
    REGISTRY_NAME=$(echo ${REGISTRY_URL} | \
        sed s@'^\(http\)\{0,1\}\(s\)\{0,1\}\(\://\)\{0,1\}\(.*\)\(/v2\).*'@'\4'@g)
    kubectl get secret ${REGISTRY_NAME} >&2 || create_docker_registry_secret ${REGISTRY_NAME}
fi

create_backend_secret(){
    if [ -z "${BACKEND_PUBLIC_KEY}" ] ; then
        echo "ERROR: --backend-public-key-file is required"
        exit 1
    fi
    if [ ! -e "${BACKEND_PUBLIC_KEY}" ] ; then
        echo "ERROR: ${BACKEND_PUBLIC_KEY} is not a valid file"
        exit 1
    fi
    echo "INFO: creating backend secret"
    kubectl create secret generic helidon-build-publisher-secret \
        --from-file=backend.pem.pub=${BACKEND_PUBLIC_KEY}
}
kubectl get secret helidon-build-publisher-secret >&2 || create_backend_secret

substitute_image(){
    sed s@"^\(\ \{0,\}\-\ \{0,\}image\ \{0,\}\:\ \{0,\}\)${1}\(.*\)$"@"\1${2}\2"@g
}

BACKEND_YAML=$(mktemp ${WORKDIR}/backendk8syaml.XXX)
echo "INFO: backend_yaml = ${BACKEND_YAML}"
cat ${WS_DIR}/k8s/backend.yaml \
    | substitute_image "helidon-build-publisher-backend:latest" "${IMAGES_NAMESPACE}helidon-build-publisher-backend:${IMAGES_TAG}" \
    > ${BACKEND_YAML}

FRONTEND_YAML=$(mktemp ${WORKDIR}/frontendk8syaml.XXX)
echo "INFO: frontend_yaml = ${FRONTEND_YAML}"
cat ${WS_DIR}/k8s/frontend.yaml \
    | substitute_image "helidon-build-publisher-frontend-ui:latest" "${IMAGES_NAMESPACE}helidon-build-publisher-frontend-ui:${IMAGES_TAG}" \
    | substitute_image "helidon-build-publisher-frontend-api:latest" "${IMAGES_NAMESPACE}helidon-build-publisher-frontend-api:${IMAGES_TAG}" \
    > ${FRONTEND_YAML}

echo "INFO: applying configuration..."
kubectl apply -f ${BACKEND_YAML} -f ${FRONTEND_YAML}

# TODO detect no config changed and kill pods to support incremental
