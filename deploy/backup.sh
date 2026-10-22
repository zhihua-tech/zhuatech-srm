#!/usr/bin/env bash
# Copyright 2026 上海如静知华信息科技有限公司 · https://www.zhuatech.cn/
set -euo pipefail

backup_dir="${1:-./backups}"
database_name="${MYSQL_DATABASE:?MYSQL_DATABASE is required}"
database_user="${MYSQL_USER:?MYSQL_USER is required}"
database_password="${MYSQL_PASSWORD:?MYSQL_PASSWORD is required}"
timestamp="$(date +%Y%m%d-%H%M%S)"
mkdir -p "${backup_dir}"
umask 077
docker compose exec -T -e MYSQL_PWD="${database_password}" mysql \
  mysqldump --single-transaction --routines --triggers --no-tablespaces \
  -u"${database_user}" "${database_name}" | gzip > "${backup_dir}/srm-${timestamp}.sql.gz"
test -s "${backup_dir}/srm-${timestamp}.sql.gz"
printf '%s\n' "Backup created: ${backup_dir}/srm-${timestamp}.sql.gz"
