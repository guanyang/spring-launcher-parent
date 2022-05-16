#!/usr/bin/env bash
#
# javaagent

launcher::plugin::javaagent::init(){

    readonly JAVAAGENT_VERSION="@@@javaagent.download.version@@@"
    readonly JAVAAGENT_NAME="@@@javaagent.download.name@@@"
    readonly JAVAAGENT_DOWNLOAD_URL="@@@javaagent.download.url@@@"

    readonly JAVAAGENT_DIR="${APP_HOME}/plugins/javaagent"
    readonly JAVAAGENT_PATH="${JAVAAGENT_DIR}/skywalking-agent/skywalking-agent.jar"
    mkdir -p ${JAVAAGENT_DIR}
}

launcher::plugin::javaagent() {

    launcher::plugin::javaagent::init

    local backend_service=${JAVAAGENT_BACKEND_SERVICE}
    if [[ -z "${backend_service}" ]]; then
        log_info "JavaAgent backend_service url not set"
        launcher::abort
    fi

    local agent_dir=${JAVAAGENT_DIR}

    local agent_filename=${JAVAAGENT_NAME}

    local agent_path=${JAVAAGENT_PATH}

    local download_target=${agent_dir}/${agent_filename}

    if [[ ! -f ${agent_path} ]]; then
        local download_timeout=${LAUNCHER_PLUGIN_JAVAAGENT_DOWNLOAD_TIMEOUT:-60}
        log_info "${javaagent_logger}" "Javaagent ${agent_filename} not found locally, starting download(timeout ${download_timeout}s). (${JAVAAGENT_DOWNLOAD_URL})"
        # 本地文件不存在，下载
        if [[ ! -f ${download_target} ]]; then
            if timeout ${download_timeout} wget --no-check-certificate --progress=dot:mega -O ${download_target} ${JAVAAGENT_DOWNLOAD_URL} 2>&1; then
                # 下载成功
                log_info "${javaagent_logger}" "Javaagent ${agent_filename} download success."
            else
                # 下载失败
                log_error "${javaagent_logger}" "JavaAgent [${agent_filename}] download failed. (${JAVAAGENT_DOWNLOAD_URL})"
            fi
        else
           log_info "${javaagent_logger}" "JavaAgent file [${download_target}] has been already exists, ignore download."
        fi

        # 解压文件
        if [[ -f ${download_target} ]]; then
            tar -zxf ${download_target} -C ${agent_dir}
            # 删除原压缩文件
            rm -f ${download_target}
        fi
    fi

    # 设置环境变量
    if [[ -f ${agent_path} ]]; then
        PLUGIN_JAVA_OPTS+="-javaagent:${agent_path} -Dskywalking.agent.service_name=${APP_NAME} -Dskywalking.collector.backend_service=${backend_service}"
    else
        log_error "${javaagent_logger}" "JavaAgent init failed"
        launcher::abort
    fi
}


launcher::plugin::javaagent::check_by_this_way() {
    local app_pid=${1}
    if [[ $(get_process_cmdline ${app_pid} | grep '/plugins/javaagent/skywalking-agent' | wc -l) -gt 0 ]]; then
        return 0
    fi
    return 1
}


launcher::plugin::javaagent::check_app_by_mode() {
    local app_pid=${1}
    local mode=${2} # healthy/ready
    local print_all_logs=${3:-true}
    if ! launcher::plugin::javaagent::check_by_this_way ${app_pid}; then
        return 2
    fi

    local app_http_port=$(get_process_cmdline ${app_pid} | awk -F '-Dlauncher.app.http.port=' '{print $2}' | awk '{print $1}' | head -1)
    if [[ -z ${app_http_port} ]]; then
        log_warn "${javaagent_logger}" "Application http port not found in start command, skip check(return check success)."
        return 0
    fi

    local check_url="http://127.0.0.1:${app_http_port}/hello"

    local check_result
    if check_result=$(launcher::action::request_url ${check_url}); then
        log_info "${javaagent_logger}" "Application is ${mode}."
        return 0
    else
        [[ ${print_all_logs} == true ]] && log_warn "${javaagent_logger}" "Application ${mode} check failed. Reason: ${check_result}"
        return 1
    fi
}


launcher::plugin::javaagent::parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            "--javaagent-backend_service")
                readonly JAVAAGENT_BACKEND_SERVICE=${2}
                DO_SHIFT=1
                shift
                ;;
            * )
                return 1
                ;;
        esac
        shift
    done
}


launcher::plugin::javaagent::usage() {
    print_arg_usage '--javaagent-enable' '(default) Start application with JavaAgent'
    print_arg_usage '--javaagent-backend_service' 'Set backend_service url'
}

launcher::plugin::javaagent::is_enabled() {
    echo "${javaagent_enable:=true}"
}

launcher::plugin::javaagent::name() {
    echo "JavaAgent"
}

