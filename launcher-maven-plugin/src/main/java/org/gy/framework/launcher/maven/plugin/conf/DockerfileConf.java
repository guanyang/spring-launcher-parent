package org.gy.framework.launcher.maven.plugin.conf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DockerfileConf {

    private String fromImage = System.getProperty("launcher.build.docker.fromImage",
        "guanyangsunlight/openjdk:8u332-jdk-oraclelinux7");

    private String packageModulePath = ".";

    private String instructionAfterFrom = "";

    private String instructionBeforeCmd = "";
}
