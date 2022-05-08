#!/usr/bin/env bash
#
# 初始化

# 初始化环境信息（与应用无关的信息）
ENV_INITED=0
launcher::init_env() {

    if [[ ${ENV_INITED} == 1 ]]; then
        return
    fi

    launcher::init_env::pod_name

    launcher::init_env::is_in_container

    launcher::init_env::is_in_k8s

    launcher::init_env::java_home

    ENV_INITED=1
}

########################################################
# 初始化POD_NAME
# Globals:
#   none
# Env:
#   POD_NAME
# Arguments:
#   none
# Returns:
#   Set Global: POD_NAME
########################################################
launcher::init_env::pod_name(){
    local pod_name=$(printenv POD_NAME)
    if  [[ -n ${pod_name} ]];then
        readonly POD_NAME=${POD_NAME}
    fi
}

########################################################
# 初始化IS_IN_CONTAINER（是否容器环境）
# Globals:
#   none
# Env:
#   IS_IN_CONTAINER
# Arguments:
#   none
# Returns:
#   Set Global: IS_IN_CONTAINER(true/false)
########################################################
launcher::init_env::is_in_container(){
    local is_in_container=false
    if  [[ $(printenv IS_IN_CONTAINER) == true ]];then
        is_in_container=true
    fi
    readonly IS_IN_CONTAINER=${is_in_container}
}

########################################################
# 初始化IS_IN_K8S（是否K8S环境）
# Globals:
#   IS_IN_CONTAINER
#   POD_NAME
# Arguments:
#   none
# Returns:
#   Set Global: IS_IN_K8S(true/false)
########################################################
launcher::init_env::is_in_k8s(){
    local is_in_k8s=false
    if [[ ${IS_IN_CONTAINER} == true && -n ${POD_NAME} ]];then
        is_in_k8s=true
    fi
    readonly IS_IN_K8S=${is_in_k8s}
}

########################################################
# 初始化IS_IN_K8S（是否K8S环境）
# Globals:
#   IS_IN_CONTAINER
#   POD_NAME
# Arguments:
#   none
# Returns:
#   Set Global: IS_IN_K8S(true/false)
########################################################
launcher::init_env::java_home() {
    if [[ ! -d "${JAVA_HOME}" ]]; then
      log_error 'JAVA_HOME not found (or not export in profiles).'
      launcher::abort
    else
      log_info "JAVA_HOME: [${JAVA_HOME}]"
      echo "$(${JAVA_HOME}/bin/java -version 2>&1)"
    fi
    readonly JAVA_HOME="${JAVA_HOME}"
}