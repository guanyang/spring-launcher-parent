package org.gy.framework.launcher.config;

import java.util.ArrayList;
import java.util.List;

public class LauncherConfigVariable {

    private static ThreadLocal<List> appMainClasses = new ThreadLocal<>();

    private LauncherConfigVariable(){

    }

    public static void setAppMainClasses(List<String> list){
        if(null == list || list.isEmpty()){
            Logger.get().error(LauncherMsg.getBuildErrMsg("ERROR-10004"));
            throw new NullPointerException(LauncherMsg.getBuildErrMsg("ERROR-10004"));
        }
        if(appMainClasses.get()!=null){
            throw new UnsupportedOperationException("Method 'setAppMainClasses' can be invoked only once");
        }
        appMainClasses.set(list);
    }

    static List<String> getAppMainClasses(){
        return new ArrayList<>(appMainClasses.get());
    }

    public static void clearAppMainClasses() {
        appMainClasses.remove();
    }
}
