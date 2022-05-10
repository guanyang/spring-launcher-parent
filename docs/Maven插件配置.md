# launcher-maven-plugin 配置
>启动器在构建阶段提供了丰富的选项帮助用户实现各类打包需求

## 添加maven配置
````xml
<!-- Project pom.xml -->
<project>
    [...]
    <build>
        [...]
        <plugins>
            [...]
            <plugin>
                <groupId>org.gy.framework</groupId>
                <artifactId>launcher-maven-plugin</artifactId>
                <version>1.0.0-SNAPSHOT</version>
                <executions>
                    <execution>
                        <goals>
                            <goal>launcher</goal>
                        </goals>
                        <configuration>
                            <!-- All launcher-maven-plugin configurations -->
                        </configuration>
                    </execution>
                </executions>
            </plugin>
            [...]
        </plugins>
        [...]
    </build>
    [...]
</project>
````

## 应用基础配置

在插件中配置 `应用名称` 跟 `MainClass`的对应关系，应用启动时可以直接通过 `-n ${APP_NAME}` 启动该应用

````xml
<configuration>
    <app>
        <!-- 应用名 -->
        <name>${APP_NAME}</name>
        
        <!-- 应用启动类 -->
        <mainClass>${APP_MAIN_CLASS}</mainClass>
    </app>
</configuration>
````

## 修改默认日志级别

````xml
<plugin>
    <groupId>org.gy.framework</groupId>
    <artifactId>launcher-maven-plugin</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <executions>
        <execution>
            <goals>
                <goal>launcher</goal>
            </goals>
            <configuration>
                <log>
                    <!-- 修改org.gy.framework日志级别，默认INFO -->
                    <logName>org.gy.framework</logName>
                    <logLevel>DEBUG</logLevel>
                    
                    <!-- 修改root日志级别，默认WARN -->
                    <rootLogLevel>ERROR</rootLogLevel>
                    
                    <!-- 单个日志文件大小（MB），默认500 -->
                    <logFileSize>1024</logFileSize>
                    
                    <!-- 每天日志文件数量，默认10 -->
                    <logFileCount>5</logFileCount>
                </log>
            </configuration>
        </execution>
    </executions>
</plugin>
````

## 追加maven-assembly-plugin配置

`launcher-maven-plugin` 允许用户将某些文件放入最终的应用打包文件中

```xml
<configuration>
    <!-- maven-assembly-plugin 打包配置文件-->
    <fileSet>package.xml</fileSet>
</configuration>
```

### 示例

将工程中的 `./src/main/resources/my-app.config` 放到打包结果中的 `conf` 目录下

编写 `maven-assembly-plugin` 的配置文件 `package.xml`

````xml
<fileSet>
    <directory>./src/main/resources</directory>
    <includes>
        <include>my-app.config</include>
    </includes>
    <outputDirectory>conf</outputDirectory>
    <lineEnding>unix</lineEnding>
</fileSet>
````

目前仅支持 `maven-assembly-plugin` 配置中的 `<fileSet>` 片段

访问 http://maven.apache.org/plugins/maven-assembly-plugin 了解更多关于 `<fileSet>` 的使用方法


## 默认JVM参数修改

`launcher` 提供了一系列标准的JVM默认参数, [jvm.options](../launcher-config/src/main/resources/launcher-config/conf/jvm.options)

并且提供了修改这些默认参数的方法，减少在启动时输入带来的复杂度

```xml
<configuration>
    <app>
        <name>${APP_NAME}</name>
        <mainClass>${APP_MAIN_CLASS}</mainClass>
        <jvmOption>
            <!-- 添加默认jvm参数可以使用<include> -->
            <include>
                -Dapp.test1=1
                -Dapp.test2=2
            </include>
            
            <!-- 排除默认jvm参数可以使用<exclude>标签 -->
            <exclude>
                -XX:+UseGCLogFileRotation
                -XX:NumberOfGCLogFiles
                -XX:GCLogFileSize
            </exclude>
        </jvmOption>
    </app>
</configuration>
```

打包完成后，在 `./conf` 目录下会生成一个 `${APP_NAME}.jvm.options` 文件其中包含已修改的配置


