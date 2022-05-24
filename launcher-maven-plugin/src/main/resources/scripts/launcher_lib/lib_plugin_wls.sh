#!/usr/bin/env bash
#
# 支持服务启动

readonly WL_DEFAULT_SERVICE_PORT=8091

launcher::plugin::wls() {
    launcher::plugin::wls::init_env
}

launcher::plugin::wls::init_env() {
    if [[ -n ${APP_ENV} ]]; then
        local wls_env=${APP_ENV}

        PLUGIN_JAVA_OPTS+=" -Dspring.profiles.active=${wls_env} "
        log_info "${wls_logger}" "Service application environment: ${wls_env}"
        log_info "${wls_logger}" "spring.profiles.active=${wls_env}"
    fi
}

launcher::plugin::wls::parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            * )
                return 1
                ;;
        esac
        shift
    done
}

# javaagent模块没开，用这种模式
launcher::plugin::wls::check_by_this_way() {
    local app_pid=${1}
    if [[ $(get_process_cmdline ${app_pid} | grep "\-Dlauncher.app.name=" | wc -l) -gt 0 ]]; then
        return 0
    fi
    return 1
}


launcher::plugin::wls::check_app_by_mode() {
    local app_pid=${1}
    local mode=${2} # healthy/ready
    local print_all_logs=${3:-true}

    if ! launcher::plugin::wls::check_by_this_way ${app_pid}; then
        return 2
    fi

    local app_http_port=$(get_process_cmdline ${app_pid} | awk -F '-Dlauncher.app.http.port=' '{print $2}' | awk '{print $1}' | head -1)
    if [[ -z ${app_http_port} ]]; then
        log_warn "${wls_logger}" "Application http port not found in start command, skip check(return check success)."
        return 0
    fi

    local check_url="http://127.0.0.1:${app_http_port}/hello"

    local check_result
    if check_result=$(launcher::action::request_url ${check_url}); then
        log_info "${wls_logger}" "Application is ${mode}."
        return 0
    else
        [[ ${print_all_logs} == true ]] && log_warn "${wls_logger}" "Application ${mode} check failed. Reason: ${check_result}"
        return 1
    fi
}


launcher::plugin::wls::usage() {
    print_arg_usage '--wls-enable'  "Start application with wls（true/false）"
}

launcher::plugin::wls::is_enabled() {
    echo "${wls_enable:=true}"
}

launcher::plugin::wls::name() {
    echo "Springboot Web Service"
}