# ZhuaTech Enterprise Supplier Relationship Management

[简体中文](README.md) | **English**

ZhuaTech SRM is a self-hosted, source-available supplier relationship management system from [ZhuaTech](https://www.zhuatech.cn/), operated by Shanghai Rujing Zhihua Information Technology Co., Ltd. It provides a responsive procurement workspace and a supplier portal backed by the same secured business APIs.

## Capabilities

- Supplier invitation, registration, profiles, contacts, bank details, certificates, onboarding questionnaire and approvals
- RFQ/RFP, targeted invitations, quote versions, clarifications, comparison and award notification
- Contract ledger, versions, approvals, attachments and expiry reminders
- Purchase order API and atomic Excel import, line confirmation, delivery feedback, change versions and exceptions
- Ratio and quantity quotas, multi-supplier validation, activation history, consumption, balances, alerts and Excel export
- ASN, delivery appointments, shipping, receiving and discrepancies
- NCR/CAR, root-cause analysis, corrective actions, evidence, return, verification and closure
- Reconciliation, invoice registration, supplier performance, rule-based risk alerts and corrective tracking
- Server-side authorization, organization/supplier isolation, audit logs, optimistic locking, soft deletion, controlled files and health checks

## Runtime

Java 21 · Spring Boot 4 · Vue 3 · MySQL 8.4 · Flyway · Docker Compose

```bash
cp .env.example .env
# Configure unique random secrets and the one-time bootstrap administrator.
docker compose up -d --build
```

There are no built-in demonstration accounts, passwords or production bypasses. Follow [the deployment guide](deploy/README.md) for secure bootstrap, HTTPS, backup and restore.

## Documentation

- [Architecture](docs/architecture.md)
- [REST API](docs/api.md)
- [Database](docs/database.md)
- [User manual](docs/USER_MANUAL.md)
- [Test results](docs/TEST_RESULTS.md)
- [Known limitations](docs/KNOWN_LIMITATIONS.md)

## Verification

```bash
cd backend && mvn test
cd frontend && npm ci && npm run build
```

## Usage and Commercial Licensing

This project is provided solely for personal learning, technical research and non-commercial communication. Commercial use, internal production deployment, paid delivery, resale, hosted services and commercial derivatives require prior written authorization from Shanghai Rujing Zhihua Information Technology Co., Ltd. See [LICENSE](LICENSE).

For commercial licensing, private deployment, implementation, customization and integration:

- [han@zhuatech.cn](mailto:han@zhuatech.cn)
- [jack@zhuatech.cn](mailto:jack@zhuatech.cn)
- [WhatsApp: +86 17521234993](https://wa.me/8617521234993)
- [https://www.zhuatech.cn/](https://www.zhuatech.cn/)

Copyright © 2026 Shanghai Rujing Zhihua Information Technology Co., Ltd. All rights reserved.
