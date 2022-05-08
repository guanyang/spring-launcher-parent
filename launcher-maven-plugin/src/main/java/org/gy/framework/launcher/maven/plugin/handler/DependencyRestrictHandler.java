package org.gy.framework.launcher.maven.plugin.handler;

import org.gy.framework.launcher.config.LauncherConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.artifact.versioning.InvalidVersionSpecificationException;
import org.apache.maven.artifact.versioning.Restriction;
import org.apache.maven.artifact.versioning.VersionRange;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class DependencyRestrictHandler extends AbstractHandler {


    public DependencyRestrictHandler(MavenProject project, Log log) {
        super(project, log);
        this.project = project;
        this.log = log;
    }

    @Override
    public void execute() throws IOException {
        Map<String, RestrictRule> restrictRuleMap = LauncherConfig.getInstance()
                .getConfigFromYamlToList("dependency-restrict/restrict.yml",RestrictRule.class)
                .stream()
                .collect(Collectors.toMap(RestrictRule::getDependency, (restrictRule) -> restrictRule));

        checkRequiredDependenciesExists(restrictRuleMap.entrySet()
                .stream()
                .filter(entry -> entry.getValue().getRequired())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue)));

        AtomicInteger bannedDependencySize = new AtomicInteger();
        AtomicInteger warnedDependencySize = new AtomicInteger();

        StringBuilder errorMsg = new StringBuilder();
        getProjectDependenciesAndPlugins().forEach(dependency -> {
            RestrictRule restrictRule = restrictRuleMap.get(dependency.getGroupId() + ":" + dependency.getArtifactId());

            if (null == restrictRule) {
                return;
            }

            if (isInVersionRange(dependency.getVersion(), restrictRule.getAvailableVersion())) {
                log.debug("Dependency " + dependency.toString()
                        + " version is in availableVersion list. Ignore check restrict.");
                return;
            }

            if (isInVersionRange(dependency.getVersion(), restrictRule.getBannedVersion())) {
                errorMsg.append(generateRestrictLog("banned", restrictRule.getBannedVersion(), dependency, restrictRule)).append(System.lineSeparator());
                bannedDependencySize.getAndIncrement();
            }

            if (isInVersionRange(dependency.getVersion(), restrictRule.getWarnedVersion())) {
                log.warn(generateRestrictLog("warned", restrictRule.getWarnedVersion(), dependency, restrictRule));
                warnedDependencySize.getAndIncrement();
            }

        });

        if (bannedDependencySize.get() > 0) {
            throw new IOException(
                    new StringBuilder("Banned dependencies found:").append(System.lineSeparator())
                        .append(errorMsg)
                        .append("You should have a update for these dependencies.")
                        .toString()
            );
        }

        if (warnedDependencySize.get() > 0) {
            long dependencyWarnRestrictDelaySeconds = LauncherConfig.getInstance()
                    .getDependencyWarnRestrictDelaySeconds().longValue();
            log.warn("Warned dependencies found, count " + warnedDependencySize + ", wait "
                    + dependencyWarnRestrictDelaySeconds + "s.");
            try {
                TimeUnit.SECONDS.sleep(dependencyWarnRestrictDelaySeconds);
            } catch (InterruptedException e) {
                log.error(e.getMessage());
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * 检查工程的依赖中是否包含所有RequiredDependencies
     * 如果没包含则抛出异常
     *
     * @param requiredRestrictRuleMap 所有required规则
     * @throws IOException
     */
    private void checkRequiredDependenciesExists(Map<String, RestrictRule> requiredRestrictRuleMap)
            throws IOException {

        Set<String> dependenciesWithOutVersion = getProjectDependenciesAndPlugins().stream()
                .map(dependency -> dependency.getGroupId() + ":" + dependency.getArtifactId())
                .collect(Collectors.toSet());

        Set<String> restrictRulesKeySet = new HashSet<>(requiredRestrictRuleMap.keySet());
        restrictRulesKeySet.removeAll(dependenciesWithOutVersion);
        StringBuilder errorMsg = new StringBuilder();
        if (CollectionUtils.isNotEmpty(restrictRulesKeySet)) {
            restrictRulesKeySet.forEach(requiredDependency -> {
                RestrictRule restrictRule = requiredRestrictRuleMap.get(requiredDependency);
                StringBuilder sb = new StringBuilder();
                sb.append(String.format("Required %s [%s] not found in the project %s list",
                        restrictRule.getDependencyType(), requiredDependency, restrictRule.getDependencyType()));

                if (StringUtils.isNoneEmpty(restrictRule.getMessage())) {
                    sb.append(", Message: ").append(restrictRule.getMessage());
                }

                if (StringUtils.isNoneEmpty(restrictRule.getRecommendVersion())) {
                    sb.append(", Recommend version: ").append(restrictRule.getRecommendVersion());
                }
                errorMsg.append(sb.toString()).append(System.lineSeparator());
            });

            throw new IOException(new StringBuilder("Required dependencies not found").append(System.lineSeparator())
                    .append(errorMsg)
                    .append("Please add these dependencies to the project.")
                    .toString());
        }
    }


    private String generateRestrictLog(String restrictType, String restrictRuleText, Dependency dependency,
            RestrictRule restrictRule) {
        StringBuilder sb = new StringBuilder();

        sb.append(String.format("Found %s %s [%s:%s:%s] in this project, %s version range: %s",
                restrictType, restrictRule.getDependencyType(),
                dependency.getGroupId(), dependency.getArtifactId(), dependency.getVersion(),
                restrictType, restrictRuleText));

        if (StringUtils.isNotEmpty(restrictRule.getMessage())) {
            sb.append(", Message: ").append(restrictRule.getMessage());
        }

        if (StringUtils.isNotEmpty(restrictRule.getRecommendVersion())) {
            sb.append(", Recommend version: ").append(restrictRule.getRecommendVersion());
        }
        return sb.toString();
    }

    private boolean isInVersionRange(String versionText, String versionRangeText) {
        if (StringUtils.isEmpty(versionText) || StringUtils.isEmpty(versionRangeText)) {
            return false;
        }
        if (StringUtils.contains(versionRangeText, "[" + versionText + "]")) {
            return true;
        }
        VersionRange versionRange = null;
        try {
            if (!versionRangeText.startsWith("(") && !versionRangeText.startsWith("[")) {
                throw new InvalidVersionSpecificationException(
                        "Version '" + versionRangeText + "' should be start with '(' or '['");
            }
            versionRange = VersionRange.createFromVersionSpec(versionRangeText);
        } catch (InvalidVersionSpecificationException e) {
            log.warn("Version range '" + versionRangeText + "' parse failed, message: " + e.getMessage(), e);
            return false;
        }
        ArtifactVersion version = new DefaultArtifactVersion(versionText);

        List<Restriction> restrictions = versionRange.getRestrictions();
        for (Restriction restriction : restrictions) {
            if (restriction.containsVersion(version)) {
                return true;
            }
        }

        return false;
    }

    private List<Dependency> getProjectDependenciesAndPlugins() {
        List<Dependency> dependencies = new ArrayList<>(project.getCompileDependencies());
        project.getBuild().getPlugins().forEach(plugin -> {
            Dependency dependency = new Dependency();
            dependency.setGroupId(plugin.getGroupId());
            dependency.setArtifactId(plugin.getArtifactId());
            dependency.setVersion(plugin.getVersion());
            dependencies.add(dependency);
        });
        return dependencies;
    }


    @Getter
    @Setter
    @ToString
    private static class RestrictRule {

        private String dependency;
        private String dependencyType = "dependency";
        private String availableVersion;
        private Boolean required = false;
        private String bannedVersion;
        private String warnedVersion;
        private String recommendVersion;
        private String message;
    }
}
