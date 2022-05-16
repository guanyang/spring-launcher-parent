package org.gy.framework.launcher.maven.plugin.handler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.gy.framework.launcher.config.LauncherMsg;
import org.gy.framework.launcher.maven.plugin.conf.AppConf;
import org.gy.framework.launcher.maven.plugin.util.ResourceUtil;

/**
 * JavaAgent处理
 */
public class JavaAgentHandler extends AbstractConfListHandler<AppConf> {

    private static final String RESOURCE_FILE = "launcher.properties";

    private static final String JAVAAGENT_VERSION = System.getProperty("launcher.javaagent.download.version",
        ResourceUtil.getResourceProperty(RESOURCE_FILE, "javaagent.download.version"));

    private static final String JAVAAGENT_NAME = System.getProperty("launcher.javaagent.download.name",
        ResourceUtil.getResourceProperty(RESOURCE_FILE, "javaagent.download.name"));

    private static final String JAVAAGENT_URL_DOWNLOAD = System.getProperty("launcher.javaagent.download.url",
        ResourceUtil.getResourceProperty(RESOURCE_FILE, "javaagent.download.url"));

    private final String javaAgentDir;
    private final String javaAgentMappingFilePath;

    public JavaAgentHandler(MavenProject project, Log log, List<AppConf> conf) {
        super(project, log, conf);
        this.javaAgentDir = this.launcherTargetBaseDir + "/plugins/javaagent/";
        this.javaAgentMappingFilePath = this.javaAgentDir + "javaagent-mapping.properties";
    }

    @Override
    public void execute() throws IOException {
        FileUtils.deleteDirectory(new File(this.javaAgentDir));

        for (AppConf appConf : confList) {
            String appName = appConf.getName();
            File javaAgentFile = downloadAppJavaAgentFile(appName);
            generateJavaAgentMapping(appName, javaAgentFile.getName());
        }
    }

    private File downloadAppJavaAgentFile(String appName) throws IOException {
        String javaAgentFileName = JAVAAGENT_NAME;
        this.log.info("Find application [" + appName + "] agent version: " + javaAgentFileName);

        String javaAgentFileTarget = this.javaAgentDir + javaAgentFileName;
        File javaAgentFile = new File(javaAgentFileTarget);

        // javaAgent文件已经下载好，直接返回
        if (javaAgentFile.exists()) {
            this.log.info("JavaAgent file exists. [" + javaAgentFile.getAbsolutePath() + "]");
            return javaAgentFile;
        }

        //清理一下，目录准备重新下载
        FileUtils.forceMkdirParent(javaAgentFile);
        FileUtils.deleteQuietly(javaAgentFile);

        // javaAgent文件如果在Cache目录里有，则把Cache目录中的考过来直接用
        String javaAgentCacheDir = System.getProperty("launcher.javaagent.cache.dir",
            System.getProperty("java.io.tmpdir"));
        File javaAgentFileCache = new File(javaAgentCacheDir + File.separator + javaAgentFileName);

        if (javaAgentFileCache.exists()) {
            FileUtils.copyFile(javaAgentFileCache, javaAgentFile);
            this.log.info("JavaAgent cached file exists. [" + javaAgentFileCache.getAbsolutePath() + "]");
            return javaAgentFile;
        }

        // 本地跟缓存目录中都没有，则重新下载
        javaAgentFile = download(JAVAAGENT_URL_DOWNLOAD, javaAgentFileTarget);
        FileUtils.copyFile(javaAgentFile, javaAgentFileCache);
        return javaAgentFile;
    }

    private void generateJavaAgentMapping(String key, String value) throws IOException {
        File javaAgentMappingFile = new File(this.javaAgentMappingFilePath);
        if (!javaAgentMappingFile.exists()) {
            javaAgentMappingFile.createNewFile();
        }
        appendStringToFile(key + "=" + value, javaAgentMappingFile);
    }

    private File download(String url, String filePath) throws IOException {
        this.log.info("Get java agent from " + url);

        IOException exception = null;
        for (int i = 0; i < 3; i++) {
            try (InputStream inputStream = new URL(url).openStream()) {
                FileOutputStream fos = new FileOutputStream(filePath);
                IOUtils.copy(inputStream, fos);
                return new File(filePath);
            } catch (IOException e) {
                log.warn(e.getMessage());
                exception = e;
            }
        }
        throw new IOException(LauncherMsg.getBuildErrMsg("ERROR-10008", url, filePath), exception);
    }

    public void appendStringToFile(String content, File file) throws IOException {
        String contentWithNewLine = content + System.lineSeparator();
        try {
            Files.write(file.toPath(), contentWithNewLine.getBytes(Charset.defaultCharset().name()),
                StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new IOException("Append content to file error", e);
        }
    }

}
