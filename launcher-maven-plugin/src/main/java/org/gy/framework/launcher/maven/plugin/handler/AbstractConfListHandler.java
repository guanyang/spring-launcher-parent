package org.gy.framework.launcher.maven.plugin.handler;

import java.util.List;

import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public abstract class AbstractConfListHandler<T> extends AbstractHandler {

    protected List<T> confList;

    public AbstractConfListHandler(MavenProject project, Log log, List<T> confList) {
        super(project, log);
        this.confList = confList;
    }

}
