package org.gy.framework.launcher.maven.plugin.handler;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public abstract class AbstractHandler {

    protected MavenProject project;
    protected Log log;

    protected final String launcherTargetBaseDir;
    protected final String launcherTargetBinDir;
    protected final String launcherTargetConfDir;
    protected final String launcherTargetBuildDir;

    public AbstractHandler(MavenProject project, Log log) {
        this.project = project;
        this.log = log;
        this.launcherTargetBaseDir = this.project.getBasedir().getPath() + "/target/launcher";
        this.launcherTargetBinDir = this.launcherTargetBaseDir + "/bin";
        this.launcherTargetConfDir = this.launcherTargetBaseDir + "/conf";
        this.launcherTargetBuildDir = this.launcherTargetBaseDir + "/build";

    }

    /**
     * 处理每个流程的入口
     */
    public abstract void execute() throws IOException, URISyntaxException;
}
