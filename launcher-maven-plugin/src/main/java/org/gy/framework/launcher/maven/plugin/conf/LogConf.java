package org.gy.framework.launcher.maven.plugin.conf;

import lombok.Data;
import lombok.experimental.Accessors;

public class LogConf {

    private String logName = "org.gy.framework";

    private String logLevel = "INFO";

    private String rootLogLevel = "INFO";

    private String maxFileSize = "500MB";

    private String maxHistory = "15";

    private String totalSizeCap = "50GB";

    public String getLogName() {
        return logName;
    }

    public void setLogName(String logName) {
        this.logName = logName;
    }

    public String getLogLevel() {
        return logLevel;
    }

    public void setLogLevel(String logLevel) {
        this.logLevel = logLevel;
    }

    public String getRootLogLevel() {
        return rootLogLevel;
    }

    public void setRootLogLevel(String rootLogLevel) {
        this.rootLogLevel = rootLogLevel;
    }

    public String getMaxFileSize() {
        return maxFileSize;
    }

    public void setMaxFileSize(String maxFileSize) {
        this.maxFileSize = maxFileSize;
    }

    public String getMaxHistory() {
        return maxHistory;
    }

    public void setMaxHistory(String maxHistory) {
        this.maxHistory = maxHistory;
    }

    public String getTotalSizeCap() {
        return totalSizeCap;
    }

    public void setTotalSizeCap(String totalSizeCap) {
        this.totalSizeCap = totalSizeCap;
    }
}
