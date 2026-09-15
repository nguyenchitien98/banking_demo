# Script sao lưu CSDL PostgreSQL & Redis snapshot cho Titan BankX (Local Sandbox)
Param (
    [string]$BackupDir = ".\backups"
)

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "   🚀 TITAN BANKX — LOCAL DATABASE BACKUP UTILITY" -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan

# Tạo thư mục lưu trữ bản sao lưu nếu chưa tồn tại
if (!(Test-Path $BackupDir)) {
    New-Item -ItemType Directory -Path $BackupDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$pgBackupFile = "$BackupDir\bankx_db_$timestamp.sql"

# 1. Sao lưu PostgreSQL Database qua Docker Container
Write-Host "[1/2] Đang sao lưu PostgreSQL CSDL bankx_db..." -ForegroundColor Yellow
docker exec -t bankx-postgres pg_dump -U bankx_user -d bankx_db > $pgBackupFile

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ PostgreSQL Dump thành công: $pgBackupFile" -ForegroundColor Green
} else {
    Write-Host "⚠️ Lỗi sao lưu PostgreSQL qua Docker container bankx-postgres" -ForegroundColor Red
}

# 2. Trigger Redis BGSAVE snapshot
Write-Host "[2/2] Đang kích hoạt Redis BGSAVE snapshot..." -ForegroundColor Yellow
docker exec -t bankx-redis redis-cli SAVE

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Redis snapshot SAVE thành công!" -ForegroundColor Green
} else {
    Write-Host "⚠️ Lỗi trigger Redis SAVE" -ForegroundColor Red
}

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host " 🎉 HOÀN THÀNH QUÁ TRÌNH SAO LƯU LOCAL DỮ LIỆU!" -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan
