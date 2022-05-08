package org.gy.framework.launcher.maven.plugin.handler;

import org.gy.framework.launcher.config.LauncherConfig;
import org.gy.framework.launcher.maven.plugin.util.ResourceUtil;
import org.gy.framework.launcher.maven.plugin.util.SpringBootMavenPluginHelper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.Collection;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class LauncherScriptsHandler extends AbstractHandler {

    private static final String LAUNCHER_SCRIPTS_BASE_PATH = "scripts";
    private static final String LAUNCHER = "launcher.sh";
    private final String TARGET_LAUNCHER_BIN;

    private String scriptFileName;

    public LauncherScriptsHandler(MavenProject project, Log log, String scriptFileName) {
        super(project, log);
        this.scriptFileName = scriptFileName;
        this.TARGET_LAUNCHER_BIN = launcherTargetBaseDir + "/bin/";
    }

    @Override
    public void execute() throws IOException, URISyntaxException {

        Collection<String> resources = ResourceUtil.listResources(LAUNCHER_SCRIPTS_BASE_PATH);

        for (String resourcePath : resources) {
            String resourceContent = getResourceContent(resourcePath);

            resourceContent = StringUtils.replace(
                    resourceContent,
                    "___LAUNCHER_CONFIG_INFO___",
                    LauncherConfig.getInstance().getReleaseInfo());

            resourceContent = StringUtils.replace(
                    resourceContent,
                    "___LAUNCHER_IS_SPRINGBOOT_REPACKAGED___",
                    SpringBootMavenPluginHelper.hasRepackageGoal(this.project) ? "true" : "false");

            resourceContent = StringUtils.replace(
                    resourceContent,
                    "___LAUNCHER_PROJECT_BUILD_DATE___",
                    DateFormatUtils.format(System.currentTimeMillis(),"yyyy-MM-dd'T'HH:mm:ss"));

            resourceContent = StringUtils.replace(
                    resourceContent,
                    "___LAUNCHER_PROJECT_ARTIFACT_ID___",
                    this.project.getArtifactId());

            String resourceRelativePath = getFileRelatePath(resourcePath, LAUNCHER_SCRIPTS_BASE_PATH);
            File targetFile;
            if (StringUtils.equals(resourceRelativePath, LAUNCHER)) {
                targetFile = new File(TARGET_LAUNCHER_BIN + scriptFileName + ".sh");
            } else {
                targetFile = new File(TARGET_LAUNCHER_BIN + resourceRelativePath);
            }
            FileUtils.writeStringToFile(targetFile, resourceContent, Charset.defaultCharset());
        }
    }

    private String getResourceContent(String resourcePath) throws IOException {
        File resource = new File(resourcePath);
        if (resource.exists()) {
            return FileUtils.readFileToString(resource, Charset.defaultCharset());
        } else {
            log.debug("Load Resource '" + resourcePath + "'");
            return ResourceUtil.loadAsString(resourcePath);
        }
    }

    private String getFileRelatePath(String resourcePath, String relativeBasePath) {
        return resourcePath.substring(resourcePath.lastIndexOf(relativeBasePath) + relativeBasePath.length() + 1);
    }
}
