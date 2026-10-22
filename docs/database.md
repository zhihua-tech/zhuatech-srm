# 数据库说明

数据库：MySQL 8.4，字符集 `utf8mb4`，表前缀 `srm_`，结构由 Flyway 管理。

## 核心表

| 业务域 | 表 | 说明 |
| --- | --- | --- |
| 平台 | `srm_organization`、`srm_user`、`srm_role_permission`、`srm_inbox_item`、`srm_audit_log` | 组织、账号、组织级角色权限、待办和不可变审计记录 |
| 供应商 | `srm_supplier`、`srm_supplier_invitation`、`srm_supplier_contact`、`srm_supplier_certificate`、`srm_supplier_onboarding`、`srm_supplier_change_record` | 邀请注册、资料、证照、准入和变更历史 |
| 寻源 | `srm_sourcing_event`、`srm_sourcing_item`、`srm_sourcing_invitation`、`srm_quote`、`srm_sourcing_clarification` | RFQ/RFP、定向邀请、报价版本和澄清 |
| 合同 | `srm_contract` | 合同编号、版本、金额、附件和状态 |
| 订单 | `srm_purchase_order`、`srm_purchase_order_line`、`srm_purchase_order_version` | 订单头行、确认反馈和版本快照 |
| 配额 | `srm_quota_plan`、`srm_quota_allocation`、`srm_quota_usage` | 配额版本、供应商分配、执行流水与余额 |
| 交付 | `srm_asn`、`srm_asn_line` | 送货通知、预约、发运、收货与差异 |
| 质量 | `srm_quality_issue` | NCR/CAR、原因、措施、证据、退回、验证与关闭 |
| 结算 | `srm_reconciliation`、`srm_reconciliation_line`、`srm_invoice` | 对账头、订单行/实收金额快照、供应商确认/异议和发票登记 |
| 绩效风险 | `srm_supplier_performance`、`srm_risk_alert` | 周期评分、业务来源型风险预警和整改跟踪 |
| 文件 | `srm_file_asset` | 文件元数据、哈希、归属和受控存储路径 |

## 关键设计

- 所有可变业务实体带 `version`、`deleted`、`created_at`、`updated_at`。
- 供应商编码、询价编号、订单号和质量问题编号按组织唯一。
- 配额范围索引覆盖组织、采购组织、工厂、物料和状态；执行流水以“分配 + 来源类型 + 来源 ID”唯一，防止重复扣减。
- 订单、ASN、证照、合同、审计、消息等高频查询均有组合索引。
- 银行账号仅在表中保存原值，普通响应只返回掩码；生产部署建议接入 KMS 或数据库列加密。

## 迁移

```text
backend/src/main/resources/db/migration/
├── V1__init.sql
├── V2__enterprise_srm.sql
└── V3__enterprise_completion.sql
```

新数据库会顺序执行全部迁移；旧版数据库继续执行尚未应用的版本。V3 增加对账行快照、风险来源和审计检索索引。不要修改已在共享环境执行过的迁移文件，后续结构变更应新增 V4、V5。

## 备份与恢复

使用 `deploy/backup.sh` 生成事务一致的 gzip 备份，使用 `deploy/restore.sh` 恢复。恢复必须先在隔离环境演练，并在恢复后执行健康检查和核心流程冒烟测试。
