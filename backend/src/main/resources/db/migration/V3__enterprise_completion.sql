-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/
-- 最新 SRM 功能清单第二轮生产化补充：对账明细、风险来源与审计查询索引。

CREATE TABLE srm_reconciliation_line (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  version BIGINT NOT NULL DEFAULT 0,
  deleted BOOLEAN NOT NULL DEFAULT FALSE,
  statement_id BIGINT NOT NULL,
  order_id BIGINT NOT NULL,
  order_line_id BIGINT NOT NULL,
  line_no INT NOT NULL,
  material_code VARCHAR(64) NOT NULL,
  ordered_quantity DECIMAL(16,4) NOT NULL,
  received_quantity DECIMAL(16,4) NOT NULL,
  unit_price DECIMAL(16,4) NOT NULL,
  order_amount DECIMAL(16,2) NOT NULL,
  settlement_amount DECIMAL(16,2) NOT NULL,
  created_at DATETIME(6) NOT NULL,
  updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_reconciliation_line_statement FOREIGN KEY (statement_id) REFERENCES srm_reconciliation(id),
  CONSTRAINT fk_reconciliation_line_order FOREIGN KEY (order_id) REFERENCES srm_purchase_order(id),
  CONSTRAINT fk_reconciliation_line_order_line FOREIGN KEY (order_line_id) REFERENCES srm_purchase_order_line(id),
  CONSTRAINT uk_reconciliation_order_line UNIQUE (statement_id, order_line_id)
);
CREATE INDEX idx_reconciliation_line_statement ON srm_reconciliation_line(statement_id, line_no);

ALTER TABLE srm_risk_alert ADD COLUMN source_type VARCHAR(40) NULL AFTER rule_code;
ALTER TABLE srm_risk_alert ADD COLUMN source_id VARCHAR(80) NULL AFTER source_type;
CREATE INDEX idx_risk_source_open ON srm_risk_alert(organization_code, rule_code, source_id, status, deleted);

CREATE INDEX idx_audit_org_actor_time ON srm_audit_log(organization_code, actor, occurred_at);
CREATE INDEX idx_audit_org_module_time ON srm_audit_log(organization_code, module, occurred_at);
