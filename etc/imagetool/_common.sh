#!/bin/bash
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
    echo "ERROR: command: ${BASH_COMMAND}"
}
trap on_error ERR

readonly IMAGETOOL_DIR=$(cd $(dirname ${BASH_SOURCE}); pwd -P)
readonly BINDIR=${IMAGETOOL_DIR}/.bin
export PATH=${IMAGETOOL_DIR}:${BINDIR}:${PATH}

WS_DIR=$(cd ${IMAGETOOL_DIR}/../..; pwd -P)

common_process_args(){
    case ${1} in
    "--help"|"-help"|"-h"|"--h")
        usage
        exit 0
        ;;
    "--v"|"-v")
        DEBUG=true
        return 0
        ;;
    "--vv"|"--vvv"|"-v"|"-vv"|"-vvv")
        DEBUG2=true
        return 0
        ;;
    "--stderr-file="*)
        STDERR=${ARG#*=}
        return 0
        ;;
    "--workdir="*)
        WORKDIR=${ARG#*=}
        return 0
        ;;
    *)
        echo "ERROR: unknown option: ${ARG}"
        exit 1
        ;;
    esac
}

common_process_user_password_args(){
    case ${1} in
    "--user="*)
        UNAME=${ARG#*=}
        return 0
        ;;
    "--password="*)
        UPASSWD=${ARG#*=}
        return 0
        ;;
    *)
        return 1
        ;;
    esac
}

common_usage(){
  cat <<EOF
  --v
          Print stderr output.

  --vv
          Print stderr output and set -x

  --stderr-file=FILE
          File to capture stderr when not in debug mode.

  --workdir=DIR
          Parent directory where to create all files.

  --help
          Prints the usage and exits.

EOF
}

common_user_password_usage(){
  cat <<EOF
  --user=USERNAME
          Registry user.

  --password=PASSWORD
          Registry password.

EOF
}

common_init(){
    if [ -z "${WORKDIR}" ] ; then
        WORKDIR=$(mktemp -d -t ${SCRIPT}.XXX)
        echo "INFO: workdir = ${WORKDIR}"
    fi
    if [ -z "${STDERR}" ] ; then
        STDERR=$(mktemp ${WORKDIR}/stderr.XXX)
        echo "INFO: redirecting stderr to ${STDERR}"
    fi
    if [ -z "${DEBUG}" ] ; then
        if [ -z "${DEBUG2}" ] ; then
            exec 2>> ${STDERR}
        fi
        DEBUG=false
    fi
    if [ ! -z "${DEBUG2}" ] ; then
        set -x
    else
        exec 2>> ${STDERR}
        DEBUG2=false
    fi

    if ! type jq > /dev/null 2>&1; then
        echo "INFO: installing jq.."
        mkdir -p ${BINDIR}
        if [ `uname` = "Darwin" ] ; then
            curl -L -o  ${BINDIR}/jq https://github.com/stedolan/jq/releases/download/jq-1.6/jq-osx-amd64
            chmod +x ${BINDIR}/jq
        elif [ `uname` = "Linux" ] ; then
            curl -L -o  ${BINDIR}/jq https://github.com/stedolan/jq/releases/download/jq-1.6/jq-linux64
            chmod +x ${BINDIR}/jq
        fi
        if ! type jq > /dev/null 2>&1; then
            echo "ERROR: jq not found in PATH"
            exit 1
        fi
    fi
    if ! type base64 > /dev/null 2>&1; then
        echo "ERROR: base64 not found in PATH"
        exit 1
    fi
    if ! type curl > /dev/null 2>&1; then
        echo "ERROR: curl not found in PATH"
        exit 1
    fi
    if ! type sha256sum > /dev/null 2>&1; then
        if ! type shasum > /dev/null 2>&1; then
            echo "ERROR: none of sha256sum shasum found in PATH"
            exit 1
        else
            # MacOS
            SHASUM="shasum -a 256"
        fi
    else
        # Linux
        SHASUM="sha256sum"
    fi
    if [[ `tar --version` =~ .*GNU\ tar.* ]]
    then
        TAR_IMPL="gnu"
    elif [[ `tar --version` =~ .*bsdtar.* ]]
    then
        TAR_IMPL="bsd"
    fi
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

trim() {
    local var="$*"
    # remove leading white space characters
    var="${var#"${var%%[![:space:]]*}"}"
    # remove trailing white space characters
    var="${var%"${var##*[![:space:]]}"}"
    echo -n "$var"
}

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

is_http_status_or_die() {
    local status=$(egrep "^HTTP/" ${1} | tail -1)
    if ! [[ "${status}" =~ ^HTTP/(1|2)(\.)?(0|1)?\ "${2}" ]] ; then
        echo "ERROR: Unexpected response: ${status}"
        exit 1
    fi
}