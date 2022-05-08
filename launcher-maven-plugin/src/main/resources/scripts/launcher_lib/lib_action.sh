#!/usr/bin/env bash
#
# launcher相关操作(start/stop/restart)


launcher::action::start() {
    launcher::init_env
    launcher::init_app

    launcher::action::excute_cmd_before_detonate
    launcher::action::detonate
}

launcher::action::stop() {
    launcher::init_env
    launcher::init_app
    launcher::action::do_stop
}

launcher::action::do_stop() {

    if [[ -z "${APP_NAME}" ]]; then
        echo "Application name should be set in service.properties or args"
        launcher::abort
    fi

     local service_pid=$(launcher::action::get_service_pid ${APP_HOME} ${APP_NAME})

     if [[ -z ${service_pid} ]]; then
        log_info "Application is not running"
        return
     fi

    log_info "Application is running with pid:${service_pid}, stopping..."

    # Write launcher stop message to biz log
    local app_base_log_dir=$(get_process_cmdline ${service_pid} | awk -F '-Dlauncher.app.log.dir=' '{print $2}' | awk '{print $1}'  | head -1)
    local app_biz_log_file=${app_base_log_dir}/biz/${APP_NAME}.biz.log
    if [[ -f ${app_biz_log_file} ]]; then
        echo "$(create_log_msg 'INFO' 'stop' 'Stop Application by '$(whoami) )" >> ${app_biz_log_file}
    fi

#    launcher::plugin::javaagent::request_shutdown_url

    if [[ -z ${APP_FORCE_KILL} ]]; then
        kill_process ${service_pid} ${LAUNCHER_STOP_TIMEOUT}
    else
        log_info "Force kill process(${service_pid})."
        kill -9 ${service_pid}
    fi
}


launcher::action::restart() {

    launcher::init_env
    launcher::init_app

    local app_start_args_file=${LAUNCHER_START_ARGS_DIR}/${APP_NAME}

    local launcher_command_args
    if [[ ! -f ${app_start_args_file} ]]; then
        log_error "Last start command args file not found, abort restart"
        launcher::abort
    fi

    launcher_command_args=$(cat ${app_start_args_file})

    if [[ -z ${launcher_command_args} ]]; then
        log_error "Last start command args is null, abort restart"
        launcher::abort
    fi

    launcher::action::do_stop

    bash ${LAUNCHER_FILE_PATH} ${launcher_command_args}
}



#############################################################
# 判断进程是否已经启动
# Globals:
#   APP_HOME
#   APP_NAME
# Arguments:
#   none
# Returns:
#   0: 已启动
#   1: 未启动
#############################################################
launcher::action::is_started(){
    local service_pid=$(launcher::action::get_service_pid ${APP_HOME} ${APP_NAME})
    if [[ ${service_pid} -gt 0 ]]; then
        return 0
    else
        return 1
    fi
}

#############################################################
# 检查APP是否正常
# Globals:
#   APP_CHECK_END_TIME: 检查终止时间
#   IN_TERMINATING: 处于正在关闭状态
# Arguments:
#   ${1}: 进程ID
#   ${2}: 模式(healthy / ready)
# Returns:
#   0: 已启动
#   1: 未启动
#############################################################
launcher::action::check_app_by_mode() {
    local app_pid=${1}
    local mode=${2} # healthy or ready
    local print_all_logs=false
    local is_the_last_retry
    local start_time=$(date +'%s')
    local cost
    if [[ -z ${APP_CHECK_END_TIME} ]]; then
        is_the_last_retry=true
    fi

    log_info "Start application ${mode} status checking..."
    if ! process_exists ${app_pid}; then
        [[ ${IN_TERMINATING} != true ]] && log_error "Process [${app_pid}] exited unexpectedly."
        launcher::abort
    fi

    local check_plugin
    if launcher::plugin::wls::check_by_this_way ${app_pid}; then
        check_plugin=wls
    else
        log_info "Plugin wls was not enable or not match this application, ${mode} check skipped."
        return 0
    fi

    log_info "Application ${mode} status check mode: ${check_plugin}"

    local check_result
    while true; do
        # 如果是最后一次检查，把日志打开，超时原因就会被打印
        [[ ${is_the_last_retry} == true ]] && print_all_logs=true

        if ! process_exists ${app_pid}; then
            [[ ${IN_TERMINATING} != true ]] && log_error "Process [${app_pid}] exited unexpectedly."
            launcher::abort
        fi

        launcher::plugin::${check_plugin}::check_app_by_mode ${app_pid} ${mode} ${print_all_logs}
        check_result=$?

        # 探活成功
        if [[ ${check_result} == 0 ]]; then
          cost=$(expr $(date +'%s') - ${start_time})
          log_info "Success for ${mode} checking. (cost: ${cost}s)"
          return 0
        fi

        # 探活不成功，并且已经到达最后一轮（超时）
        if [[ ${is_the_last_retry} == true ]]; then
          cost=$(expr $(date +'%s') - ${start_time})
          log_error "Timed out(cost: ${cost}s) for ${mode} checking."
          return 1
        fi

        [[ $(date +'%s') -gt ${APP_CHECK_END_TIME} ]] && is_the_last_retry=true
        sleep 1
    done
}


launcher::action::status(){

    launcher::init_env
    launcher::init_app

    if [[ -z "${APP_NAME}" ]]; then
        log_info "Application name not set"
        launcher::abort
    fi

    local app_pid=$(launcher::action::get_service_pid ${APP_HOME} ${APP_NAME})
    if [[ ${app_pid} -ne 0 ]]; then
        log_info "Application [${APP_NAME}] is running with pid: ${app_pid}"
    else
        log_info "Application [${APP_NAME}] is not started."
        launcher::abort
    fi

    local application_is_healthy
    if launcher::action::check_app_by_mode ${app_pid} "healthy"; then
        application_is_healthy=true
    else
        application_is_healthy=false
    fi

    local application_is_ready
    if launcher::action::check_app_by_mode ${app_pid} "ready"; then
        application_is_ready=true
    else
        application_is_ready=flase
    fi

    if [[ ${CHECK_HEALTHY_PARAM} == true && ${application_is_healthy} != "true" ]]; then
        launcher::abort
    fi

    if [[ ${CHECK_READY_PARAM} == true && ${application_is_ready} != "true" ]]; then
        launcher::abort
    fi
}

launcher::action::check_app_started() {
    local app_pid=${1}

    log_info "Wait for application start, timeout: ${PARAM_START_TIMEOUT:-${LAUNCHER_START_TIMEOUT:-120}}s"
    APP_CHECK_END_TIME=$(expr "$(date +'%s')" + "${PARAM_START_TIMEOUT:-${LAUNCHER_START_TIMEOUT:-120}}")


    ! launcher::action::check_app_by_mode ${app_pid} "healthy" && return 1

    ! launcher::action::check_app_by_mode ${app_pid} "ready" && return 1

    return 0
}

launcher::action::request_url() {
    local check_url=${1}

    local check_result=$(curl -4 --connect-timeout 5 -s -w '\n%{http_code}__placeholder\n' ${check_url})
    local check_status_code=$(echo ${check_result##*$'\n'} | awk -F '__placeholder' '{print $1}')
    local check_msg=$(echo ${check_result} | awk -F ${check_status_code}__placeholder '{print $1}')

    # 检查应用是否健康（可用）
    if [[ ${check_status_code} != 200 ]]; then
        echo "Request url [${check_url}] failed! Return code: ${check_status_code}, Message: ${check_msg}"
        return 1
    fi
}

#############################################################
# 处理terminate以及interrupt事件
# Globals:
#   LAUNCHER_SIG_TERM_DELAY 延迟等待时间
# Arguments:
#   none
# Returns:
#   none
#############################################################
launcher::action::process_signal_term_int() {

    log_warn "Caught terminate/interrupt signal";
    readonly IN_TERMINATING=true

    [[ ${LAUNCHER_SIG_TERM_DELAY} && ${LAUNCHER_SIG_TERM_DELAY} =~ ^[0-9]+$ && ${LAUNCHER_SIG_TERM_DELAY} -gt 0 ]] && log_info "Wait for ${LAUNCHER_SIG_TERM_DELAY}s" && sleep ${LAUNCHER_SIG_TERM_DELAY}

#    launcher::plugin::javaagent::request_shutdown_url

    trap - TERM INT

    print_all_background_process
    kill_all_background_process ${LAUNCHER_STOP_TIMEOUT}
}


#############################################################
# 获取服务进程ID
# Globals:
#   none
# Arguments:
#   $1: APP_HOME
#   $2: APP_NAME
# Returns:
#   STDOUT: Application pid
#############################################################
launcher::action::get_service_pid(){
    local app_home=${1}
    local app_name=${2}

    local app_pid_file=${app_home}/bin/launcher_run/${app_name}/pid
    local app_pid
    if [[ -f ${app_pid_file} ]]; then
        app_pid=$(cat ${app_pid_file})
        if [[ $(cat /proc/${app_pid}/cmdline 2>/dev/null | grep -a '\-Dlauncher.app.name='${2} | grep -a '\-Dlauncher.app.home='${1} | wc -l) == 1 ]]; then
            echo ${app_pid}
            return
        fi
    fi
    ps --no-headers ww -o pid,cmd -C java |  grep -w '\-Dlauncher.app.name='${2} | grep -w '\-Dlauncher.app.home='${1} | awk '{print $1}' | head -1
}


launcher::action::save_app_pid() {
    local app_name=${1}
    local app_pid=${2}

    set -e; mkdir -p ${LAUNCHER_RUNTIME_DIR}/${app_name}; set +e
    echo "${app_pid}" > ${LAUNCHER_RUNTIME_DIR}/${app_name}/pid

    readonly APP_PID_FILE=${LAUNCHER_RUNTIME_DIR}/${app_name}/pid
}

###################################################################
# 在应用执行启动环节前执行命令，如果命令执行失败（exit_code!=0）则终止应用启动
###################################################################
launcher::action::excute_cmd_before_detonate(){
  if [[ -z ${LAUNCHER_CMD_BEFORE_DETONATE} ]]; then
    return
  fi

  log_info "Execute command in environment variable 'LAUNCHER_CMD_BEFORE_DETONATE': ${LAUNCHER_CMD_BEFORE_DETONATE}"
  local exit_code
  eval "${LAUNCHER_CMD_BEFORE_DETONATE}"
  exit_code=$?

  if [[ ! ${exit_code} == 0 ]]; then
    log_error "Failed to execute command in environment variable 'LAUNCHER_CMD_BEFORE_DETONATE'. Exit code ${exit_code}"
    launcher::abort
  fi

  log_info "Command in environment variable 'LAUNCHER_CMD_BEFORE_DETONATE' execute success."
}

#############################################################
# 启动应用
# Globals:
#   JAVA_HOME
#   APP_CLASSPATH
#   APP_JAVA_OPTS
#   APP_MAIN_CLASS
#   APP_START_IN_DAEMON
#   DRY_RUN
#   APP_LOG_DIR
#   LAUNCHER_LOG_FILE
#   APP_NAME
#   SERVICE_PID
# Arguments:
#   none
# Returns:
#   START_COMMAND_PID
#############################################################
launcher::action::detonate() {

    local start_cmd
    if [[ ${LAUNCHER_IS_SPRINGBOOT_REPACKAGED} == true ]]; then
        start_cmd+="${JAVA_HOME}/bin/java -cp ${APP_CLASSPATH} ${APP_JAVA_OPTS} -Dloader.main=${APP_MAIN_CLASS} org.springframework.boot.loader.PropertiesLauncher ${APP_ARGS}"
    else
        start_cmd+="${JAVA_HOME}/bin/java -cp ${APP_CLASSPATH} ${APP_JAVA_OPTS} ${APP_MAIN_CLASS} ${APP_ARGS}"
    fi

    if [[ ${LAUNCHER_KEEP_STDOUT_FORMAT} == 1 || ${LAUNCHER_KEEP_STDOUT_FORMAT} == true ]]; then
      # 添加 IFS= 可以保持标准输出中前后两段的空格及制表符，但是当服务器环境变量很多的(env | wc -l)情况下，这个动作会影响标准输出的性能（阿里云环境）
      # https://unix.stackexchange.com/questions/26784/understanding-ifs
      start_cmd="${start_cmd} </dev/null 2>&1 | while IFS= read -r line; do echo \"\${line}\" >> ${APP_LOG_DIR}/biz/${APP_NAME}.biz.log; done &"
    else
      start_cmd="${start_cmd} </dev/null 2>&1 | while read -r line; do echo \"\${line}\" >> ${APP_LOG_DIR}/biz/${APP_NAME}.biz.log; done &"
    fi

    if [[ ${APP_START_IN_DAEMON} == true ]]; then
        log_info "Start application in daemon mode"
        start_cmd="nohup ${start_cmd}"
    fi

    if [[ -n ${PARAM_VARIABLES} ]]; then
        start_cmd="${PARAM_VARIABLES} ${start_cmd}"
    fi

    log_info "Application start command: ${start_cmd}"
    log_info "Application starting, please wait..."

    if [[ -n ${DRY_RUN} ]]; then
        log_info "Dry run finished"
        return
    fi

    trap 'launcher::action::process_signal_term_int' TERM INT

    launcher::action::enable_biz_log_print

    eval "${start_cmd}"
    sleep 1
    readonly start_command_pid=$(jobs -l | grep 'bin/java -cp' | awk '{print $2}')
    if [[ -z ${start_command_pid} ]]; then
        log_error "Application java process not found, may be killed by kernel OOM Killer, please check /var/log/message for more information"
        launcher::abort
    fi
    log_info "Application java process id: [${start_command_pid}]"
    launcher::action::save_app_pid ${APP_NAME} ${start_command_pid}

    # 如果check失败（超时）则终止启动
    ! launcher::action::check_app_started ${start_command_pid} && launcher::abort

    echo "${LAUNCHER_COMMAND_ARGS}" > ${LAUNCHER_START_ARGS_DIR}/${APP_NAME}
    readonly APP_FINAL_STATUS="SUCCESS"
    log_info "Application [${APP_NAME}] start success(cost: $(expr $(date +'%s') - ${LAUNCHER_ACTION_TIMESTAMP_IN_SECONDS})s), pid:${start_command_pid}"


    # 前台执行，等待进程运行结束
    if [[ ${APP_START_IN_DAEMON} == false ]]; then
        if [[ ${APP_LOG_ON_CONSOLE} == false ]]; then
            launcher::action::disable_biz_log_print
        fi

        # TODO 该方式在其他终端里进行kill时，探测不到具体exit code
        local app_exit_code
        wait ${start_command_pid}
        app_exit_code=$?

        # 取消ShutdownHook
        trap - TERM INT

        wait ${start_command_pid}

        log_info "Application [${APP_NAME}] exit with code ${app_exit_code}"
        CLEAN_BACKEND_PROCESS=true
    fi

}

#############################################################
# 启用业务日志打印
# 该方法执行后可以打印出业务日志以及launcher脚本日志
#############################################################
launcher::action::enable_biz_log_print(){
    [[ ${TAIL_LOG_PID} -gt 0 ]] && return
    log_info "Enable biz log forwarding to console output."
    ENABLE_CONSOLE_OUTPUT=false
    tail -n 0 -F ${APP_LOG_DIR}/biz/${APP_NAME}.biz.log | print_with_color &
    TAIL_LOG_PID=$(jobs -l | grep 'tail -n 0 -F' | awk '{print $2}')
}

#############################################################
# 关闭业务日志打印
# 该方法执行后只能印出launcher脚本日志
#############################################################
launcher::action::disable_biz_log_print(){
    [[ ! ${TAIL_LOG_PID} -gt 0 ]] && return
    ! process_exists ${TAIL_LOG_PID} && return

    log_info "Disable biz log forwarding to console output."
    log_info "Kill process(biz log forwarding) ${TAIL_LOG_PID}."
    kill ${TAIL_LOG_PID} 2>/dev/null
    ENABLE_CONSOLE_OUTPUT=true

    log_info "Process(biz log forwarding)[${TAIL_LOG_PID}] exited."

    TAIL_LOG_PID=""
}