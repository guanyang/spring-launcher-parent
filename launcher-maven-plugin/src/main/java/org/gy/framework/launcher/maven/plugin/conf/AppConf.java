package org.gy.framework.launcher.maven.plugin.conf;

public class AppConf {

    public AppConf() {

    }

    public AppConf(String name, String mainClass) {
        this.name = name;
        this.mainClass = mainClass;
    }

    private String name;

    private String mainClass;

    private String startArgs;

    private JvmOptionConf jvmOption = new JvmOptionConf();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public JvmOptionConf getJvmOption() {
        return jvmOption;
    }

    public void setJvmOption(JvmOptionConf jvmOption) {
        this.jvmOption = jvmOption;
    }

    public String getStartArgs() {
        return startArgs;
    }

    public void setStartArgs(String startArgs) {
        this.startArgs = startArgs;
    }
}
