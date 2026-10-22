# 生产部署指南

## 前置条件

- Docker 24+、Docker Compose v2
- Linux x86_64/arm64，建议 4 核、8 GB 内存、100 GB SSD 起步
- 可用域名与 TLS 证书
- 独立备份目录或对象存储

## 首次启动

1. 复制配置：`cp .env.example .env`。
2. 为数据库、root 和 JWT 分别生成不同的随机强密钥；限制 `.env` 文件权限。
3. 首次启动时临时设置：

   ```text
   BOOTSTRAP_ADMIN_ENABLED=true
   BOOTSTRAP_ADMIN_USERNAME=<企业管理员账号>
   BOOTSTRAP_ADMIN_PASSWORD=<至少12位且包含字母和数字的随机密码>
   BOOTSTRAP_ORGANIZATION_CODE=<组织编码>
   ```

4. 执行 `docker compose up -d --build`，查看 `docker compose ps` 和后端日志。
5. 登录后修改管理员密码，把 `BOOTSTRAP_ADMIN_ENABLED` 改为 `false`，再重建后端容器。

系统不内置演示账号、演示数据、万能密码或绕过权限的接口。

## 运行参数

- `FILE_STORAGE_MAX_SIZE`、`MAX_FILE_SIZE`、`MAX_REQUEST_SIZE`：附件业务限制及 HTTP 请求限制，应保持一致。
- `MAX_LOGIN_ATTEMPTS`、`LOGIN_LOCK_MINUTES`：连续登录失败锁定策略。
- `BUSINESS_RULE_CRON`：合同履约/到期、证照、订单和质量提醒扫描计划，默认每天 01:15 执行；多实例部署时应确保仅一个实例执行定时任务，或接入客户统一调度平台。
- 管理员也可以调用 `/api/admin/system/business-rules/refresh` 在上线或数据导入后立即刷新规则。

## HTTPS 与网络

Compose 默认仅发布前端端口。生产环境应在前端前部署 Nginx、Caddy 或云负载均衡，强制 HTTPS、HSTS 和合理的请求体上限。`CORS_ORIGINS` 只配置实际 HTTPS 域名。MySQL 与后端不要直接暴露公网端口。

## 健康与日志

- 存活探针：`GET /actuator/health/liveness`
- 就绪探针：`GET /actuator/health/readiness`
- 管理员可访问基础 `metrics`；公网入口只放行 health。
- 运行日志由容器平台采集并设置轮转；不要在日志中记录密码、JWT、银行账号或附件内容。

## 备份恢复

在项目根目录加载 `.env` 后执行：

```bash
set -a; . ./.env; set +a
./deploy/backup.sh /srv/backup/zhuatech-srm
./deploy/restore.sh /srv/backup/zhuatech-srm/srm-YYYYMMDD-HHMMSS.sql.gz
```

建议每天全量备份并设置异地保留策略。每季度在隔离环境做一次恢复演练；恢复后执行后端测试、登录、供应商隔离、订单和配额冒烟验证。

## 升级

1. 备份数据库和附件卷。
2. 阅读版本说明与新增 Flyway 脚本。
3. 在预生产环境验证。
4. 执行 `docker compose up -d --build`；Flyway 会在应用启动前升级结构。
5. 检查健康、错误日志和核心业务流程。

商业授权、生产部署和二次开发支持：[https://www.zhuatech.cn/](https://www.zhuatech.cn/)
