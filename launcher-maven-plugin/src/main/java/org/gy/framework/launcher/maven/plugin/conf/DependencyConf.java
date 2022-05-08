package org.gy.framework.launcher.maven.plugin.conf;

import java.io.File;

import org.apache.maven.model.Dependency;

public class DependencyConf {

    private Dependency dependency;
    private File jarFile;

    public DependencyConf(Dependency dependency, File jarFile) {
        this.dependency = dependency;
        this.jarFile = jarFile;
    }

    public Dependency getDependency() {
        return dependency;
    }

    public File getJarFile() {
        return jarFile;
    }
}
