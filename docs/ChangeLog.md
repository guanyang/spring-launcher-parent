### 1.0.1-SNAPSHOT
- 添加`skywalking-agent`插件支持，方便链路追踪
- Javaagent默认下载路径`dlcdn.apache.org`，如果构建时下载过慢，可以在`launcher-maven-plugin/pom.xml`中修改`${javaagent.download.url}`变量
- 增加Javaagent本地文件缓存机制，提升打包速度，缓存目录可以通过`-Dlauncher.javaagent.cache.dir`设置，默认为系统变量`java.io.tmpdir`

### 1.0.0-SNAPSHOT
- 版本初始化
- 规范应用打包结构，并且提供众多可选的启动参数，参考[启动参数](docs/启动器使用说明.md)
- 通过maven插件简单配置，即可快速引用，参考[Maven插件配置](docs/Maven插件配置.md)
- 统一应用日志路径及格式，并且提供了零配置开箱即用的特性，参考[日志配置指南](docs/日志配置指南.md)
- 自动生成`Dockerfile`文件，方便容器化部署，参考[Dockerfile扩展支持](docs/Dockerfile扩展支持.md)