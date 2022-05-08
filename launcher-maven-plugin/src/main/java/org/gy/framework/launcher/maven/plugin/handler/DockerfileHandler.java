package org.gy.framework.launcher.maven.plugin.handler;

import org.gy.framework.launcher.maven.plugin.conf.DockerfileConf;
import org.gy.framework.launcher.maven.plugin.util.ResourceUtil;
import org.gy.framework.launcher.maven.plugin.util.VelocityHelper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class DockerfileHandler extends AbstractHandler {

    private DockerfileConf dockerfileConf;

    private String baseDirectory;

    public DockerfileHandler(MavenProject project, Log log,
            DockerfileConf dockerfileConf, String baseDirectory) {
        super(project, log);
        this.project = project;
        this.log = log;
        this.dockerfileConf = dockerfileConf;
        this.baseDirectory = baseDirectory;
    }


    @Override
    public void execute() throws IOException {

        String templateStr = ResourceUtil.loadAsString("conf/Dockerfile");

        String result = VelocityHelper.render(templateStr, initVelocityContext());

        FileUtils.writeStringToFile(new File(project.getBasedir().getPath() + "/target/Dockerfile"), result,
                Charset.defaultCharset());

        //生成一份包含模块名的dockerfile，方便发布系统查找
        String dockerfileName = project.getArtifactId() + "-launcher.dockerfile";
        FileUtils.writeStringToFile(new File(project.getBasedir().getPath() + "/target/" + dockerfileName), result,
                Charset.defaultCharset());
    }


    private Map initVelocityContext() {
        Map<String, String> pMap = new HashMap<>(10);
        pMap.put("fromImage", dockerfileConf.getFromImage());
        pMap.put("packageModulePath", dockerfileConf.getPackageModulePath());
        pMap.put("appBaseDirectory", baseDirectory);
        pMap.put("instructionAfterFrom", trimEveryLine(dockerfileConf.getInstructionAfterFrom()));
        pMap.put("instructionBeforeCmd", trimEveryLine(dockerfileConf.getInstructionBeforeCmd()));

        return pMap;
    }


    private static String trimEveryLine(String text) {
        return Arrays.stream(StringUtils.split(text, System.lineSeparator()))
                .map(String::trim)
                .collect(Collectors.joining(System.lineSeparator()));
    }

}
