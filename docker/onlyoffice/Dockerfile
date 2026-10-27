# 允许 Document Server 从 host.docker.internal 拉取 docx（默认 default.json 为 false）
FROM onlyoffice/documentserver:8.2.2
RUN sed -i 's/"allowPrivateIPAddress": false/"allowPrivateIPAddress": true/' /etc/onlyoffice/documentserver/default.json \
 && sed -i 's/"allowMetaIPAddress": false/"allowMetaIPAddress": true/' /etc/onlyoffice/documentserver/default.json
