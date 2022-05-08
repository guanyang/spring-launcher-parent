package org.gy.framework.launcher.maven.plugin.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class ResourceUtil {

    private ResourceUtil() {
    }

    public static String loadAsString(String resourcePath) throws IOException {
        try {
            return IOUtils.toString(Thread.currentThread().getContextClassLoader().getResourceAsStream(resourcePath),
                    Charset.defaultCharset());
        } catch (IOException e) {
            throw new IOException("Resource '" + resourcePath + "' load failed", e);
        }
    }

    public static void saveToFile(String content, String path) throws IOException {
        File dest = new File(path);
        try {
            FileUtils.forceMkdirParent(dest);
            FileUtils.writeByteArrayToFile(dest, content.getBytes(Charset.defaultCharset().name()));
        } catch (IOException e) {
            throw new IOException(e);
        }
    }

    public static List<String> getResourceFiles(String path) throws IOException {
        List<String> filenames = new ArrayList<>();
        try (
                InputStream in = ResourceUtil.class.getResourceAsStream(path);
                BufferedReader br = new BufferedReader(new InputStreamReader(in, Charset.defaultCharset()))) {
            String resource;

            while ((resource = br.readLine()) != null) {
                filenames.add(resource);
            }
        }

        return filenames;
    }


    public static Collection<String> listResources(String path) throws IOException, URISyntaxException {
        return listResources(ResourceUtil.class, path);
    }

    public static Collection<String> listResources(Class clazz, String path) throws IOException, URISyntaxException {
        // TODO file正常读，jar特殊处理
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (dirURL != null && "file".equals(dirURL.getProtocol())) {
            List<String> result = new ArrayList<>();
            for (File file : FileUtils.listFiles(new File(dirURL.toURI()), null, true)) {
                result.add(file.getPath());
            }
            return result;
        }

        if (dirURL == null) {
            /*
             * In case of a jar file, we can't actually find a directory.
             * Have to assume the same jar as clazz.
             */
            String me = clazz.getName().replace(".", "/") + ".class";
            dirURL = clazz.getClassLoader().getResource(me);
        }

        if (dirURL != null && "jar".equals(dirURL.getProtocol())) {
            /* A JAR path */
            //strip out only the JAR file
            String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf('!'));
            JarFile jar = new JarFile(URLDecoder.decode(jarPath, Charset.defaultCharset().name()));

            //gives ALL entries in jar
            Enumeration<JarEntry> entries = jar.entries();

            //avoid duplicates in case it is a subdirectory
            Collection<String> results = new ArrayList<>();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();

                if (name.startsWith(path) && !name.endsWith("/")) {
                    results.add(name);
                }
            }
            jar.close();
            return results;
        }

        throw new UnsupportedOperationException("Cannot list files for URL " + dirURL);
    }

    public static String getResourceProperty(String resourceFileName, String property) {
        String resourceContent = null;
        try {
            resourceContent = loadAsString(resourceFileName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (String propertyPair : resourceContent.split("\\r\\n|\\n|\\r")) {
            propertyPair = propertyPair.trim();
            if (propertyPair.startsWith(property + "=")) {
                return propertyPair.substring(property.length()+1).trim();
            }
        }
        throw new RuntimeException("Property '" + property + "' not found in resource file '" + resourceFileName + "'");
    }
}
