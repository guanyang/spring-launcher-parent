### 背景
- launcher提供了`Dockerfile`生成功能，支持定制化基础运行环境
- 用户可以在`launcher-maven-plugin`的 pom 配置中对 `Dockerfile` 进行修改，从而支持对业务基础环境的定制化
- 进行 mvn package 之后，`Dockerfile`会生成在`${project_module}/target/Dockerfile`

### Dockerfile扩展点配置
- `<fromImage/>`：该元素中的内容会替换 Dockerfile 中 `FROM` 指令原本的配置
- `<instructionAfterFrom/>`：该元素中的内容会添加到 Dockerfile 的 `FROM` 指令之后，通常用来进行一些基础组件的安装（yum install）
  - 使用 Dockerfile 的 `RUN` 指令时，执行用户为 `root`，当前工作目录为 /
  - 执行该过程时还未拷贝应用程序安装包，用户无法获取应用安装包内的文件
  - 该过程会使用到Docker镜像构建层级缓存，在第二次构建时，如果指令内容未改变，会直接使用上次生成好的镜像层以提升构建速度。
- `<instructionBeforeCmd/>`：CMD指令前执行的语句，可以用来进行应用安装包内的文件拷贝，如：拷贝安装包内的指定文件到指定目录下
  - 使用 Dockerfile 的 `RUN` 指令时，执行用户为 `www`，当前工作目录为 `/home/www`

### 完整配置示例
```
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
              
                <!-- Dockerfile 配置从这里开始 -->
                <dockerfile>
                    <!-- 示例：修改基础镜像 -->
                    <fromImage>hub.docker.com/repository/docker/guanyangsunlight/default-repo/java-centos8:1.8_201_b09</fromImage>
                    
                    <!-- 示例：安装一些自定义组件 -->
                    <instructionAfterFrom>
                        <![CDATA[
                        RUN set -ex && \
                        yum -y install openssl python-pip && \
                        pip install pyelliptic==1.5.7
                        ]]>
                    </instructionAfterFrom>
                    
                    <!-- 示例：拷贝文件到指定目录 -->
                    <instructionBeforeCmd>
                        <![CDATA[
                        RUN set -ex && \
                        mv /home/www/launcher-sample/conf/xxxx  /home/www
                        ]]>
                    </instructionBeforeCmd>
                </dockerfile>
              
            </configuration>
        </execution>
    </executions>
</plugin>
```
> 注意所有的语句使用 && \ 进行换行
- RUN set -ex && \ 说明如下：
  - `RUN`: Dockerfile 指令，后续接上我们常规的 shell 操作命令；
  - `set -ex`: 执行命令时开启调试模式打命令执行过程，遇到错误则终止后续后续命令执行；