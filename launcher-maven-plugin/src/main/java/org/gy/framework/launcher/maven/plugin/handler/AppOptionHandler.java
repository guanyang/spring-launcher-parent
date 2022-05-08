package org.gy.framework.launcher.maven.plugin.handler;

import org.gy.framework.launcher.config.LauncherConfig;
import org.gy.framework.launcher.maven.plugin.conf.AppConf;
import org.gy.framework.launcher.maven.plugin.util.DependencyAnalyzer;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class AppOptionHandler extends AbstractConfListHandler<AppConf> {


    private DependencyAnalyzer dependencyAnalyzer;

    public AppOptionHandler(MavenProject project, Log log,
            List<AppConf> confList, DependencyAnalyzer dependencyAnalyzer) {
        super(project, log, confList);
        this.dependencyAnalyzer = dependencyAnalyzer;
    }

    @Override
    public void execute() throws IOException {
        FileUtils.writeStringToFile(new File(launcherTargetConfDir + "/jvm.options"),
                LauncherConfig.getInstance().getConfigFileText("conf/jvm.options"), Charset.defaultCharset());

        checkAppNameConflict(confList);
        for (AppConf appConf : confList) {
            if (! dependencyAnalyzer.hasClass(appConf.getMainClass())) {
                throw new IOException("Application [" + appConf.getName() + "] main class [" + appConf.getMainClass()
                        + "] not found");
            }
            generateAppOptionFile(appConf);
        }
    }

    private void checkAppNameConflict(List<AppConf> confList) {
        Set<String> appNameSet = new HashSet<>();
        confList.stream().forEach(appConf -> {
            String appName = appConf.getName();
            if (appNameSet.contains(appName)) {
                throw new RuntimeException("AppName '" + appName
                        + "' has been already exists, please check your launcher plugin configuration.");
            }
            appNameSet.add(appName);
        });

    }

    private void generateAppOptionFile(AppConf appConf) throws IOException {
        List<String> appOptions = new ArrayList<>();
        appOptions.add("app.name=" + appConf.getName());
        appOptions.add("app.mainClass=" + appConf.getMainClass());

        //TODO 2.命令行参数支持在pom.xml中配置，减少启动命令长度
        if (appConf.getStartArgs() != null) {
            System.out.println(appConf.getStartArgs());
            //  appOptions.add("app.startArgs=" + appConf.getStartArgs());
        }

        File appOptionFile = new File(launcherTargetConfDir + File.separator + appConf.getName() + ".app.options");
        FileUtils.writeLines(appOptionFile, appOptions);
    }

}
