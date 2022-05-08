package org.gy.framework.launcher.maven.plugin.mojo;

import org.gy.framework.launcher.config.LauncherConfig;
import org.gy.framework.launcher.config.LauncherConfigVariable;
import org.gy.framework.launcher.config.LauncherMsg;
import org.gy.framework.launcher.config.Logger;
import org.gy.framework.launcher.maven.plugin.conf.AppConf;
import org.gy.framework.launcher.maven.plugin.conf.DockerfileConf;
import org.gy.framework.launcher.maven.plugin.conf.LogConf;
import org.gy.framework.launcher.maven.plugin.handler.AbstractHandler;
import org.gy.framework.launcher.maven.plugin.handler.AppOptionHandler;
import org.gy.framework.launcher.maven.plugin.handler.DependencyListHandler;
import org.gy.framework.launcher.maven.plugin.handler.DependencyRejectionHandler;
import org.gy.framework.launcher.maven.plugin.handler.DependencyRestrictHandler;
import org.gy.framework.launcher.maven.plugin.handler.LauncherScriptsHandler;
import org.gy.framework.launcher.maven.plugin.handler.DockerfileHandler;
import org.gy.framework.launcher.maven.plugin.handler.FileSetHandler;
import org.gy.framework.launcher.maven.plugin.handler.JarConflictHandler;
import org.gy.framework.launcher.maven.plugin.handler.LogHandler;
import org.gy.framework.launcher.maven.plugin.util.DependencyAnalyzer;
import org.gy.framework.launcher.maven.plugin.util.ResourceUtil;
import org.gy.framework.launcher.maven.plugin.util.SpringBootMavenPluginHelper;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.model.Plugin;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.plugins.assembly.mojos.SingleAssemblyMojo;
import org.apache.maven.settings.Settings;

/**
 * 服务器启动脚本自动打包插件，自动将启动脚本打包到/bin目录中
 */
@Mojo(name = "launcher",threadSafe = true,
        defaultPhase = LifecyclePhase.PACKAGE,
        requiresDependencyCollection = ResolutionScope.COMPILE_PLUS_RUNTIME,
        requiresDependencyResolution = ResolutionScope.COMPILE_PLUS_RUNTIME)
public class LauncherMojo extends SingleAssemblyMojo {

    @Parameter(name = "dockerfile")
    private DockerfileConf dockerfile = new DockerfileConf();

    @Parameter(name = "logConf")
    private LogConf logConf = new LogConf();

    @Parameter(name = "apps")
    private List<AppConf> apps = new ArrayList<>();

    @Parameter(defaultValue = "launcher")
    private String fileName;

    @Parameter
    private String fileSet;

    @Parameter(defaultValue = "target/launcher/build/package.xml")
    private String descriptorFile;

    @Parameter(defaultValue = "${settings}", readonly = true)
    private Settings settings;

    @Parameter(defaultValue = "${project.artifactId}")
    private String baseDirectory;

    private DependencyAnalyzer dependencyAnalyzer;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        Logger.set(this.getLog());

        LauncherConfigVariable
                .setAppMainClasses(apps.stream().map(AppConf::getMainClass).collect(Collectors.toList()));

        getLog().info(ResourceUtil.getResourceProperty("launcher.properties", "launcher-maven-plugin.releaseInfo"));
        getLog().info(LauncherConfig.getInstance().getReleaseInfo());

        checkDefaultCharsetIsUnicode();

        checkProjectPackagingIsJar();

        checkSpringBootMavenPluginOrder();

        checkOriginalJarExists();

        this.dependencyAnalyzer = initDependencyAnalyzer();

        List<AbstractHandler> handlers = initHandlers();
        Collection excludedHandlers = initExcludedHandlers();

        for (AbstractHandler handler : handlers) {
            try {
                if (excludedHandlers.contains(handler.getClass().getSimpleName())) {
                    continue;
                }
                long start = System.currentTimeMillis();
                getLog().info(
                        "launcher plugin handler [" + handler.getClass().getSimpleName() + "] start");
                handler.execute();
                getLog().info(
                        "launcher plugin handler [" + handler.getClass().getSimpleName() + "] finished, cost " + (
                                System.currentTimeMillis() - start) + "ms");
            } catch (IOException | URISyntaxException e) {
                getLog().error(e.getMessage());
                throw new MojoExecutionException(e.getMessage(), e);
            }
        }

        assemble();
        movePackage();
        LauncherConfigVariable.clearAppMainClasses();
    }

    private void checkOriginalJarExists() throws MojoExecutionException {
        if (!SpringBootMavenPluginHelper.hasRepackageGoal(this.getProject())) {
            return;
        }
        if (!new File(getProject().getArtifact().getFile().getAbsolutePath() + ".original").exists()) {
            logErrorAndThrow(LauncherMsg.getBuildErrMsg("ERROR-10003",
                    getProject().getArtifact().getFile().getAbsolutePath() + ".original"));
        }
    }

    private void checkProjectPackagingIsJar() throws MojoExecutionException {
        if (!StringUtils.equals(getProject().getPackaging(), "jar")) {
            logErrorAndThrow(LauncherMsg
                    .getBuildErrMsg("ERROR-10011", getProject().getArtifactId(), getProject().getPackaging()));
        }
    }


    private Collection initExcludedHandlers() {
        List<String> handlerExcludesInlauncherSettings = LauncherConfig.getInstance()
                .getHandlerExcludes();

        List<String> handlerExcludesInJvmParam = Arrays
                .asList(System.getProperty("launcher.handler.excludes", "").split(","));

        return CollectionUtils.union(handlerExcludesInlauncherSettings, handlerExcludesInJvmParam);
    }

    private void checkDefaultCharsetIsUnicode() throws MojoExecutionException {
        String currentCharset = Charset.defaultCharset().toString().toLowerCase();
        if (!StringUtils.startsWith(currentCharset, "utf") &&
                !StringUtils.startsWith(currentCharset, "unicode")) {
            if (LauncherConfig.getInstance().getLauncherSettings().getBlockOnInvalidCharset()) {
                throw new MojoExecutionException(LauncherMsg.getBuildErrMsg("ERROR-10010", currentCharset));
            }
            this.getLog().warn(LauncherMsg.getBuildErrMsg("ERROR-10010", currentCharset));
        }
    }

    private DependencyAnalyzer initDependencyAnalyzer() {
        List<Artifact> artifactList = new ArrayList<>(getProject().getArtifacts());

        Artifact projectArtifact = getProject().getArtifact();
        DefaultArtifact originArtifact = new DefaultArtifact(projectArtifact.getGroupId(),
                projectArtifact.getArtifactId(), projectArtifact.getVersion(),
                "compile", "jar", projectArtifact.getClassifier(),
                projectArtifact.getArtifactHandler());
        if (SpringBootMavenPluginHelper.hasRepackageGoal(this.getProject())) {
            originArtifact.setFile(new File(projectArtifact.getFile().getAbsolutePath() + ".original"));
        } else {
            originArtifact.setFile(new File(projectArtifact.getFile().getAbsolutePath()));
        }

        artifactList.add(0, originArtifact);
        return new DependencyAnalyzer(artifactList, getLog());
    }

    private void checkSpringBootMavenPluginOrder() throws MojoExecutionException {
        List<Plugin> plugins = getProject().getBuild().getPlugins();

        int indexOfSpringBootMavenPlugin = -1;
        int indexOflauncherMavenPlugin = -1;
        for (int i = 0; i < plugins.size(); i++) {
            if (StringUtils.equals(plugins.get(i).getArtifactId(), "spring-boot-maven-plugin")) {
                indexOfSpringBootMavenPlugin = i;
            } else if (StringUtils.equals(plugins.get(i).getArtifactId(), "launcher-maven-plugin")) {
                indexOflauncherMavenPlugin = i;
            }
        }

        if (indexOfSpringBootMavenPlugin > indexOflauncherMavenPlugin) {
            logErrorAndThrow(LauncherMsg.getBuildErrMsg("ERROR-10006"));
        }
    }

    private void logErrorAndThrow(String message) throws MojoExecutionException {
        getLog().error(message);
        throw new MojoExecutionException(message);
    }

    private void assemble() throws MojoFailureException, MojoExecutionException {
        setFinalName(getProject().getArtifactId() + "-" + getProject().getVersion() + ".launcher");
        setAppendAssemblyId(false);
        setDescriptors(new String[]{descriptorFile});
        super.execute();
    }


    private void movePackage() throws MojoExecutionException {
        try {
            String releaseDirStr = System.getProperty("launcher.build.releaseDir");
            if (StringUtils.isEmpty(releaseDirStr)) {
                return;
            }
            File releaseDir = new File(releaseDirStr);
            File targetDir = new File(getProject().getModel().getBuild().getDirectory());
            File launcherPackage = new File(targetDir,
                    getProject().getArtifactId() + "-" + getProject().getVersion() + ".launcher.tar.gz");
            FileUtils.moveToDirectory(launcherPackage, releaseDir, true);
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }
    }


    private List<AbstractHandler> initHandlers() {
        List<AbstractHandler> handlers = new ArrayList<>();
        handlers.add(new JarConflictHandler(getProject(), this.dependencyAnalyzer, getLog()));
        handlers.add(new DependencyListHandler(getProject(), this.dependencyAnalyzer, getLog()));
        handlers.add(new LauncherScriptsHandler(getProject(), getLog(), this.fileName));
        handlers.add(new LogHandler(getProject(), getLog(), logConf, dependencyAnalyzer));
        handlers.add(new DockerfileHandler(getProject(), getLog(), dockerfile, baseDirectory));
        handlers.add(new FileSetHandler(getProject(), getLog(), fileSet, baseDirectory));
        handlers.add(new DependencyRestrictHandler(getProject(), getLog()));
        handlers.add(new DependencyRejectionHandler(getProject(), getLog()));
        handlers.add(new AppOptionHandler(getProject(), getLog(), apps, dependencyAnalyzer));
        return handlers;
    }
}
