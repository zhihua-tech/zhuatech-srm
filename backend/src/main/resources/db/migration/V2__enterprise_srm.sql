-- Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/
-- 企业级 SRM 业务结构升级。

ALTER TABLE srm_supplier
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN organization_code VARCHAR(32) NOT NULL DEFAULT 'DEFAULT',
  ADD COLUMN registration_no VARCHAR(32) NULL,
  ADD COLUMN legal_representative VARCHAR(80) NULL,
  ADD COLUMN registered_address VARCHAR(300) NULL,
  ADD COLUMN contact_email VARCHAR(120) NULL,
  ADD COLUMN bank_account_name VARCHAR(120) NULL,
  ADD COLUMN bank_name VARCHAR(120) NULL,
  ADD COLUMN bank_account_no VARCHAR(64) NULL,
  ADD COLUMN status_reason VARCHAR(500) NULL,
  MODIFY COLUMN category_name VARCHAR(200) NOT NULL,
  MODIFY COLUMN status VARCHAR(24) NOT NULL,
  MODIFY COLUMN rating DECIMAL(5,2) NOT NULL;
DROP INDEX supplier_code ON srm_supplier;
CREATE UNIQUE INDEX uk_supplier_org_code ON srm_supplier(organization_code, supplier_code);
CREATE UNIQUE INDEX uk_supplier_org_registration ON srm_supplier(organization_code, registration_no);
CREATE INDEX idx_supplier_org_status ON srm_supplier(organization_code, status, deleted);

ALTER TABLE srm_user
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN organization_code VARCHAR(32) NOT NULL DEFAULT 'DEFAULT',
  ADD COLUMN failed_login_attempts INT NOT NULL DEFAULT 0,
  ADD COLUMN locked_until DATETIME(6) NULL,
  ADD COLUMN password_changed_at DATETIME(6) NULL,
  ADD COLUMN last_login_at DATETIME(6) NULL,
  MODIFY COLUMN username VARCHAR(64) NOT NULL;
CREATE INDEX idx_user_org_role ON srm_user(organization_code, role, enabled);

ALTER TABLE srm_sourcing_event
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN organization_code VARCHAR(32) NOT NULL DEFAULT 'DEFAULT',
  ADD COLUMN type VARCHAR(8) NOT NULL DEFAULT 'RFQ',
  ADD COLUMN requirement_description VARCHAR(1000) NULL,
  ADD COLUMN published_at DATETIME(6) NULL,
  ADD COLUMN proposed_quote_id BIGINT NULL,
  ADD COLUMN proposed_award_reason VARCHAR(500) NULL,
  ADD COLUMN awarded_supplier_id BIGINT NULL,
  ADD COLUMN award_reason VARCHAR(500) NULL,
  ADD CONSTRAINT fk_sourcing_awarded_supplier FOREIGN KEY (awarded_supplier_id) REFERENCES srm_supplier(id);
DROP INDEX event_no ON srm_sourcing_event;
CREATE UNIQUE INDEX uk_sourcing_org_no ON srm_sourcing_event(organization_code, event_no);
CREATE INDEX idx_sourcing_org_status_deadline ON srm_sourcing_event(organization_code, status, deadline);

ALTER TABLE srm_quote
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN quote_version INT NOT NULL DEFAULT 1,
  ADD COLUMN tax_rate DECIMAL(5,2) NOT NULL DEFAULT 13.00,
  MODIFY COLUMN remark VARCHAR(500) NULL,
  ADD COLUMN attachment_key VARCHAR(64) NULL,
  ADD COLUMN evaluation_score DECIMAL(6,2) NULL;
CREATE UNIQUE INDEX uk_quote_event_supplier_version ON srm_quote(event_id, supplier_id, quote_version);

ALTER TABLE srm_purchase_order
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN organization_code VARCHAR(32) NOT NULL DEFAULT 'DEFAULT',
  ADD COLUMN procurement_organization VARCHAR(32) NOT NULL DEFAULT 'DEFAULT',
  ADD COLUMN factory_code VARCHAR(32) NOT NULL DEFAULT 'DEFAULT',
  ADD COLUMN currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
  ADD COLUMN order_version INT NOT NULL DEFAULT 1,
  ADD COLUMN change_reason VARCHAR(500) NULL,
  ADD COLUMN abnormal BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN abnormal_reason VARCHAR(500) NULL,
  ADD COLUMN confirmed_at DATETIME(6) NULL,
  MODIFY COLUMN status VARCHAR(32) NOT NULL;
DROP INDEX order_no ON srm_purchase_order;
CREATE UNIQUE INDEX uk_order_org_no ON srm_purchase_order(organization_code, order_no);
CREATE INDEX idx_order_org_supplier_status ON srm_purchase_order(organization_code, supplier_id, status);

ALTER TABLE srm_quality_issue
  ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
  ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE,
  ADD COLUMN organization_code VARCHAR(32) NOT NULL DEFAULT 'DEFAULT',
  ADD COLUMN issue_type VARCHAR(8) NOT NULL DEFAULT 'NCR',
  ADD COLUMN root_cause VARCHAR(2000) NULL,
  ADD COLUMN interim_action VARCHAR(2000) NULL,
  ADD COLUMN permanent_action VARCHAR(2000) NULL,
  ADD COLUMN verification_result VARCHAR(2000) NULL,
  ADD COLUMN return_reason VARCHAR(500) NULL,
  ADD COLUMN evidence_file_key VARCHAR(64) NULL,
  ADD COLUMN submitted_at DATETIME(6) NULL,
  ADD COLUMN closed_at DATETIME(6) NULL;
DROP INDEX issue_no ON srm_quality_issue;
CREATE UNIQUE INDEX uk_quality_org_no ON srm_quality_issue(organization_code, issue_no);
CREATE INDEX idx_quality_org_supplier_status ON srm_quality_issue(organization_code, supplier_id, status);

CREATE TABLE srm_audit_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  organization_code VARCHAR(32) NOT NULL,
  actor VARCHAR(64) NOT NULL,
  module VARCHAR(40) NOT NULL,
  action VARCHAR(40) NOT NULL,
  object_type VARCHAR(60) NOT NULL,
  object_id VARCHAR(80) NULL,
  before_content TEXT NULL,
  after_content TEXT NULL,
  client_ip VARCHAR(64) NULL,
  occurred_at DATETIME(6) NOT NULL
);
CREATE INDEX idx_audit_org_time ON srm_audit_log(organization_code, occurred_at);
CREATE INDEX idx_audit_actor_module ON srm_audit_log(actor, module);

CREATE TABLE srm_organization (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  organization_code VARCHAR(32) NOT NULL UNIQUE, organization_name VARCHAR(120) NOT NULL, enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL
);

CREATE TABLE srm_role_permission (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  organization_code VARCHAR(32) NOT NULL, role VARCHAR(20) NOT NULL, permission_code VARCHAR(64) NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT TRUE, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL
);
CREATE INDEX idx_role_permission_scope ON srm_role_permission(organization_code, role, enabled);

CREATE TABLE srm_inbox_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  organization_code VARCHAR(32) NOT NULL, recipient_username VARCHAR(64) NOT NULL, supplier_id BIGINT NULL,
  kind VARCHAR(16) NOT NULL, status VARCHAR(16) NOT NULL, title VARCHAR(160) NOT NULL, content VARCHAR(1000) NULL,
  business_type VARCHAR(80) NULL, business_id VARCHAR(80) NULL, link VARCHAR(300) NULL,
  due_at DATETIME(6) NULL, handled_at DATETIME(6) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_inbox_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id)
);
CREATE INDEX idx_inbox_recipient_status ON srm_inbox_item(recipient_username, status, due_at);

CREATE TABLE srm_supplier_invitation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  organization_code VARCHAR(32) NOT NULL, token VARCHAR(64) NOT NULL UNIQUE, supplier_name VARCHAR(120) NOT NULL,
  email VARCHAR(120) NOT NULL, contact_name VARCHAR(40) NULL, status VARCHAR(16) NOT NULL,
  expires_at DATETIME(6) NOT NULL, accepted_at DATETIME(6) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL
);
CREATE TABLE srm_supplier_contact (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  supplier_id BIGINT NOT NULL, name VARCHAR(50) NOT NULL, position_name VARCHAR(60) NULL, phone VARCHAR(30) NULL,
  email VARCHAR(120) NULL, primary_contact BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_contact_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id)
);
CREATE TABLE srm_supplier_certificate (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  supplier_id BIGINT NOT NULL, certificate_type VARCHAR(40) NOT NULL, certificate_no VARCHAR(80) NOT NULL,
  valid_from DATE NOT NULL, expires_on DATE NOT NULL, file_key VARCHAR(64) NULL, verified BOOLEAN NOT NULL DEFAULT FALSE,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_certificate_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id)
);
CREATE INDEX idx_certificate_supplier_expiry ON srm_supplier_certificate(supplier_id, expires_on);
CREATE TABLE srm_supplier_onboarding (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  supplier_id BIGINT NOT NULL, questionnaire_json TEXT NOT NULL, status VARCHAR(20) NOT NULL,
  first_approver VARCHAR(64) NULL, final_approver VARCHAR(64) NULL, approval_comment VARCHAR(500) NULL,
  submitted_at DATETIME(6) NULL, approved_at DATETIME(6) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_onboarding_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id)
);
CREATE TABLE srm_supplier_change_record (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  supplier_id BIGINT NOT NULL, changed_by VARCHAR(64) NOT NULL, field_name VARCHAR(60) NOT NULL,
  before_value TEXT NULL, after_value TEXT NULL, reason VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_supplier_change_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id)
);

CREATE TABLE srm_sourcing_item (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  event_id BIGINT NOT NULL, line_no INT NOT NULL, material_code VARCHAR(64) NOT NULL, material_name VARCHAR(160) NOT NULL,
  specification VARCHAR(300) NULL, quantity DECIMAL(16,4) NOT NULL, unit VARCHAR(16) NOT NULL, required_date DATE NOT NULL,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_sourcing_item_event FOREIGN KEY (event_id) REFERENCES srm_sourcing_event(id)
);
CREATE TABLE srm_sourcing_invitation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  event_id BIGINT NOT NULL, supplier_id BIGINT NOT NULL, invited_at DATETIME(6) NOT NULL, viewed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_sourcing_invitation_event FOREIGN KEY (event_id) REFERENCES srm_sourcing_event(id),
  CONSTRAINT fk_sourcing_invitation_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id),
  CONSTRAINT uk_sourcing_invitation UNIQUE (event_id, supplier_id)
);
CREATE TABLE srm_sourcing_clarification (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  event_id BIGINT NOT NULL, supplier_id BIGINT NOT NULL, question VARCHAR(1000) NOT NULL, answer VARCHAR(2000) NULL,
  asked_by VARCHAR(64) NOT NULL, answered_by VARCHAR(64) NULL, answered_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_clarification_event FOREIGN KEY (event_id) REFERENCES srm_sourcing_event(id),
  CONSTRAINT fk_clarification_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id)
);

CREATE TABLE srm_contract (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  organization_code VARCHAR(32) NOT NULL, contract_no VARCHAR(40) NOT NULL, contract_version INT NOT NULL,
  supplier_id BIGINT NOT NULL, title VARCHAR(160) NOT NULL, amount DECIMAL(16,2) NOT NULL, currency VARCHAR(8) NOT NULL,
  effective_from DATE NOT NULL, effective_to DATE NOT NULL, attachment_key VARCHAR(64) NULL, status VARCHAR(24) NOT NULL,
  change_reason VARCHAR(500) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_contract_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id),
  CONSTRAINT uk_contract_org_no_version UNIQUE (organization_code, contract_no, contract_version)
);
CREATE INDEX idx_contract_org_expiry ON srm_contract(organization_code, effective_to, status);

CREATE TABLE srm_purchase_order_line (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  order_id BIGINT NOT NULL, line_no INT NOT NULL, material_code VARCHAR(64) NOT NULL, material_name VARCHAR(160) NOT NULL,
  specification VARCHAR(300) NULL, ordered_quantity DECIMAL(16,4) NOT NULL, unit_price DECIMAL(16,4) NOT NULL,
  unit VARCHAR(16) NOT NULL, requested_date DATE NOT NULL, confirmed_quantity DECIMAL(16,4) NULL,
  confirmed_date DATE NULL, received_quantity DECIMAL(16,4) NOT NULL DEFAULT 0, supplier_remark VARCHAR(500) NULL,
  confirm_status VARCHAR(16) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_order_line_order FOREIGN KEY (order_id) REFERENCES srm_purchase_order(id),
  CONSTRAINT uk_order_line UNIQUE (order_id, line_no)
);

CREATE TABLE srm_purchase_order_version (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  order_id BIGINT NOT NULL, order_version INT NOT NULL, snapshot_json TEXT NOT NULL,
  change_reason VARCHAR(500) NOT NULL, changed_by VARCHAR(64) NOT NULL,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_order_version_order FOREIGN KEY (order_id) REFERENCES srm_purchase_order(id)
);

CREATE TABLE srm_quota_plan (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  organization_code VARCHAR(32) NOT NULL, procurement_organization VARCHAR(32) NOT NULL,
  factory_code VARCHAR(32) NOT NULL, material_code VARCHAR(64) NOT NULL, material_name VARCHAR(160) NOT NULL,
  quota_type VARCHAR(16) NOT NULL, total_quantity DECIMAL(16,4) NOT NULL, valid_from DATE NOT NULL, valid_to DATE NOT NULL,
  plan_version INT NOT NULL, status VARCHAR(16) NOT NULL, change_reason VARCHAR(500) NULL,
  activated_by VARCHAR(64) NULL, activated_at DATETIME(6) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT uk_quota_scope_version UNIQUE (organization_code, procurement_organization, factory_code, material_code, valid_from, valid_to, plan_version)
);
CREATE INDEX idx_quota_scope_status ON srm_quota_plan(organization_code, procurement_organization, factory_code, material_code, status);
CREATE TABLE srm_quota_allocation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  plan_id BIGINT NOT NULL, supplier_id BIGINT NOT NULL, quota_value DECIMAL(16,4) NOT NULL,
  allocated_quantity DECIMAL(16,4) NOT NULL, used_quantity DECIMAL(16,4) NOT NULL DEFAULT 0,
  adjustment_note VARCHAR(500) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_quota_allocation_plan FOREIGN KEY (plan_id) REFERENCES srm_quota_plan(id),
  CONSTRAINT fk_quota_allocation_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id),
  CONSTRAINT uk_quota_plan_supplier UNIQUE (plan_id, supplier_id)
);
CREATE TABLE srm_quota_usage (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  allocation_id BIGINT NOT NULL, source_type VARCHAR(24) NOT NULL, source_id VARCHAR(80) NOT NULL,
  quantity DECIMAL(16,4) NOT NULL, operated_by VARCHAR(64) NOT NULL, remark VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_quota_usage_allocation FOREIGN KEY (allocation_id) REFERENCES srm_quota_allocation(id),
  CONSTRAINT uk_quota_usage_source UNIQUE (allocation_id, source_type, source_id)
);

CREATE TABLE srm_asn (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  organization_code VARCHAR(32) NOT NULL, asn_no VARCHAR(40) NOT NULL, supplier_id BIGINT NOT NULL, order_id BIGINT NOT NULL,
  planned_arrival_at DATETIME(6) NOT NULL, appointment_at DATETIME(6) NULL, shipped_at DATETIME(6) NULL, received_at DATETIME(6) NULL,
  carrier_name VARCHAR(80) NULL, tracking_no VARCHAR(80) NULL, discrepancy_reason VARCHAR(500) NULL, status VARCHAR(24) NOT NULL,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_asn_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id),
  CONSTRAINT fk_asn_order FOREIGN KEY (order_id) REFERENCES srm_purchase_order(id)
);
CREATE INDEX idx_asn_org_supplier_status ON srm_asn(organization_code, supplier_id, status);
CREATE TABLE srm_asn_line (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  asn_id BIGINT NOT NULL, order_line_id BIGINT NOT NULL, shipped_quantity DECIMAL(16,4) NOT NULL,
  received_quantity DECIMAL(16,4) NOT NULL DEFAULT 0, discrepancy_reason VARCHAR(500) NULL,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_asn_line_asn FOREIGN KEY (asn_id) REFERENCES srm_asn(id),
  CONSTRAINT fk_asn_line_order_line FOREIGN KEY (order_line_id) REFERENCES srm_purchase_order_line(id)
);

CREATE TABLE srm_reconciliation (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  organization_code VARCHAR(32) NOT NULL, statement_no VARCHAR(40) NOT NULL, supplier_id BIGINT NOT NULL,
  period_from DATE NOT NULL, period_to DATE NOT NULL, order_amount DECIMAL(16,2) NOT NULL,
  received_amount DECIMAL(16,2) NOT NULL, settlement_amount DECIMAL(16,2) NOT NULL,
  status VARCHAR(24) NOT NULL, dispute_reason VARCHAR(500) NULL, supplier_confirmed_at DATETIME(6) NULL,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_reconciliation_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id)
);
CREATE TABLE srm_invoice (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  statement_id BIGINT NOT NULL, invoice_no VARCHAR(64) NOT NULL, amount DECIMAL(16,2) NOT NULL,
  invoice_date DATE NOT NULL, attachment_key VARCHAR(64) NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_invoice_statement FOREIGN KEY (statement_id) REFERENCES srm_reconciliation(id)
);
CREATE TABLE srm_supplier_performance (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  organization_code VARCHAR(32) NOT NULL, supplier_id BIGINT NOT NULL, period_from DATE NOT NULL, period_to DATE NOT NULL,
  quality_score DECIMAL(5,2) NOT NULL, delivery_score DECIMAL(5,2) NOT NULL, cost_score DECIMAL(5,2) NOT NULL,
  service_score DECIMAL(5,2) NOT NULL, total_score DECIMAL(5,2) NOT NULL, grade VARCHAR(24) NOT NULL,
  corrective_action_required BOOLEAN NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_performance_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id)
);
CREATE TABLE srm_risk_alert (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  organization_code VARCHAR(32) NOT NULL, supplier_id BIGINT NOT NULL, rule_code VARCHAR(40) NOT NULL,
  risk_level VARCHAR(16) NOT NULL, description VARCHAR(500) NOT NULL, owner_username VARCHAR(64) NULL,
  corrective_action VARCHAR(1000) NULL, due_at DATETIME(6) NULL, status VARCHAR(24) NOT NULL,
  created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_risk_alert_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id)
);

CREATE TABLE srm_file_asset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT, version BIGINT NOT NULL DEFAULT 0, deleted BOOLEAN NOT NULL DEFAULT FALSE,
  file_key VARCHAR(64) NOT NULL UNIQUE, organization_code VARCHAR(32) NOT NULL, supplier_id BIGINT NULL,
  original_name VARCHAR(255) NOT NULL, content_type VARCHAR(120) NOT NULL, size_bytes BIGINT NOT NULL,
  sha256 VARCHAR(64) NOT NULL, storage_path VARCHAR(500) NOT NULL, business_type VARCHAR(80) NOT NULL,
  business_id VARCHAR(80) NULL, uploaded_by VARCHAR(64) NOT NULL, created_at DATETIME(6) NOT NULL, updated_at DATETIME(6) NOT NULL,
  CONSTRAINT fk_file_supplier FOREIGN KEY (supplier_id) REFERENCES srm_supplier(id)
);
CREATE INDEX idx_file_business ON srm_file_asset(organization_code, business_type, business_id);
