package org.gy.framework.launcher.maven.plugin.handler;

import org.gy.framework.launcher.config.LauncherConfig;
import org.gy.framework.launcher.config.LauncherConfig.ConflictIgnore;
import org.gy.framework.launcher.maven.plugin.util.DependencyAnalyzer;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class JarConflictHandler extends AbstractHandler {

    private DependencyAnalyzer dependencyAnalyzer;

    public JarConflictHandler(MavenProject project,
            DependencyAnalyzer dependencyAnalyzer, Log log) {
        super(project, log);
        this.dependencyAnalyzer = dependencyAnalyzer;
    }

    @Override
    public void execute() throws IOException {
        findOutConflictClassesWithArtifacts();
    }

    private void findOutConflictClassesWithArtifacts() throws IOException {
        List<ConflictIgnore> conflictIgnoreList = LauncherConfig.getInstance().getConfigFromYamlToList("dependency-conflict/conflict-ignore.yml",ConflictIgnore.class);

        Map<String, Collection<Artifact>> duplicatedClassesMap = dependencyAnalyzer.getDuplicatedClassesMap();

        AtomicInteger conflictCount = new AtomicInteger();
        duplicatedClassesMap.entrySet().stream()
                .filter(entry -> !inConflictIgnoreList(entry.getKey(), entry.getValue(), conflictIgnoreList))
                .forEach(entry -> {
                    conflictCount.getAndIncrement();
                    log.warn("Class conflict detected! Class name: " + entry.getKey() + ". Conflict dependency: "
                                    + entry.getValue().stream()
                                    .map(Artifact::getId)
                                    .collect(Collectors.toList()));

                });

        if (!LauncherConfig.getInstance().ignoreClassConflict()){
            throw new IOException("Class conflict detected, build exit.");
        }
    }

    public boolean inConflictIgnoreList(String className, Collection<Artifact> artifacts,
        List<ConflictIgnore> conflictIgnoreList) {
        List<ConflictIgnore> classIgnoreList = conflictIgnoreList.stream()
                .filter(conflictIgnore -> className.startsWith(conflictIgnore.getClassOrPackage()))
                .collect(Collectors.toList());

        Map<String, String> artifactVersionMap = artifacts.stream().collect(
                Collectors.toMap(artifact -> artifact.getGroupId() + ":" + artifact.getArtifactId(),
                        Artifact::getVersion));

        for (ConflictIgnore conflictIgnore : classIgnoreList) {

            /*
             * 1. IgnoreWithSameVersion选项开启，
             * 2. 所有artifact都在ignoreList中
             * 3. 所有artifact都是同一个版本
             * */
            if (conflictIgnore.getIgnoreWithSameVersion()
                    && conflictIgnore.getDependency().containsAll(artifactVersionMap.keySet())
                    && new HashSet<>(artifactVersionMap.values()).size() == 1) {
                return true;
            }

            /*
             * 所有artifact都在ignoreList中
             * */
            if (conflictIgnore.getDependency()
                    .containsAll(artifacts.stream().map(e -> e.getGroupId() + ":" + e.getArtifactId()).collect(Collectors.toList()))) {
                return true;
            }
        }
        return false;
    }

}
