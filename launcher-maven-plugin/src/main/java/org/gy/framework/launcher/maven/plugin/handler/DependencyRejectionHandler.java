package org.gy.framework.launcher.maven.plugin.handler;

import org.gy.framework.launcher.config.LauncherConfig;
import org.gy.framework.launcher.config.LauncherMsg;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;


public class DependencyRejectionHandler extends AbstractHandler {


    public DependencyRejectionHandler(MavenProject project, Log log) {
        super(project, log);
        this.project = project;
        this.log = log;
    }

    @Override
    public void execute() throws IOException {
        Set<String> dependencyNames = getProjectDependenciesAndPlugins()
                .stream()
                .map(dependency -> dependency.getGroupId() + ":" + dependency.getArtifactId())
                .collect(Collectors.toSet());

        List<RejectionRule> rejectionRules = LauncherConfig.getInstance()
                .getConfigFromYamlToList("dependency-rejection/rule.yml", RejectionRule.class);

        for (RejectionRule rejectionRule : rejectionRules) {
            if (dependencyNames.containsAll(rejectionRule.getDependencies())) {
                throw new IOException(
                        LauncherMsg.getBuildErrMsg(
                                "ERROR-10012",
                                String.join(",", rejectionRule.getDependencies())
                        )
                );
            }
        }
    }

    private List<Dependency> getProjectDependenciesAndPlugins() {
        List<Dependency> dependencies = new ArrayList<>(project.getCompileDependencies());
        project.getBuild().getPlugins().forEach(plugin -> {
            Dependency dependency = new Dependency();
            dependency.setGroupId(plugin.getGroupId());
            dependency.setArtifactId(plugin.getArtifactId());
            dependency.setVersion(plugin.getVersion());
            dependencies.add(dependency);
        });
        return dependencies;
    }


    @Getter
    @Setter
    @ToString
    private static class RejectionRule {

        private List<String> dependencies;
        private String message;
    }
}
