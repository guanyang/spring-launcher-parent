#!/usr/bin/env bash
# Constants
readonly LAUNCHER_VERSION="@@@project.version@@@"
readonly LAUNCHER_RELEASE_DATE="@@@timestamp@@@"
readonly LAUNCHER_INFO="Launcher Version: ${LAUNCHER_VERSION}, Released on ${LAUNCHER_RELEASE_DATE}"
readonly LAUNCHER_CONFIG_INFO="___LAUNCHER_CONFIG_INFO___"
readonly LAUNCHER_IS_SPRINGBOOT_REPACKAGED="___LAUNCHER_IS_SPRINGBOOT_REPACKAGED___"

readonly LAUNCHER_PROJECT_INFO="Project [___LAUNCHER_PROJECT_ARTIFACT_ID___] build date: ___LAUNCHER_PROJECT_BUILD_DATE___"

readonly LAUNCHER_FILE_PATH=$(readlink -f $0)
readonly LAUNCHER_FILE_DIR=$(dirname "${LAUNCHER_FILE_PATH}")

readonly LAUNCHER_ACTION_TIMESTAMP=$(date '+%Y-%m-%dT%H_%M_%S')
readonly LAUNCHER_ACTION_TIMESTAMP_IN_SECONDS=$(date '+%s')

readonly LAUNCHER_RUNTIME_DIR=${LAUNCHER_FILE_DIR}/launcher_run
set -e; mkdir -p ${LAUNCHER_RUNTIME_DIR}; set +e

readonly LAUNCHER_START_ARGS_DIR=${LAUNCHER_RUNTIME_DIR}/start_args
set -e; mkdir -p ${LAUNCHER_START_ARGS_DIR}; set +e

readonly LAUNCHER_BANNER='LAUNCHER BASH'
# Variables init
ENABLE_CONSOLE_OUTPUT=true
SAVE_LAUNCHER_LOG=false
LAUNCHER_LOG_FILE="${LAUNCHER_FILE_DIR}/launcher_$(echo $$).log"


# Import libs
for f in ${LAUNCHER_FILE_DIR}/*/lib_*.sh; do source $f; done

# Init plugin list
PLUGIN_LIST=$(find ${LAUNCHER_FILE_DIR}/*/lib_plugin_*.sh -printf "%f\n" \
                | awk -F 'lib_plugin_' '{print $2}' \
                | awk -F '.sh' '{print $1}')

# Init plugin logger
for plugin in ${PLUGIN_LIST}; do
    eval ${plugin}_logger="plugin-${plugin}"
done

clean_on_exit() {
    # sleep 0.1s 解决末尾日志打印丢失的问题,
    # 丢失原因：整个启动脚本执行完毕后，会kill tail业务日志的进程，这个时候还没来得及输出就被kill了
    sleep 0.1

    launcher::action::disable_biz_log_print
    local LAUNCHER_tmp_log_file="${LAUNCHER_FILE_DIR}/launcher_$(echo $$).log"
    if [[ -f "${launcher_tmp_log_file}" ]]; then
        log_info "Delete launcher temporary log file: ${launcher_tmp_log_file}"
        rm -f "${launcher_tmp_log_file}"
    fi

    if [[ -f ${APP_PID_FILE} ]]; then
        log_info "Delete application pid file: ${APP_PID_FILE}"
        rm -f ${APP_PID_FILE}
    fi

    [[ ${LAUNCHER_ACTION} != "start" ]] && return

    print_all_background_process
    if [[ ${CLEAN_BACKEND_PROCESS} == true ]]; then
      kill_all_background_process ${LAUNCHER_STOP_TIMEOUT}
    fi

    if [[ ${APP_START_IN_DAEMON} == true && ${APP_FINAL_STATUS} == "SUCCESS" ]]; then
        log_info "Application [${APP_NAME}] status: RUNNING, pid: ${start_command_pid}"
    elif [[ ${APP_FINAL_STATUS} == "FAILED" ]]; then
        log_info "Application [${APP_NAME}] startup failed."
    else
        log_info "launcher exit."
    fi

    # 清除上次删除后打印的日志
    rm -f "${launcher_tmp_log_file}"

    # sleep解决使用Systemd时，最后日志输出被吞掉的问题
    sleep 0.1
}

launcher::abort() {
    APP_FINAL_STATUS="FAILED"
    log_warn "Terminate application start."
    CLEAN_BACKEND_PROCESS=true
    exit 1
}


trap clean_on_exit EXIT

readonly LAUNCHER_COMMAND_ARGS="$@"
main "$@"