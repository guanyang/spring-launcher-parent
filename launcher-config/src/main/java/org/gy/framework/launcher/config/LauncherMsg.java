package org.gy.framework.launcher.config;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.ArrayUtils;

public class LauncherMsg {

    private static ResourceLoader resourceLoader = new ResourceLoader();
    private static Map<String,Msg> msgCodeMap = initMsgCodeMap();

    private static Map<String, Msg> initMsgCodeMap() {
        try {
            return resourceLoader.getResourceFromYamlToList("launcher-message.yml",Msg.class)
                .stream().collect(Collectors.toMap(Msg::getCode,(Msg) -> Msg));
        } catch (IOException e) {
            Logger.get().error(e.getMessage(),e);
            return new HashMap<>();
        }
    }

    private LauncherMsg() {

    }

    public static String getBuildErrMsg(String key) {
        return getBuildErrMsg(key,null);
    }

    public static String getBuildErrMsg(String code, String... formatArgs) {
        Msg msg = msgCodeMap.get(code);
        if(null == msg){
            return getBuildErrMsg("ERROR-10007",code);
        }

        String buildErrMsgContent = msg.getMessage();

        if(!ArrayUtils.isEmpty(formatArgs)){
            buildErrMsgContent = String.format(buildErrMsgContent,formatArgs);
        }

        return "["+code+"] " + buildErrMsgContent;
    }

    @Getter
    @Setter
    @ToString
    public static class Msg {
        private String code;
        private String message;
    }
}
