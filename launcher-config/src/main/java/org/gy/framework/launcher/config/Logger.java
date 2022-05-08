package org.gy.framework.launcher.config;

import org.apache.maven.monitor.logging.DefaultLog;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.console.ConsoleLogger;

public class Logger {

    private static final Log consoleLog = new DefaultLog(new ConsoleLogger());

    private Logger() {

    }

    private static Log log;

    public static void set(Log log) {
        log = log;
    }

    public static Log get() {
        return null != log ? log : consoleLog;
    }
}
