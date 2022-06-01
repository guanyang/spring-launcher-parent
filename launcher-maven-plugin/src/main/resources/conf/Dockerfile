FROM ${fromImage}

${instructionAfterFrom}

COPY ${packageModulePath}/target/*.launcher.tar.gz /tmp

RUN set -ex && \
    build_timestamp=$(date +%s%N) && \
    echo "Build timestamp: ${build_timestamp}" && \
    #echo ${build_timestamp} | sha256sum | base64 | head -c 32 | passwd --stdin root && \
    groupadd -g 571 www && \
    useradd -m www -u 571 -g 571 && \
    tar -zxf /tmp/*.launcher.tar.gz -C /home/www && \
    rm -rf /tmp/*.launcher.tar.gz && \
    ln -s /home/www/${appBaseDirectory}/bin/launcher.sh /usr/local/bin/launcher.sh && \
    chown -R 571:571 /usr/local/bin/launcher.sh && \
    chown -R 571:571 /home/www

USER www
WORKDIR /home/www

${instructionBeforeCmd}

CMD ['/bin/bash','-l']