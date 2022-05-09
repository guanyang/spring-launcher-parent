#!/usr/bin/env bash
#
# 入口函数,参数处理

function main {
    readonly LAUNCHER_ACTION=$1
    case $1 in
    "start" )
        SAVE_LAUNCHER_LOG=true
        shift
        while [[ $# -gt 0 ]]
        do
            case $1 in
                "-d" | "--daemon" )
                    readonly PARAM_START_IN_DAEMON=true
                    ;;
                "-cp" | "--classpath" )
                    echo "Unsupported argument '${1}'"
                    launcher::abort
                    readonly PARAM_CLASSPATH="$2"
                    shift
                    ;;
                "-e" | "--app-environment")
                    PARAM_APP_ENV="$2"
                    shift
                    ;;
                "-p" | "--server-port")
                    check_is_valid_port_range ${2}
                    PARAM_APP_HTTP_PORT="$2"
                    shift
                    ;;
                "-r" | "--app-region")
                    PARAM_APP_REGION="$2"
                    shift
                    ;;
                "-v" | "--var")
                    PARAM_VARIABLES+=" $2"
                    shift
                    ;;
                "-n" | "--app-name")
                    PARAM_APP_NAME="$2"
                    shift
                    ;;
                "-mc" | "--main-class")
                    readonly PARAM_MAIN_CLASS="$2"
                    shift
                    ;;
                "-a" | "--args")
                    PARAM_APP_ARGS+=" $2"
                    shift
                    ;;
                "-i" | "--app-instance-id")
                    readonly PARAM_APP_INSTANCE_ID="$2"
                    shift
                    ;;
                "-jo" | "--java-opts")
                    PARAM_JAVA_OPTS+=" $2"
                    shift
                    ;;
                "-ld" | "--log-dir")
                    readonly PARAM_LOG_DIR=${2}
                    if [[ ${PARAM_LOG_DIR} != /* ]]; then
                        echo "Argument --log-dir value [${2}] should be a absolute directory path (e.g. /home/www/logs) "
                        launcher::abort
                    fi

                    if [[ ! -f ${PARAM_LOG_DIR} && ! -d ${PARAM_LOG_DIR} ]]; then
                        echo "Log directory [${PARAM_LOG_DIR}] not exists, create"
                        # exit when mkdir failed
                        set -e; mkdir -p ${PARAM_LOG_DIR}; set +e
                    else
                        if [[ ! -w ${PARAM_LOG_DIR} ]]; then
                            echo "Argument --log-dir value [${PARAM_LOG_DIR}] has no writable permission"
                            launcher::abort
                        fi
                    fi

                    shift
                    ;;
                "-ldt" | "--log-dir-type")
                    readonly PARAM_LOG_DIR_TYPE=$(echo ${2} | tr 'a-z' 'A-Z')
                    if [[ ! ${PARAM_LOG_DIR_TYPE} =~ ^(BASE|APP|INSTANCE)$ ]]; then
                        echo "Param --log-dir-type value [${2}] should be in base/app/instance"
                        launcher::abort
                    fi
                    shift
                    ;;
                "-loc" | "--log-on-console")
                    [[ ${2} == true ]] && readonly PARAM_LOG_ON_CONSOLE=true || readonly PARAM_LOG_ON_CONSOLE=false
                    shift
                    ;;
                "-st" | "--start-timeout")
                    if ! [[ ${2} =~ ^[0-9]+$ && ${2} -gt 0 ]]; then
                        log_error "Argument '${1}' value should be a number, current is '${2}'"
                        launcher::abort
                    fi
                    PARAM_START_TIMEOUT="$2"
                    shift
                    ;;
                "--restart")
                    PARAM_RESTART=true
                    ;;
                "--dry-run")
                    readonly DRY_RUN=true
                    ;;
                "-h" | "--help" )
                    launcher::main::print_start_usage
                    launcher::main::print_plugin_usage
                    echo ""
                    exit 0
                    ;;
                --*-enable )
                    # 参数是-PLUGIN_NAME-enable格式，开启指定插件
                    local target_plugin
                    for plugin in ${PLUGIN_LIST}; do
                        if [[ $1 != "--${plugin}-enable" ]]; then
                            continue
                        fi

                        target_plugin=${plugin}
                        if [[ ${2} == -* || -z ${2} ]]; then
                            eval ${plugin}_enable=true
                            break
                        fi

                        # with args
                        if [[ ${2} =~ ^(true|false)$  ]]; then
                            eval ${plugin}_enable=${2}
                            shift
                            break
                        else
                            echo "The args ${1} value expect with true or false, value is ${2}"
                            launcher::abort
                        fi
                    done

                    if [[ -z ${target_plugin} ]]; then
                        echo "Unknow option $1"
                        echo "See '$0 start --help'"
                        echo ""
                        exit 1
                    fi
                    ;;
                * )
                    # 判断是否是-PLUGIN开头，如果是的则使用指定插件去解析他的参数
                    local target_plugin
                    for plugin in ${PLUGIN_LIST}; do
                        if [[ $1 != --${plugin}-* ]]; then
                            continue
                        fi

                        target_plugin=${plugin}
                        if launcher::plugin::${plugin}::parse_args $1 $2; then
                            if [[ ${DO_SHIFT} == 1 ]]; then
                                shift
                                break
                            fi
                        else
                            echo "Unknow option $1"
                            echo "See '$0 start --help'"
                            echo ""
                            exit 1
                        fi
                    done


                    if [[ -z ${target_plugin} ]]; then
                        echo "Unknow option $1"
                        echo "See '$0 start --help'"
                        echo ""
                        exit 1
                    fi
            esac
            shift
        done
        log_banner
        log_info "${LAUNCHER_INFO}"
        log_info "${LAUNCHER_CONFIG_INFO}"
        log_info "${LAUNCHER_PROJECT_INFO}"
        log_info "Current user: $(whoami)"
        launcher::main::check_plugin_status
        launcher::action::start
        ;;
    "stop" )
        shift
        while [[ $# -gt 0 ]]
        do
            case $1 in
                "-f" | "--force")
                     APP_FORCE_KILL=true
                     ;;
                "-n" | "--app-name")
                     PARAM_APP_NAME="$2"
                     shift
                     ;;
                * )
                     launcher::main::print_stop_usage
                     echo ""
                     exit 0
                     ;;
            esac
            shift
        done
        launcher::action::stop
        ;;
    "restart" )
        shift
        while [[ $# -gt 0 ]]
        do
            case $1 in
                "-f" | "--force")
                     APP_FORCE_KILL=true
                     ;;
                "-n" | "--app-name")
                     PARAM_APP_NAME="$2"
                     shift
                     ;;
                * )
                     launcher::main::print_restart_usage
                     echo ""
                     exit 0
                     ;;
            esac
            shift
        done
        launcher::action::restart
        ;;
    "status" )
        shift
        while [[ $# -gt 0 ]]
        do
            case $1 in
                "-n" | "--app-name")
                     PARAM_APP_NAME="$2"
                     shift
                     ;;
                "-ch" | "--check-healthy")
                     CHECK_HEALTHY_PARAM=true
                     ;;
                "-cr" | "--check-ready")
                     CHECK_READY_PARAM=true
                     ;;
                * )
                     launcher::main::print_status_usage
                     echo ""
                     exit 0
                     ;;
            esac
            shift
        done
        launcher::action::status
        ;;
    "-h" | "--help" )
        launcher::main::print_usage
        exit 0
        ;;
    * )
        echo "Unknow command $1"
        echo "See '$0 --help'"
        echo ""
        exit 0
    esac
}

######################################################
# 打印插件的使用说明
# Globals:
#   PLUGIN_LIST: 插件列表
# Arguments:
#   none
# Returns:
#   none
######################################################
launcher::main::print_plugin_usage() {
    for plugin in ${PLUGIN_LIST}; do
        echo ""
        echo "Plugin $(launcher::plugin::${plugin}::name):"
        launcher::plugin::${plugin}::usage
    done
}

######################################################
# 使用插件去解析启动参数
# Globals:
#   PLUGIN_LIST: 插件列表
# Arguments:
#   $1: 参数1
#   $2: 参数2
# Returns:
#   none
######################################################
launcher::main::parse_args_by_plugin() {
    for plugin in ${PLUGIN_LIST}; do
        launcher::plugin::${plugin}::parse_args $1 $2
        if [[ $? == 0 ]]; then
            return 0
        fi
    done
    return 1
}


launcher::main::check_plugin_status() {
    for plugin in ${PLUGIN_LIST}; do
        log_info "Plugin [$(launcher::plugin::${plugin}::name)] enabled: $(launcher::plugin::${plugin}::is_enabled)"
    done
}


launcher::main::print_usage() {
cat << EOF
Usage:  $0 COMMAND [arg...]

Commands:
    start       Start an application
    stop        Stop an application
    restart     Restart an application
    status      Show application status
    shortcut    Create command shortcut
EOF
}

launcher::main::print_start_usage() {
    printf "Usage:  $0 start [OPTIONS]\n\n"
    printf "Example:  $0 start -d -n MyService --args 'arg1 arg2 arg3' -jo '-Xmx2g'\n\n"
    printf "Start an application\n\n"
    printf 'Options:\n'

    print_arg_usage '-a'    '--args'            "Set application args (e.g. -a 'customArg1 customArg2' )"
    #print_arg_usage '-cp'   '--classpath'       'Set classpath'
    print_arg_usage '-d'    '--daemon'          'Run the application in background'
    print_arg_usage '-e'    '--app-environment' 'Set application environment [prod/pre/stress/live/dev] (e.g. -e dev)'
    print_arg_usage '-r'    '--app-region'      'Set application region'
    print_arg_usage '-v'    '--var'             'Set system variables (e.g. -v LD_LIBRARY_PATH=/opt/bin)'
    print_arg_usage '-i'    '--app-instance-id' 'Set application instance id manually'
    print_arg_usage '-jo'   '--java-opts'       "Set java opts (e.g. -jo '-Xmx4g -Xms4g')"
    print_arg_usage '-ld'   '--log-dir'         'Set log directory (e.g. --ld /home/www/logs）'
    print_arg_usage '-ldt'  '--log-dir-type'    'Set log directory type, default base [base/app/instance] (e.g. -ldt instance)'
    print_arg_usage '-loc'  '--log-on-console'  'Print biz logs on console in non-daemon mode (e.g. -loc true) (default true)'
    print_arg_usage '-mc'   '--main-class'      'Set application main class to start (e.g. -mc org.gy.framework.sample.main)'
    print_arg_usage '-n'    '--app-name'        'Set application name (e.g. -n launcher-sample)'
    print_arg_usage '-p'    '--server-port'     'SpringBoot web container port(-Dserver.port)'
    print_arg_usage '-st'   '--start-timeout'   'Set start timeout (default 120s)'
    print_arg_usage         '--restart'         'Restart application when it is running'
    print_arg_usage         '--dry-run'         'Only print, not start'
    print_arg_usage '-h'    '--help'            'Print usage'
}

launcher::main::print_stop_usage() {
    printf "Usage:  $0 stop [OPTIONS]\n\n"
    printf "Stop an application\n\n"
    printf 'Options:\n'

    print_arg_usage '-n'    '--app-name'        'Set application name'
    print_arg_usage '-f'    '--forcekill'       "Stop application with 'kill -9'"
}

launcher::main::print_restart_usage() {
    printf "Usage:  $0 restart [OPTIONS]\n\n"
    printf "Restart an application\n\n"
    printf 'Options:\n'

    print_arg_usage '-n'    '--app-name'        'Set application name'
    print_arg_usage '-f'    '--forcekill'       "Stop application with 'kill -9'"
}

launcher::main::print_status_usage() {
    printf "Usage:  $0 status [OPTIONS]\n\n"
    printf "Check application status\n\n"
    printf 'Options:\n'

    print_arg_usage '-n'    '--app-name'        'Set application name'
    print_arg_usage '-ch'   '--check-healthy'   'Exit with error code 1 when check healthy failed'
    print_arg_usage '-cr'   '--check-ready'     'Exit with error code 1 when check ready failed'
}

