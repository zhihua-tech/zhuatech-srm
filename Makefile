# Copyright 2026 上海如静知华信息科技有限公司
.PHONY: dev test build up down

dev:
	cd frontend && npm run dev

test:
	cd backend && mvn test

build:
	cd backend && mvn package
	cd frontend && npm run build

up:
	docker compose up --build -d

down:
	docker compose down
