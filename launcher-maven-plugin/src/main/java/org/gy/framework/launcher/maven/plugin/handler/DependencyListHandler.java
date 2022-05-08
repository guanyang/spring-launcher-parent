package org.gy.framework.launcher.maven.plugin.handler;

import org.gy.framework.launcher.maven.plugin.util.DependencyAnalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class DependencyListHandler extends AbstractHandler {

    private DependencyAnalyzer dependencyAnalyzer;

    public DependencyListHandler(MavenProject project,
            DependencyAnalyzer dependencyAnalyzer, Log log) {
        super(project, log);
        this.dependencyAnalyzer = dependencyAnalyzer;
    }

    @Override
    public void execute() throws IOException {
        generateDependencyListToConfDir();
        generateClassPathFilesToConfDir();
    }


    private void generateDependencyListToConfDir() throws IOException {
        String dependencyListFilePath = launcherTargetBaseDir + "/conf/dependency-list.txt";
        String lineSeparator = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder();
        dependencyAnalyzer.getAllArtifactsWithout(this.project.getArtifact()).stream()
                .map(Artifact::getId)
                .forEach(artifactStr -> sb.append(artifactStr).append(lineSeparator));

        try {
            FileUtils.writeStringToFile(new File(dependencyListFilePath), sb.toString(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new IOException("Generate 'dependency-list.txt' failed", e);
        }
    }

    private void generateClassPathFilesToConfDir() throws IOException {
        String outputFileContent = launcherTargetBaseDir + "/conf/classpath-files.txt";
        String lineSeparator = System.getProperty("line.separator");
        StringBuilder sb = new StringBuilder(this.project.getName())
                .append("-")
                .append(this.project.getVersion())
                .append(".jar")
                .append(lineSeparator);
        dependencyAnalyzer.getAllArtifactsWithout(this.project.getArtifact()).stream()
                .forEach(art -> sb.append(art.getArtifactId())
                        .append("-")
                        .append(art.getVersion())
                        .append(null == art.getClassifier() ? "" : "-"+art.getClassifier())
                        .append(".jar")
                        .append(lineSeparator));

        try {
            FileUtils.writeStringToFile(new File(outputFileContent), sb.toString(), Charset.defaultCharset());
        } catch (IOException e) {
            throw new IOException("Generate 'classpath-files.txt' failed", e);
        }
    }

}
