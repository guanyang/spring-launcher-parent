package org.gy.framework.launcher.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

class LauncherMetaConfig {


    private static final ResourceLoader resourceLoader = new ResourceLoader("/");

    private static final Properties launcherMavenPluginProperties = initLauncherMavenPluginProperties();


    private LauncherMetaConfig() {

    }

    private static Properties initLauncherMavenPluginProperties() {
        return resourceLoader.getResourceAsProperties("launcher-config.properties");
    }

    static String getLauncherConfigProfile() {
        return System.getProperty("launcher.config.profile", getLauncherConfigProfileByAutoDetect());
    }

    private static String getLauncherConfigProfileByAutoDetect() {
        List<LauncherConfigProfile> resourceFromYamlToList;
        try {
            resourceFromYamlToList = resourceLoader.getResourceFromYamlToList("launcher-config-profile-mapping.yml",
                LauncherConfigProfile.class);
            List<String> appMainClasses = LauncherConfigVariable.getAppMainClasses();
            for (String mainClass : appMainClasses) {
                String launcherProfile = findLauncherProfile(mainClass, resourceFromYamlToList);
                if (StringUtils.isNoneEmpty(launcherProfile)) {
                    return launcherProfile;
                }
            }
            return null;
        } catch (IOException e) {
            Logger.get().warn("Resource not found, use default launcher config", e);
            return "";
        }
    }

    private static String findLauncherProfile(String mainClass,
        List<LauncherConfigProfile> launcherConfigProfileList) {
        for (LauncherConfigProfile launcherConfigProfile : launcherConfigProfileList) {
            /**
             * if mainClass in exclude list, continue
             */
            for (String packagePrefixExclude : launcherConfigProfile.getPackagePrefixExcludes()) {
                if (StringUtils.startsWith(mainClass, packagePrefixExclude)) {
                    continue;
                }
            }

            /**
             * if mainClass in include list, return
             */
            for (String packagePrefixInclude : launcherConfigProfile.getPackagePrefixIncludes()) {
                if (StringUtils.startsWith(mainClass, packagePrefixInclude)) {
                    return launcherConfigProfile.getProfile();
                }
            }
        }
        return null;
    }

    static String getLauncherConfigReleaseInfo() {
        return getLauncherConfigProperty("launcher-config.releaseInfo");
    }

    static String getLauncherConfigProperty(String name) {
        return launcherMavenPluginProperties.getProperty(name);
    }


    @Getter
    @Setter
    @ToString
    static class LauncherConfigProfile {

        private String profile;
        private List<String> packagePrefixIncludes = new ArrayList<>();
        private List<String> packagePrefixExcludes = new ArrayList<>();

    }
}


