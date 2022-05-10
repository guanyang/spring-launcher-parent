package org.gy.framework.launcher.maven.plugin.conf;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DockerfileConf {

    private String fromImage=System.getProperty("launcher.build.docker.fromImage","hub.docker.com/repository/docker/guanyangsunlight/java-centos:1.8_201_b09");

    private String packageModulePath=".";

    private String instructionAfterFrom="";

    private String instructionBeforeCmd="";
}
