# launcher通用启动器
launcher是一个Java应用通用启动器，它不仅规范了应用的打包结构并且提供了众多可选的启动参数。

用户可以通过Maven插件的方式引入launcher，通过一些简单的配置即可将Java通过launcher启动起来。

## 快速接入

launcher的新用户如果想要快速接入项目，可以参考本工程**launcher-sample**模块下的pom.xml文件。

### 移除当前打包工具

在项目`pom.xml`中删除已有的打包工具配置，例如：`maven-assembly-plugin`、`spring-boot-maven-plugin`

### 添加detonator打包插件

添加`launcher-maven-plugin`到`pom.xml`中

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
                <apps>
                    <app>
                        <!-- 应用名 -->
                        <name>${APP_NAME}</name>
                        <!-- 启动类 -->
                        <mainClass>${MAIN_CLASS}</mainClass>
                    </app>
                </apps>
            </configuration>
        </execution>
    </executions>
</plugin>
````

替换配置文件中的${APP_NAME}为服务名称，例如：launcher-sample

替换配置文件中的${MAIN_CLASS}启动类名称，例如：org.gy.framework.launcher.sample.Main

### 开始构建

接入完成后，使用mvn package构建工程

package执行完毕后，会自动在模块中的`target`目录下生成${PROJECT_MODULE_NAME}-${VERSION}.tar.gz文件

### 启动工程

解压缩${PROJECT_MODULE_NAME}-${VERSION}.tar.gz文件

启动器主文件为 `./bin/launcher.sh`

使用 `./launcher.sh start -n ${APP_NAME}` 启动

使用 `./launcher.sh stop -n ${APP_NAME}` 停止

使用 `./launcher.sh restart -n ${APP_NAME}` 重启

想要查看更多选项请使用 `launcher.sh -h` 以及 `launcher.sh [start/stop/restart/status] -h`

## 更多

[启动器使用说明](docs/启动器使用说明.md)

[Maven插件配置](docs/Maven插件配置.md)

[日志配置指南](docs/日志配置指南.md)

[Dockerfile扩展支持](docs/Dockerfile.md)

[测试用例](docs/测试用例.md)

