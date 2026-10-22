#!/usr/bin/env bash
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/
set -euo pipefail

backup_file="${1:?Usage: restore.sh <backup.sql.gz>}"
database_name="${MYSQL_DATABASE:?MYSQL_DATABASE is required}"
database_user="${MYSQL_USER:?MYSQL_USER is required}"
database_password="${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
test -f "${backup_file}"
gzip -t "${backup_file}"
gzip -dc "${backup_file}" | docker compose exec -T -e MYSQL_PWD="${database_password}" mysql \
  mysql -u"${database_user}" "${database_name}"
printf '%s\n' "Restore completed. Restart the backend and run the smoke tests."
