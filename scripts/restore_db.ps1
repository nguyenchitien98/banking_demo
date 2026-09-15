# Script khôi phục CSDL PostgreSQL từ bản sao lưu cho Titan BankX (Local Sandbox)
Param (
    [Parameter(Mandatory=$true)]
    [string]$File
)

Write-Host "=========================================================" -ForegroundColor Cyan
Write-Host "   🚀 TITAN BANKX — LOCAL DATABASE RESTORE UTILITY" -ForegroundColor Cyan
Write-Host "=========================================================" -ForegroundColor Cyan

if (!(Test-Path $File)) {
    Write-Host "❌ File sao lưu không tồn tại: $File" -ForegroundColor Red
    exit 1
}

Write-Host "Đang khôi phục PostgreSQL từ $File vào container bankx-postgres..." -ForegroundColor Yellow

Get-Content $File | docker exec -i bankx-postgres psql -U bankx_user -d bankx_db

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Khôi phục PostgreSQL CSDL bankx_db thành công!" -ForegroundColor Green
} else {
    Write-Host "❌ Lỗi trong quá trình khôi phục CSDL PostgreSQL" -ForegroundColor Red
}

Write-Host "=========================================================" -ForegroundColor Cyan
