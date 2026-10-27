# OnlyOffice 镜像拉不下来（国内网络）

`onlyoffice/documentserver` 约 **3～4GB**，直连 Docker Hub 容易长时间卡在 `Pulling` 或 `EOF`。

## 1. 先停掉卡住的拉取

在终端 **Ctrl+C** 结束当前的 `docker compose up` / `docker pull`，避免 Docker 一直占着不完整层。

## 2. 推荐：镜像加速拉取 + 打本地 tag（不改 compose 逻辑）

`docker-compose.yml` 支持环境变量 **`ONLYOFFICE_IMAGE`**。任选一种能跑通的源拉取，再 **tag 成 compose 默认名**：

```powershell
# 方式 A：DaoCloud 代理（常见）
docker pull docker.m.daocloud.io/onlyoffice/documentserver:8.2.2
docker tag docker.m.daocloud.io/onlyoffice/documentserver:8.2.2 onlyoffice/documentserver:8.2.2

# 方式 B：轩辕镜像公共加速（需按站点说明，示例）
# docker pull docker.xuanyuan.run/onlyoffice/documentserver:8.2.2
# docker tag docker.xuanyuan.run/onlyoffice/documentserver:8.2.2 onlyoffice/documentserver:8.2.2

# 方式 C：华为云 SWR 同步站（注意选 linux/amd64 标签，勿用 arm64）
# docker pull swr.cn-north-4.myhuaweicloud.com/ddn-k8s/docker.io/onlyoffice/documentserver:8.2.2
# docker tag ... onlyoffice/documentserver:8.2.2

cd D:\java\project\full-project\GymMind
docker compose up -d onlyoffice
```

也可在 `.env` 里直接指定镜像（跳过 tag）：

```env
ONLYOFFICE_IMAGE=docker.m.daocloud.io/onlyoffice/documentserver:8.2.2
```

## 3. Docker Desktop 配置代理 / 镜像加速

- **有 VPN/公司代理**：Docker Desktop → Settings → Resources → **Proxies**，填 `http://127.0.0.1:端口`，Apply & Restart 后再 `docker pull`。
- **Registry mirrors**：国内很多公共 mirror 已不稳定，优先 **代理** 或 **上面带前缀的 pull + tag**。

## 4. 离线包（最后手段）

在能访问 Docker Hub 的机器上：

```bash
docker pull onlyoffice/documentserver:8.2.2
docker save onlyoffice/documentserver:8.2.2 -o onlyoffice-ds-8.2.2.tar
```

拷贝到本机后：

```powershell
docker load -i onlyoffice-ds-8.2.2.tar
docker compose up -d onlyoffice
```

## 5. 暂时不用 OnlyOffice 时

业务链路（改 docx → 重解析 → 发布 → 检索）可不依赖 UI，用脚本验证：

```powershell
python scripts/e2e-docx-edit-test.py
```

关闭在线编辑：`.env` 或环境变量 `GYMMIND_KNOWLEDGE_ONLYOFFICE_ENABLED=false`，前端不再加载 OnlyOffice 脚本。

## 6. 编辑器壳能开、正文一直灰色骨架屏

OnlyOffice 默认 **禁止从私网 URL 下载文档**（`allowPrivateIPAddress: false`）。  
GymMind 的 `document.url` 使用 `http://host.docker.internal:8080/...`。项目用 [`docker/onlyoffice/Dockerfile`](../docker/onlyoffice/Dockerfile) 在镜像内打开 `allowPrivateIPAddress`（勿只读挂载 `local.json`，会导致 OnlyOffice 启动失败、8088 无响应）。

```powershell
docker compose build onlyoffice
docker compose up -d onlyoffice --force-recreate
```

## 7. 拉成功后的自检

```powershell
curl http://localhost:8088/welcome/
docker exec gymmind-onlyoffice curl -sf http://host.docker.internal:8080/actuator/health
```

浏览器打开：`http://localhost:5173/knowledge/<docx文档id>/edit`。
