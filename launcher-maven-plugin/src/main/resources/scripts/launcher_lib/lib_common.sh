#!/usr/bin/env bash
#
# 公共函数

######################################################
# 解析JVM参数配置文件，将文件中的JVM参数全部提取出来并拼接成一行
# Globals:
#   none
# Arguments:
#   $1: JVM参数文件名
# Returns:
#   将结果输出到STDOUT
# Usage:
#   JVM_OPTS=$(parse_jvm_options "./jvm.options")
######################################################
parse_jvm_options() {
  if [[ -f "$1" ]]; then
    echo "$(grep "^-" "$1" | tr '\n' ' ')"
  fi
}

#############################################################################
# 从配置文件中获取到某个配置项的值，配置项与值以'='分隔
# Globals:
#   none
# Arguments:
#   $1: 配置项名
#   $2: JVM参数文件名
# Returns:
#   将结果输出到STDOUT
# Usage:
#   MAIN_CLASS="$(get_property_in_file "MAIN_CLASS" "./service.properties" )"
#############################################################################
get_property_in_file() {
  if [[ -f "$2" ]]; then
    echo "$(grep "^$1=" "$2" | tail -1 | cut -d "=" -f 2 )"
  fi
}

#############################################################
# 日志
# Globals:
#   LAUNCHER_LOG_FILE: 启动器日志文件位置
#   SAVE_LAUNCHER_LOG: 是否保存日志文件到${LAUNCHER_LOG_FILE}中
# Arguments:
#   $1: 日志级别
#   $2: 日志文本
# Returns:
#   None
#############################################################
log_generic (){
    local log_msg=$(create_log_msg "$@")

    if [[ ${SAVE_LAUNCHER_LOG} == true ]]; then
        echo "${log_msg}" >> "${LAUNCHER_LOG_FILE}"
    fi

    [[ ${ENABLE_CONSOLE_OUTPUT} == true ]] && print_msg_with_colored_log_level "${log_msg}"
    return 0
}

log_banner(){
  echo "${LAUNCHER_BANNER}"
  if [[ ${SAVE_LAUNCHER_LOG} == true ]]; then
      echo "${LAUNCHER_BANNER}" >> "${LAUNCHER_LOG_FILE}"
  fi
}

log_info(){
    log_generic "INFO" "$@"
}

log_warn(){
    log_generic "WARN" "$@"
}

log_error(){
    log_generic "ERROR" "$@"
}

create_log_msg() {
    local log_level=$1
    local logger
    local log_content
    if [[ $# == 3 ]]; then
        logger=$2
        log_content=$3
    else
        logger="main"
        log_content=$2
    fi

    local log_pattern="$(date '+%Y-%m-%dT%H:%M:%S.%3N%z') | %-5s | N/A | launcher | %s | %s\n"
    printf "${log_pattern}" "${log_level}" "${logger}" "${log_content}"
}

print_msg_with_colored_log_level() {
    local log_msg="${1}"

    if [[ ${LAUNCHER_COLORED_CONSOLE_OUTPUT} == false || ${LAUNCHER_COLORED_CONSOLE_OUTPUT} == 0 ]]; then
        echo "${log_msg}"
        return
    fi

    local color="\033[0m"
    if [[ ${log_msg} == *" | ERROR "* ]]; then
        color="\033[1;31m"
    elif [[ ${log_msg} == *" | WARN "* ]]; then
        color="\033[1;33m"
    fi
    printf "${color}%s\n" "${log_msg}"
}

print_with_color() {
    while IFS= read -r data; do
        print_msg_with_colored_log_level "${data}"
    done
}

print_arg_usage() {
    local short_arg=$1
    local long_arg=$2
    local arg_desc=$3
    if [[ $# == 2 ]]; then
        if [[ $1 == --* ]]; then
            short_arg=""
            long_arg=$1
        else
            short_arg=$1
            long_arg=""
        fi
        arg_desc=$2
    fi
    if [[ -n ${short_arg} ]]; then
        short_arg="${short_arg},"
    fi
    printf '    '
    printf '%-5s' "${short_arg}"
    printf ' '
    printf '%-20s' "${long_arg}"
    printf '          '
    printf  '%s' "${arg_desc}"
    printf  '\n'
}

get_process_cmdline() {
    local pid=${1}
    cat /proc/${pid}/cmdline 2>/dev/null | tr "\0" " "
}

check_is_valid_port_range() {
    local port=${1}
    if [[ ${port} =~ ^[0-9]+$ && ${port} -gt 0 && ${port} -lt 65535 ]];then
        return 0
    else
        log_error "Invalid port '${port}', port should be a number between 1024 to 65535"
        launcher::abort
    fi
}

#############################################################
# 判断进程是否存在
# Arguments:
#   $1: 进程PID
# Returns:
#   0： 存在
#   1： 不存在
#############################################################
process_exists() {
    local app_pid=${1}
    if [[ -e /proc/${app_pid} ]]; then
        return 0
    fi
    return 1
}

#############################################################
# 等待某个进程结束，如果超过指定时间则返回1
# Arguments:
#   $1: 进程PID
#   $2: 超时时间
# Returns:
#   0： 正常退出
#   1： 等待超时
#############################################################
wait_process_with_timeout(){
    local pid=${1}
    local time_out_sec=${2:-45}

    [[ -z ${pid} ]] && return

    log_info "Wait for the process[${pid}] exit(timeout ${time_out_sec}s)."

    declare -i time_sec=1
    while process_exists ${pid}
    do
        if [[ ${time_sec} -gt ${time_out_sec} ]]; then
            log_warn "Process[${pid}] exit timeout(${time_out_sec}s)."
            return 1
        fi
        time_sec+=1
        sleep 1
    done
    return 0
}
#############################################################
# 杀死某个进程，如果超过指定时间，使用-9强杀
# Arguments:
#   $1: 进程PID
#   $2: 超时时间
#############################################################
kill_process(){
    local pid=${1}
    local time_out_sec=${2:-45}

    if ! process_exists ${pid}; then
      return
    fi

    log_info "Kill process ${pid}."
    kill ${pid} 2>/dev/null &

    if ! wait_process_with_timeout ${pid} ${time_out_sec}; then
      log_info "Force kill(-9) process ${pid}."
      kill -9 ${pid}
    fi
    log_info "Process[${pid}] exited."
}

#############################################################
# 打印当前进程的所有后台进程（子进程）
#############################################################
print_all_background_process(){
  local job_list=$(jobs -r -l 2>&1)

  if [[ -z ${job_list} ]]; then
    return
  fi

  log_info "Background process list:
${job_list}"
}
#############################################################
# 杀死所有当前进程的后台进程（子进程）
# Arguments:
#   $1: 超时时间
#############################################################
kill_all_background_process(){
  local time_out_sec=${1:-45}
  local job_list=$(jobs -r -p | sort -r)
  [[ -z ${job_list} ]] && return

  local last_job_pid=$(echo "${job_list}" | tail -n 1)

  echo "${job_list}" | while read pid; do
    [[ ${pid} != ${last_job_pid} ]] && kill_process ${pid} ${time_out_sec}
  done

  # 最后一个进程通常是tail -f 业务日志的进程，如果在上面循环体内关闭则会导致launcher日志也无法输出
  # 这里我们先禁用业务日志输出，这样程序会将日志输出模式从tail -f切换到原生日志打印（echo）
  launcher::action::disable_biz_log_print
  kill_process ${last_job_pid} ${time_out_sec}

  log_info "All background processes have been killed."
}