# 知华 SRM 企业级供应商关系管理系统

[简体中文](README.md) | [English](README.en.md)

[![Java](https://img.shields.io/badge/Java-21-2f5b85)](backend/pom.xml)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-5f913a)](backend/pom.xml)
[![Vue](https://img.shields.io/badge/Vue-3.5-3f8b72)](frontend/package.json)
[![MySQL](https://img.shields.io/badge/MySQL-8.4-366a96)](compose.yaml)
[![License](https://img.shields.io/badge/license-personal%20non--commercial-c46b38)](LICENSE)

知华 SRM 是 **知华科技（上海如静知华信息科技有限公司）** 建设的前后端分离供应商关系管理系统，提供 PC 响应式采购管理端和供应商门户。系统围绕供应商准入、寻源、合同、订单、配额、交付、质量、结算和绩效形成可追踪的业务闭环。

- 官网：[https://www.zhuatech.cn/](https://www.zhuatech.cn/)
- Java 包名：`cn.zhuatech.srm`
- 技术栈：Java 21、Spring Boot 4、Vue 3、MySQL 8.4、Flyway、Docker Compose
- 许可：个人非商业学习交流；商用、企业内部生产使用和交付需书面授权

## 两类工作空间

| 工作空间 | 主要用户 | 能力 |
| --- | --- | --- |
| 采购管理端 | 管理员、采购、质量、财务、审计 | 供应商准入、询价定标、合同、订单、配额、收货、质量、结算、绩效、审计 |
| 供应商门户 | 供应商业务、质量和财务人员 | 企业资料、报价澄清、订单确认、ASN、质量整改、对账开票、绩效风险 |

所有受保护接口均在后端进行身份和权限校验；供应商数据按账号关联的供应商范围隔离，不能依赖前端隐藏菜单实现安全。

## 核心能力

- 供应商邀请注册、企业资料、联系人、供货范围、银行信息、证照到期、准入问卷与两级审批
- 合作、整改、暂停、淘汰、黑名单、重复供应商识别、资料变更历史
- RFQ/RFP、定向邀请、报价版本、澄清、比价、定标原因与结果通知
- 合同台账、附件、审批状态、版本和到期提醒
- 订单开放接口、Excel 原子导入、行级确认、数量/交期反馈、版本快照和异常待办
- 比例/数量配额、多供应商合计校验、版本、生效/失效、使用流水、余额、超配预警和 Excel 导出
- ASN、送货预约、发运、到货、收货数量和差异
- NCR/CAR、原因分析、临时/永久措施、8D 证据、退回、验证和关闭
- 基础对账、供应商确认/异议、发票登记、绩效评分、规则风险和整改跟踪
- 账号锁定、服务端权限、审计日志、乐观锁、软删除、受控附件、健康检查和备份恢复

最新功能完成情况见 [功能清单完成情况](docs/FEATURE_COMPLETION.md)，原始代码差距及处理方案见 [功能差距分析](docs/FEATURE_GAP_ANALYSIS.md)。

## 工程结构

```text
zhuatech-srm/
├── backend/       # Spring Boot 后端、Flyway 与自动化测试
├── frontend/      # Vue 3 采购管理端与供应商门户
├── docs/          # 架构、接口、数据库、用户说明和测试结果
├── deploy/        # 部署、备份和恢复脚本
├── compose.yaml   # MySQL、后端、前端编排
├── LICENSE
└── NOTICE
```

## 快速启动

系统不提供默认账号、演示账号、演示数据或万能密码。

```bash
cp .env.example .env
# 编辑 .env：设置三个不同的随机密钥；首次启动时临时启用管理员引导
docker compose up -d --build
```

浏览器访问 `http://localhost:8090`。首次管理员的安全创建和关闭引导流程见 [生产部署指南](deploy/README.md)。

本地研发：

```bash
cd backend && mvn test && mvn spring-boot:run
cd frontend && npm ci && npm run dev
```

## 交付文档

- [企业交付架构](docs/architecture.md)
- [REST API](docs/api.md)
- [数据库说明](docs/database.md)
- [用户操作说明](docs/USER_MANUAL.md)
- [部署、备份与恢复](deploy/README.md)
- [测试结果](docs/TEST_RESULTS.md)
- [已知限制与实施边界](docs/KNOWN_LIMITATIONS.md)

## 安全与上线

生产环境必须配置 HTTPS、可信 CORS、独立随机数据库和 JWT 密钥、持久化附件卷、日志轮转、备份保留及恢复演练。首次管理员创建完成后关闭引导开关。不要提交 `.env`、数据库备份、客户数据或生产附件。

## 许可边界

**本工程仅可用于个人、非商业性的学习、研究和技术交流，不得商用。** 企业内部生产使用、SaaS、私有化交付、收费服务、投标、商业培训及其他直接或间接商业用途，均须事先取得 **上海如静知华信息科技有限公司** 的书面授权。完整条款以 [LICENSE](LICENSE) 为准。

## 深度开发与商业授权

如需私有化部署、客户系统集成、功能定制、实施或商业授权，请联系知华科技。

| 微信咨询一 | 微信咨询二 |
| --- | --- |
| ![扫码添加微信咨询知华科技](docs/images/zhuatech-wechat-consulting.png) | ![扫码添加微信咨询知华科技](docs/images/zhuatech-wechat-consulting-2.png) |

更多企业信息化、中小企业 AI 转型、软件项目外包、实施、FDE 外包与 OPC 技术支持，请访问 [知华科技官网](https://www.zhuatech.cn/)。

Copyright © 2026 上海如静知华信息科技有限公司。保留所有权利。
