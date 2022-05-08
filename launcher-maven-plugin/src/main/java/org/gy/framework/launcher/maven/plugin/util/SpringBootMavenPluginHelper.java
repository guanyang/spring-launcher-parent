package org.gy.framework.launcher.maven.plugin.util;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;

public class SpringBootMavenPluginHelper {

    private SpringBootMavenPluginHelper() {

    }

    public static boolean hasRepackageGoal(MavenProject project) {
        Plugin springBootMavenPlugin = getSpringBootMavenPlugin(project);
        if (null == springBootMavenPlugin) {
            return false;
        }

        for(PluginExecution execution : springBootMavenPlugin.getExecutions()){
            for (String goal : execution.getGoals()){
                if(StringUtils.equals(goal,"repackage")){
                    return true;
                }
            }
        }
        return true;
    }

    private static Plugin getSpringBootMavenPlugin(MavenProject project) {
        List<Plugin> plugins = project.getBuild().getPlugins();
        for (Plugin plugin : plugins) {
            if (StringUtils.equals(plugin.getArtifactId(), "spring-boot-maven-plugin")) {
                return plugin;
            }
        }
        return null;
    }

}
