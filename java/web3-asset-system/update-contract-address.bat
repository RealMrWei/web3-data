@echo off
REM ============================================
REM 合约地址配置脚本 (Windows 版本)
REM 功能：从 Hardhat 部署结果中提取合约地址并更新 Spring Boot 配置
REM ============================================

echo.
echo 🔍 正在读取 Hardhat 部署结果...
echo.

REM 检查配置文件是否存在
set ASSET_CONTRACTS_FILE=..\..\config\asset-contracts.json
if not exist "%ASSET_CONTRACTS_FILE%" (
    echo ❌ 错误: 未找到 %ASSET_CONTRACTS_FILE%
    echo 💡 请先运行部署脚本: npx hardhat run scripts/deploy-assets.js --network localhost
    pause
    exit /b 1
)

REM 使用 PowerShell 解析 JSON 提取合约地址
for /f "delims=" %%i in ('powershell -Command "(Get-Content '%ASSET_CONTRACTS_FILE%' | ConvertFrom-Json).contracts.assetToken"') do set ASSET_TOKEN=%%i
for /f "delims=" %%i in ('powershell -Command "(Get-Content '%ASSET_CONTRACTS_FILE%' | ConvertFrom-Json).contracts.depositVault"') do set DEPOSIT_VAULT=%%i
for /f "delims=" %%i in ('powershell -Command "(Get-Content '%ASSET_CONTRACTS_FILE%' | ConvertFrom-Json).contracts.withdrawalManager"') do set WITHDRAWAL_MANAGER=%%i

echo ✅ 合约地址提取成功:
echo    AssetToken:        %ASSET_TOKEN%
echo    DepositVault:      %DEPOSIT_VAULT%
echo    WithdrawalManager: %WITHDRAWAL_MANAGER%
echo.

REM 更新 application.yml 中的合约地址
set APPLICATION_YML=src\main\resources\application.yml

echo 📝 正在更新 %APPLICATION_YML% ...

REM 使用 PowerShell 替换合约地址
powershell -Command "(Get-Content '%APPLICATION_YML%') -replace 'asset-token:.*', 'asset-token: %ASSET_TOKEN%' | Set-Content '%APPLICATION_YML%'"
powershell -Command "(Get-Content '%APPLICATION_YML%') -replace 'deposit-vault:.*', 'deposit-vault: %DEPOSIT_VAULT%' | Set-Content '%APPLICATION_YML%'"
powershell -Command "(Get-Content '%APPLICATION_YML%') -replace 'withdrawal-manager:.*', 'withdrawal-manager: %WITHDRAWAL_MANAGER%' | Set-Content '%APPLICATION_YML%'"

echo ✅ 配置文件更新完成！
echo.
echo 🚀 下一步：
echo    1. 检查配置文件: type %APPLICATION_YML%
echo    2. 启动应用: mvn spring-boot:run
echo.
pause
