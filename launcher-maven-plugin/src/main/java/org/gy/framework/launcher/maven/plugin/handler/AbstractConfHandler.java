package org.gy.framework.launcher.maven.plugin.handler;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public abstract class AbstractConfHandler<T> extends AbstractHandler {

    protected T conf;

    public AbstractConfHandler(MavenProject project, Log log, T conf) {
        super(project, log);
        this.conf = conf;
    }

}
