# ==============================================================================
# BankX — Quick Start Scripts
# Chạy từ thư mục banking/
# ==============================================================================

# Khởi động toàn bộ infrastructure (PostgreSQL, Redis, Kafka, Grafana, Jaeger...)
infra-up:
	docker-compose -f infrastructure/docker-compose.yml up -d

# Dừng infrastructure (giữ lại data volumes)
infra-down:
	docker-compose -f infrastructure/docker-compose.yml down

# Dừng và XÓA toàn bộ data volumes (reset hoàn toàn)
infra-reset:
	docker-compose -f infrastructure/docker-compose.yml down -v
	@echo "All volumes deleted. Fresh start!"

# Xem logs của các services
infra-logs:
	docker-compose -f infrastructure/docker-compose.yml logs -f

# Khởi động pgAdmin (dev only)
infra-dev:
	docker-compose -f infrastructure/docker-compose.yml --profile dev up -d

# Kiểm tra status các containers
infra-status:
	docker-compose -f infrastructure/docker-compose.yml ps

# ==============================================================================
# Backend
# ==============================================================================

# Build toàn bộ backend (skip test cho nhanh)
backend-build:
	cd backend && mvn clean package -DskipTests

# Build + Run tests
backend-test:
	cd backend && mvn clean test

# Khởi động API Gateway
gateway-run:
	cd backend && mvn spring-boot:run -pl bankx-api-gateway

# Khởi động Banking Core
core-run:
	cd backend && mvn spring-boot:run -pl bankx-banking-core

# ==============================================================================
# Frontend
# ==============================================================================

# Cài dependencies
frontend-install:
	cd frontend-web && npm install

# Chạy Angular dev server
frontend-run:
	cd frontend-web && npm start

# Build production
frontend-build:
	cd frontend-web && npm run build

# ==============================================================================
# Tiện ích
# ==============================================================================

# Mở tất cả URLs trong browser
open-urls:
	start http://localhost:4200      # Angular App
	start http://localhost:8080      # Kafka UI (AKHQ)
	start http://localhost:3000      # Grafana
	start http://localhost:16686     # Jaeger
	start http://localhost:9090      # Prometheus
	start http://localhost:5050      # pgAdmin
	start http://localhost:8091/swagger-ui.html  # Swagger API Docs
