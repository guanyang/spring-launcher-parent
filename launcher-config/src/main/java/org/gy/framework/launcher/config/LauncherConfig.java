package org.gy.framework.launcher.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

public class LauncherConfig {

    private static final ResourceLoader resourceLoader = initResourceLoader();

    private LauncherSettings launcherSettings;

    private static LauncherConfig ourInstance = new LauncherConfig();

    public static LauncherConfig getInstance() {
        return ourInstance;
    }

    private LauncherConfig() {
        try {
            launcherSettings = initLauncherConfig();
        } catch (Exception e) {
            Logger.get().error("Launcher config init failed. " + e.getMessage(), e);
            throw new RuntimeException("Launcher config init failed.");
        }
    }

    private static ResourceLoader initResourceLoader() {
        String configProfile = LauncherMetaConfig.getLauncherConfigProfile();
        Logger.get().info(String.format("Launcher Config Profile: %s",
            StringUtils.isNotEmpty(configProfile) ? configProfile : "default"));
        if (StringUtils.isNotEmpty(configProfile)) {
            return new ResourceLoader("/launcher-config-" + configProfile, "/launcher-config");
        }
        return new ResourceLoader("/launcher-config");
    }

    private LauncherSettings initLauncherConfig() throws IOException {
        return resourceLoader.getResourceFromYamlToObject("launcher-settings.yml", LauncherSettings.class);
    }

    public String getConfigFileText(String path) throws IOException {
        return resourceLoader.getResourceAsString(path);
    }

    public <T> List<T> getConfigFromYamlToList(String path, Class<T> clazz) throws IOException {
        return resourceLoader.getResourceFromYamlToList(path, clazz);
    }

    public String getReleaseInfo() {
        return LauncherMetaConfig.getLauncherConfigReleaseInfo();
    }

    public boolean ignoreClassConflict() {
        return launcherSettings.getIgnoreClassConflict();
    }

    public Integer getDependencyWarnRestrictDelaySeconds() {
        return launcherSettings.getDependencyWarnRestrictDelaySeconds();
    }


    public List<String> getHandlerExcludes() {
        List<String> handlerExcludes = launcherSettings.getHandlerExcludes();
        if (null == handlerExcludes) {
            return new ArrayList<>();
        }
        return handlerExcludes;
    }

    public LauncherSettings getLauncherSettings() {
        return launcherSettings;
    }

    @Getter
    @Setter
    @ToString
    public static class LauncherSettings {

        private Integer dependencyWarnRestrictDelaySeconds = 15;
        private Boolean ignoreClassConflict = false;
        private List<String> handlerExcludes = new ArrayList<>();
        private Boolean blockOnInvalidCharset = false;
    }

    @Getter
    @Setter
    @ToString
    public static class ConflictIgnore {

        private String classOrPackage;
        private Boolean ignoreWithSameVersion = false;
        private List<String> dependency;
    }


}


