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
readonly SCRIPT=$(basename ${SCRIPT_PATH})

usage(){
  cat <<EOF

DESCRIPTION: Authenticate against a docker registry

USAGE:

$(basename ${SCRIPT}) [OPTIONS] --registry-url=URL  --repository=REPOSITORY --SCOPES=SCOPES --output-file=PATH

  --registry-url=URL
          Registry URL. (e.g. https://registry-1.docker.io/v2)

  --repository=REPOSITORY
          Image repository. (e.g. library/nginx)

  --scopes=SCOPES
          Authentication scopes. (e.g. "pull" or "push,pull")

  --output-file=PATH
          Path to the output file to store the token.

OPTIONS:
  --user=USERNAME
          Username to authenticate against the registry.

  --password=PASSWORD
          Password to authenticate against the registry.

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
    "--user="*)
        readonly UNAME=${ARG#*=}
        ;;
    "--password="*)
        readonly UPASSWD=${ARG#*=}
        ;;
    "--repository="*)
        readonly REPOSITORY=${ARG#*=}
        ;;
    "--scopes="*)
        readonly SCOPES=${ARG#*=}
        ;;
    "--output-file="*)
        readonly OUTPUT_FILE=${ARG#*=}
        ;;
    *)
        echo "ERROR: unknown option: ${ARG}"
        usage
        exit 1
        ;;
  esac
}

if [ -z "${REGISTRY_URL}" ] ; then
    echo "ERROR: --registry-url option is required"
    usage
    exit 1
elif [ -z "${REPOSITORY}" ] ; then
    echo "ERROR: --repository option is required"
    usage
    exit 1
elif [ -z "${SCOPES}" ] ; then
    echo "ERROR: --scopes option is required"
    usage
    exit 1
elif [ -z "${OUTPUT_FILE}" ] ; then
    echo "ERROR: --output-file option is required"
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

touch ${OUTPUT_FILE} || exit 1

# get auth challenge
readonly RESPONSE_HEADERS=$(mktemp -t XXX-headers)
curl -v -L -D ${RESPONSE_HEADERS} "${REGISTRY_URL}" 1> /dev/null
STATUS=$(grep 'HTTP/1.1 ' ${RESPONSE_HEADERS} | tail -1)
if ! [[ ${STATUS} =~ ^HTTP/1.1\ 401 ]] ; then
    echo "ERROR: Unexpected response: ${STATUS1}"
    exit 1
fi

trim() {
    local var="$*"
    # remove leading whitespace characters
    var="${var#"${var%%[![:space:]]*}"}"
    # remove trailing whitespace characters
    var="${var%"${var##*[![:space:]]}"}"
    echo -n "$var"
}

# parse challenge header
readonly CHALLENGE_HEADER_PREFIX="Www-Authenticate: bearer "
readonly CHALLENGE_HEADER=$(grep -i "${CHALLENGE_HEADER_PREFIX}" ${RESPONSE_HEADERS})
readonly CHALLENGE_HEADER_VALUE=${CHALLENGE_HEADER:${#CHALLENGE_HEADER_PREFIX}}
IFS=,
readonly CHALLENGE_HEADER_VALUES=(${CHALLENGE_HEADER_VALUE[*]})
for ((i=0;i<${#CHALLENGE_HEADER_VALUES[@]};i++))
{
    ELT=$(trim ${CHALLENGE_HEADER_VALUES[${i}]})
    if [[ "${ELT}" =~ ^realm=.* ]] ; then
        REALM=${ELT:7:$((${#ELT}-8))}
    elif [[ "${ELT}" =~ ^service=.* ]] ; then
        SERVICE=${ELT:9:$((${#ELT}-10))}
    fi
}
if [ -z "${REALM}" ] || [ -z "${SERVICE}" ] ; then
    echo "ERROR: Unable to parse auth challenge header"
    exit 1
fi

# get token
# TODO set offline_token=1 ?
readonly TOKEN_OPTS="service=${SERVICE}&scope=repository:${REPOSITORY}:${SCOPES}"
if [ ! -z "${UNAME}" ] && [ ! -z "${UPASSWD}" ] ; then
    curl -u "${UNAME}:${UPASSWD}" -D ${RESPONSE_HEADERS} "${REALM}?${TOKEN_OPTS}" | jq -r '.token' > ${OUTPUT_FILE}
else
    curl -D ${RESPONSE_HEADERS} "${REALM}?${TOKEN_OPTS}" | jq -r '.token' > ${OUTPUT_FILE}
fi
STATUS=$(grep 'HTTP/1.1 ' ${RESPONSE_HEADERS} | tail -1)
if ! [[ ${STATUS} =~ ^HTTP/1.1\ 200 ]] ; then
    echo "ERROR: Unexpected response: ${STATUS2}"
    exit 1
fi