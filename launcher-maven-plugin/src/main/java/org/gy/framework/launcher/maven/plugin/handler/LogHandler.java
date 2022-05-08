package org.gy.framework.launcher.maven.plugin.handler;

import org.gy.framework.launcher.config.LauncherConfig;
import org.gy.framework.launcher.config.LauncherMsg;
import org.gy.framework.launcher.maven.plugin.conf.LogConf;
import org.gy.framework.launcher.maven.plugin.util.DependencyAnalyzer;
import org.gy.framework.launcher.maven.plugin.util.ResourceUtil;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class LogHandler extends AbstractConfHandler<LogConf> {

    private DependencyAnalyzer dependencyAnalyzer;

    public LogHandler(MavenProject project, Log log, LogConf conf,
            DependencyAnalyzer dependencyAnalyzer) {
        super(project, log, conf);
        this.dependencyAnalyzer = dependencyAnalyzer;
    }

    @Override
    public void execute() throws IOException {

        boolean hasLogbackDependency = false;
        for (Dependency dependency : (List<Dependency>) this.project.getCompileDependencies()) {
            if (StringUtils.equals(dependency.getGroupId(), "ch.qos.logback") && StringUtils
                    .equals(dependency.getArtifactId(), "logback-classic")) {
                hasLogbackDependency = true;
            } else if (StringUtils.equals(dependency.getGroupId(), "org.apache.logging.log4j") && StringUtils
                    .equals(dependency.getArtifactId(), "log4j-slf4j-impl")) {
                throw new IOException(LauncherMsg.getBuildErrMsg("ERROR-10001"));
            }

        }
        if (hasLogbackDependency) {
            generateLogbackConfigurationFile();
        } else {
            throw new IOException(LauncherMsg.getBuildErrMsg("ERROR-10002"));
        }

    }

    private void generateLogbackConfigurationFile() throws IOException {
        String templateStr = LauncherConfig.getInstance().getConfigFileText("conf/logback.xml");

        templateStr = replaceVariables(templateStr);

        ResourceUtil.saveToFile(templateStr, launcherTargetConfDir + "/logback.xml");
    }


    private String replaceVariables(String templateStr) {
        templateStr = templateStr.replaceAll("___logName___", this.conf.getLogName());
        templateStr = templateStr.replaceAll("___logLevel___", this.conf.getLogLevel());
        templateStr = templateStr.replaceAll("___rootLogLevel___", this.conf.getRootLogLevel());
        templateStr = templateStr.replaceAll("___maxFileSize___", this.conf.getMaxFileSize());
        templateStr = templateStr.replaceAll("___maxHistory___", this.conf.getMaxHistory());
        templateStr = templateStr.replaceAll("___totalSizeCap___", this.conf.getTotalSizeCap());
        templateStr = StringUtils
                .replace(templateStr, "___snippetInclude___", generateLogbackSnippetXmlFromDependency());
        return templateStr;
    }

    private String generateLogbackSnippetXmlFromDependency() {
        StringBuilder includeElements = new StringBuilder();
        dependencyAnalyzer.getArtifactLogbacksnippettextMap()
                .entrySet().stream()
                .forEach(entry -> {
                    try {
                        String artifactId = entry.getKey().getArtifactId();
                        generateLogbackSnippetXmlToConf(artifactId, entry.getValue());
                        includeElements.append("<include optional=\"true\" file=\"${launcher.app.home}/conf/logback.snippet.")
                                .append(artifactId)
                                .append(".xml\"/>")
                                .append(System.getProperty("line.separator"))
                                .append("    ");
                    } catch (IOException e) {
                        log.error(e.getMessage(), e);
                    }
                });
        return includeElements.toString();
    }

    private void generateLogbackSnippetXmlToConf(String artifactId, String content) throws IOException {
        FileUtils.writeStringToFile(
                new File(this.launcherTargetConfDir + File.separator + "logback.snippet." + artifactId + ".xml"),
                content,
                Charset.defaultCharset());
    }


}
