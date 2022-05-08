package org.gy.framework.launcher.maven.plugin.handler;

import org.gy.framework.launcher.maven.plugin.util.ResourceUtil;
import org.gy.framework.launcher.maven.plugin.util.SpringBootMavenPluginHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class FileSetHandler extends AbstractConfHandler<String> {


    private static final String SPRING_BOOT_PACKAGED_JAR_CONFIG_IN_PACKAGE_XML = "<fileSet>\n"
            + "            <directory>./target</directory>\n"
            + "            <outputDirectory>lib</outputDirectory>\n"
            + "            <includes>\n"
            + "                <include>${project.build.finalName}.jar</include>\n"
            + "            </includes>\n"
            + "        </fileSet>";

    private static final String DEPENDENCY_SETS_CONFIG_IN_PACKAGE_XML = "<dependencySets>\n"
            + "        <dependencySet>\n"
            + "            <outputDirectory>lib</outputDirectory>\n"
            + "            <scope>runtime</scope>\n"
            + "        </dependencySet>\n"
            + "    </dependencySets>";

    private final String baseDirectory;
    private final boolean isSpringBootRepackaged;

    public FileSetHandler(MavenProject project, Log log,
            String conf, String baseDirectory) {
        super(project, log, conf);
        this.baseDirectory = baseDirectory;
        isSpringBootRepackaged = SpringBootMavenPluginHelper.hasRepackageGoal(this.project);
    }

    @Override
    public void execute() throws IOException {
        String packageXmlContent = ResourceUtil.loadAsString("conf/package.xml");
        packageXmlContent = packageXmlContent
                .replace("___baseDirectory___", baseDirectory)
                .replace("___customFileSet___", loadFile(conf))
                .replace("___addSpringBootPackagedJar___",initPlaceHolderAddSpringBootPackagedJar())
                .replace("___dependenciesWithoutPackaged___",initPlaceHolderDependenciesWithoutPackaged());

        File descriptorFile = new File(launcherTargetBuildDir + "/package.xml");
        FileUtils.writeStringToFile(descriptorFile, packageXmlContent, Charset.defaultCharset());
    }

    private String initPlaceHolderAddSpringBootPackagedJar() {
        if (isSpringBootRepackaged) {
            return SPRING_BOOT_PACKAGED_JAR_CONFIG_IN_PACKAGE_XML;
        }
        return "";
    }
    private String initPlaceHolderDependenciesWithoutPackaged() {
        if (isSpringBootRepackaged) {
            return "";
        }
        return DEPENDENCY_SETS_CONFIG_IN_PACKAGE_XML;
    }


    private String loadFile(String path) throws IOException {
        if (StringUtils.isEmpty(conf)) {
            return "";
        }

        File file = new File(path);
        if (!file.exists()) {
            file = new File(project.getBasedir().getPath() + File.separator + path);
        }

        if (file.exists()) {
            this.log.info("Custom fileset file '" + file.getAbsolutePath() + "' found ");
            try {
                return FileUtils.readFileToString(file, Charset.defaultCharset());
            } catch (IOException e) {
                throw new IOException("FileSet file '" + path + "' load error", e);
            }
        }

        throw new IOException("FileSet file '" + path + "' load error, File not found");
    }



}
