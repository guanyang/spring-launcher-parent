package org.gy.framework.launcher.maven.plugin.util;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.logging.Log;

public class DependencyAnalyzer {

    private Log log;
    private List<Artifact> artifacts;
    private Map<String, Collection<Artifact>> classnameArtifactMap = new LinkedHashMap<>(4096);
    private List<Artifact> allArtifacts = new ArrayList<>();
    private Map<Artifact, String> artifactLogbacksnippettextMap = new LinkedHashMap<>();

    public DependencyAnalyzer(List<Artifact> artifacts, Log log) {
        this.artifacts = artifacts;
        this.log = log;
        init();
    }

    private void init() {
        this.artifacts.stream()
                .filter(artifact -> "compile".equals(artifact.getScope()) || "runtime".equals(artifact.getScope()))
                .filter(artifact -> "jar".equals(artifact.getType()))
                .forEach(artifact -> {
                    allArtifacts.add(artifact);
                    extractAllClassNames(artifact).forEach(
                            classname -> classnameArtifactMap.computeIfAbsent(classname, k -> new LinkedHashSet<>())
                                    .add(artifact));
                });
    }


    private List<String> extractAllClassNames(Artifact artifact) {
        List<String> result = new ArrayList<>();
        try (JarFile jar = new JarFile(artifact.getFile())) {
            Enumeration<JarEntry> enumFiles = jar.entries();
            while (enumFiles.hasMoreElements()) {
                JarEntry entry = enumFiles.nextElement();

                result.addAll(extractClassname(artifact, entry));

                extractLogbackSnippet(artifact, jar, entry);
            }
        } catch (IOException e) {
            log.info("Get artifact file failed. [" + artifact.toString() + "]");
            return result;
        }
        return result;
    }

    private List<String> extractClassname(Artifact artifact, JarEntry entry) {
        List<String> result = new ArrayList<>();
        if (entry.getName().contains("META-INF")) {
            return result;
        }

        String classFullName = entry.getName();
        if (!classFullName.endsWith(".class")) {
            return result;
        }

        String className = classFullName.substring(0, classFullName.length() - 6).replace("/", ".");
        result.add(className);
        log.debug("Class: " + className + " detected in " + artifact.toString());
        return result;
    }

    private void extractLogbackSnippet(Artifact artifact, JarFile jar, JarEntry entry) throws IOException {
        if (entry.getName().equals("logback.snippet.xml")) {
            artifactLogbacksnippettextMap
                    .put(artifact, IOUtils.toString(jar.getInputStream(entry), Charset.defaultCharset()));
        }
    }


    public Map<String, Collection<Artifact>> getDuplicatedClassesMap() {
        return classnameArtifactMap.entrySet().stream()
                .filter(entry -> entry.getValue().size() > 1)
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
    }

    public boolean hasClass(String className) {
        return classnameArtifactMap.containsKey(className);
    }

    public List<Artifact> getAllArtifactsWithout(Artifact artifact) {
        return allArtifacts.stream()
                .filter(a -> !a.getId().equals(artifact.getId()))
                .collect(Collectors.toList());
    }

    public Map<Artifact, String> getArtifactLogbacksnippettextMap() {
        return new LinkedHashMap<>(this.artifactLogbacksnippettextMap);
    }

}
