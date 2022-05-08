#!/usr/bin/env bash
#
# JProfiler插件

readonly JPROFILER_DOWNLOAD_BASE_URL="http://jprofiler."

launcher::plugin::jprofiler() {

    local file_name=jprofiler_linux_9_2_1.tar.gz
    local dir_name=jprofiler9
    local download_target=/tmp/${file_name}

    if [[ ! -f ${download_target} ]]; then
        wget ${JPROFILER_DOWNLOAD_BASE_URL}/${file_name} -O ${download_target}
    fi

    if [[ ! -d "/opt/${dir_name}" ]]; then
        tar -zxf ${download_target} -C /opt
    fi

    readonly JPROFILER_DIR="/opt/jprofiler9"
    local jprofiler_ld=${JPROFILER_DIR}/bin/linux-x64

    if [[ $(echo ${LD_LIBRARY_PATH} | grep ${jprofiler_ld} | wc -l) -lt 1 ]];then
        export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:${jprofiler_ld}
    fi
}

launcher::plugin::jprofiler::download(){
    local file_name=jprofiler_linux_9_2_1.tar.gz
    local dir_name=jprofiler9
    local download_target=/tmp/${file_name}

    if [[ ! -f ${download_target} ]]; then
        wget https://download-gcdn.ej-technologies.com/jprofiler/${file_name} -O ${download_target}
    fi

    if [[ ! -d "/opt/${dir_name}" ]]; then
        tar -zxf ${download_target} -C /opt
    fi

    readonly JPROFILER_DIR="/opt/jprofiler9"
    local jprofiler_ld=${JPROFILER_DIR}/bin/linux-x64

    if [[ $(echo ${LD_LIBRARY_PATH} | grep ${jprofiler_ld} | wc -l) -lt 1 ]];then
        export LD_LIBRARY_PATH=${LD_LIBRARY_PATH}:${jprofiler_ld}
    fi
}

launcher::plugin::jprofiler::parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            "--jprofiler-port")
                if [[ ${2} -gt 1024 && ${2} -lt 65535 ]];then
                    PARAM_JAVAAGENT_VERSION="$2"
                else
                    log_error "Argument '--jprofiler-port' should be a number between 1024 to 65535"
                    launcher::abort
                fi
                DO_SHIFT=1
                shift
                ;;
            * )
                return 1
                ;;
        esac
        shift
    done
    return 1
}


launcher::plugin::jprofiler::usage() {
    print_arg_usage '--jprofiler-enable' 'Start with jprofiler and set port'
    print_arg_usage '--jprofiler-port '  "Set jprofiler port"
}

launcher::plugin::jprofiler::is_enabled() {
    echo "${jprofiler_enable:=false}"
}

launcher::plugin::jprofiler::name() {
    echo "JProfiler"
}