package org.gy.framework.launcher.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.representer.Representer;

class ResourceLoader {

    private String [] resourceBasePaths = new String[]{"/"};

    ResourceLoader(String... resourceBasePaths) {
        if(ArrayUtils.isEmpty(resourceBasePaths)){
            return;
        }
        for (int i = 0; i < resourceBasePaths.length; i++) {
            if (!resourceBasePaths[i].endsWith("/")) {
                resourceBasePaths[i] = resourceBasePaths[i] + "/";
            }
        }
        this.resourceBasePaths = resourceBasePaths;

    }

    InputStream getResourceAsStream(String path) throws IOException {
        if(path.startsWith("/")) {
            return getResourceAsStream(path.substring(1));
        }
        for (String basePath : resourceBasePaths) {
            InputStream resourceAsStream = LauncherConfig.class.getResourceAsStream(basePath + path);
            if (null != resourceAsStream) {
                return resourceAsStream;
            }
        }
        throw new IOException(
                String.format("Resource [%s] not found in basePath %s", path, Arrays.toString(resourceBasePaths)));
    }

    String getResourceAsString(String path) throws IOException {
        return IOUtils.toString(getResourceAsStream(path), Charset.defaultCharset());
    }

    <T> List<T> getResourceFromYamlToList(String path, Class<T> clazz) throws IOException {
        Yaml yaml = newYaml();
        List<Map> loadResult = yaml.load(getResourceAsString(path));
        if(null == loadResult){
            return new ArrayList<>();
        }
        return loadResult.stream()
                .map(mapEntity -> new ObjectMapper().convertValue(mapEntity, clazz))
                .collect(Collectors.toList());
    }

    <T> T getResourceFromYamlToObject(String path, Class<T> clazz) throws IOException {
        Yaml yaml = newYaml();
        return yaml.loadAs(getResourceAsString(path), clazz);
    }

    private Yaml newYaml() {
        Representer representer = new Representer();
        representer.getPropertyUtils().setSkipMissingProperties(true);
        return new Yaml(representer);
    }

    Properties getResourceAsProperties(String path)  {
        Properties properties = new Properties();
        try {
            properties.load(new InputStreamReader(getResourceAsStream(path),Charset.defaultCharset()));
        } catch (IOException e) {
            Logger.get().error(String.format("Resource file [%s] load failed.",path),e);
        }
        return properties;
    }
}
