# REST API

统一前缀 `/api`，除登录、受邀供应商注册和健康检查外均需要 `Authorization: Bearer <token>`。统一成功响应：

```json
{"success": true, "message": "操作成功", "data": {}}
```

失败响应返回明确消息；订单导入校验失败时 `data.errors` 包含行号、字段和原因。

## 账号与工作台

| 方法 | 路径 | 权限 | 说明 |
| --- | --- | --- | --- |
| POST | `/api/auth/login` | 公开 | 登录、失败次数与锁定控制 |
| GET | `/api/auth/me` | 已登录 | 当前账号和权限 |
| POST | `/api/auth/change-password` | 已登录 | 修改密码 |
| GET | `/api/admin/dashboard` | `DASHBOARD` | 采购工作台 |
| GET | `/api/portal/dashboard` | `PORTAL` | 供应商工作台 |
| GET | `/api/inbox` | `MESSAGE` | 消息与待办 |
| POST | `/api/inbox/{id}/{action}` | `MESSAGE` | `READ` / `COMPLETE` |
| GET | `/api/admin/audit-logs` | `AUDIT_READ` | 分页审计日志；支持 `actor`、`module`、`fromTime`、`toTime` |

## 供应商

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/admin/suppliers/page` | 分页供应商库 |
| POST | `/api/admin/supplier-invitations` | 创建限时邀请 |
| POST | `/api/public/suppliers/register` | 使用邀请令牌注册 |
| GET/PUT | `/api/admin/suppliers/{id}` | 资料详情与变更留痕 |
| POST | `/api/admin/suppliers/{id}/contacts` | 新增联系人 |
| POST | `/api/admin/suppliers/{id}/certificates` | 新增资质证照 |
| PATCH | `/api/admin/suppliers/{id}/status` | 整改、暂停、淘汰和黑名单 |
| GET/PUT | `/api/portal/profile` | 供应商查看/修改自身资料 |
| POST | `/api/portal/profile/contacts` | 供应商新增自身联系人 |
| POST | `/api/portal/profile/certificates` | 供应商新增自身资质证照 |
| PUT | `/api/portal/onboarding/questionnaire` | 保存准入问卷 |
| POST | `/api/portal/onboarding/submit` | 提交准入 |
| POST | `/api/admin/onboarding/{id}/approve` | 初审、终审或退回 |

## 寻源、合同、订单与配额

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET/POST | `/api/admin/sourcing-events` | 查询、新建 RFQ/RFP |
| POST | `/api/admin/sourcing-events/{id}/publish` | 发布定向询价 |
| GET | `/api/admin/sourcing-events/{id}/compare` | 比价 |
| POST | `/api/admin/sourcing-events/{eventId}/quotes/{quoteId}/evaluate` | 价格、交期、质量和服务基础综合评审 |
| POST | `/api/admin/sourcing-events/{id}/award` | 提交定标审批 |
| POST | `/api/admin/sourcing-events/{id}/award/approval` | 定标审批或驳回，通过后通知结果 |
| GET | `/api/portal/sourcing-events` | 供应商受邀项目 |
| POST | `/api/portal/sourcing-events/{id}/quotes` | 提交报价版本 |
| POST | `/api/portal/sourcing-events/{id}/clarifications` | 提交澄清 |
| GET/POST | `/api/contracts` | 合同台账与初始版本 |
| GET | `/api/contracts/{id}` | 合同详情、履约金额/比率及完整版本历史 |
| POST | `/api/contracts/{id}/versions` | 基于最新已完成审批版本创建不可覆盖的新版本 |
| POST | `/api/contracts/{id}/{action}` | `SUBMIT` / `APPROVE` / `ACTIVATE` / `EXPIRE` / `TERMINATE` |
| GET/POST | `/api/admin/orders` | 采购订单分页与开放接口创建 |
| POST | `/api/admin/orders/import` | xlsx 原子导入 |
| GET | `/api/admin/orders/import-template` | 下载与服务端校验一致的 xlsx 模板 |
| POST | `/api/admin/orders/{id}/versions` | 创建订单变更版本 |
| GET | `/api/portal/orders/list` | 当前供应商订单 |
| POST | `/api/portal/orders/{orderId}/lines/{lineId}/confirm` | 数量及交期反馈 |
| POST | `/api/portal/orders/{orderId}/confirm` | 整单确认，可统一反馈交期和备注 |
| GET/POST | `/api/admin/quotas` | 配额查询与草稿创建 |
| POST | `/api/admin/quotas/{id}/activate` | 校验合计并启用版本 |
| POST | `/api/admin/quotas/{id}/expire` | 失效版本 |
| POST | `/api/admin/quotas/{id}/manual-usage` | 手工执行流水 |
| GET | `/api/admin/quotas/export` | Excel 导出执行余额 |

## 交付、质量、结算与绩效

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/asns` | 数据域内 ASN |
| GET | `/api/asns/{id}` | 按组织/供应商数据域查看 ASN 头行明细 |
| POST | `/api/portal/asns` | 创建 ASN |
| POST | `/api/asns/{id}/appointment` | 预约送货 |
| POST | `/api/portal/asns/{id}/ship` | 发运登记 |
| POST | `/api/admin/asns/{id}/receive` | 收货和差异记录 |
| GET | `/api/quality-issues` | 数据域内质量问题 |
| GET | `/api/quality-issues/{id}` | 质量问题详情及对象级操作历史 |
| GET | `/api/admin/quality-issues/export` | 按当前采购组织导出质量问题 Excel |
| POST | `/api/admin/quality-issues` | 创建 NCR/CAR |
| POST | `/api/portal/quality-issues/{id}/start` | 开始原因分析 |
| POST | `/api/portal/quality-issues/{id}/actions` | 提交整改和证据 |
| POST | `/api/admin/quality-issues/{id}/review` | 接受或退回整改 |
| POST | `/api/admin/quality-issues/{id}/close` | 验证关闭 |
| GET/POST | `/api/reconciliations`、`/api/admin/reconciliations` | 查询及生成基础对账 |
| GET | `/api/reconciliations/{id}` | 对账头、冻结订单行、实收金额及发票明细 |
| POST | `/api/portal/reconciliations/{id}/action` | 确认或提出异议 |
| POST | `/api/portal/reconciliations/{id}/invoices` | 发票登记 |
| GET/POST | `/api/performance`、`/api/admin/performance` | 绩效查询和评分 |
| GET | `/api/risks` | 风险预警 |
| POST | `/api/risks/{id}/action` | 分配、验证或关闭整改 |

## 文件与系统管理

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/files` | `FILE`；受控上传，限制扩展名、签名和大小 |
| GET | `/api/files/{fileKey}` | `FILE`；按组织和供应商归属下载 |
| GET/POST | `/api/admin/system/organizations` | 组织管理 |
| GET/POST | `/api/admin/system/users` | 账号与角色管理 |
| PATCH | `/api/admin/system/users/{id}/enabled` | 启停账号 |
| GET/PUT | `/api/admin/system/roles/{role}/permissions` | 按组织查询或替换角色权限 |
| POST | `/api/admin/system/business-rules/refresh` | 管理员手动执行合同/证照/订单/质量提醒与风险规则 |

OpenAPI 平台接入可基于本表扩展 springdoc；当前交付的准确请求模型以各 Controller 对应 Service 内的 `record` DTO 为准。
