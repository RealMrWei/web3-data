#!/bin/bash

# ============================================
# 合约地址配置脚本
# 功能：从 Hardhat 部署结果中提取合约地址并更新 Spring Boot 配置
# ============================================

echo "🔍 正在读取 Hardhat 部署结果..."

# 检查配置文件是否存在
ASSET_CONTRACTS_FILE="../../config/asset-contracts.json"
if [ ! -f "$ASSET_CONTRACTS_FILE" ]; then
    echo "❌ 错误: 未找到 $ASSET_CONTRACTS_FILE"
    echo "💡 请先运行部署脚本: npx hardhat run scripts/deploy-assets.js --network localhost"
    exit 1
fi

# 使用 jq 提取合约地址（如果没有 jq，使用 python 替代）
if command -v jq &> /dev/null; then
    ASSET_TOKEN=$(jq -r '.contracts.assetToken' "$ASSET_CONTRACTS_FILE")
    DEPOSIT_VAULT=$(jq -r '.contracts.depositVault' "$ASSET_CONTRACTS_FILE")
    WITHDRAWAL_MANAGER=$(jq -r '.contracts.withdrawalManager' "$ASSET_CONTRACTS_FILE")
else
    # 使用 Python 解析 JSON
    ASSET_TOKEN=$(python3 -c "import json; data=json.load(open('$ASSET_CONTRACTS_FILE')); print(data['contracts']['assetToken'])")
    DEPOSIT_VAULT=$(python3 -c "import json; data=json.load(open('$ASSET_CONTRACTS_FILE')); print(data['contracts']['depositVault'])")
    WITHDRAWAL_MANAGER=$(python3 -c "import json; data=json.load(open('$ASSET_CONTRACTS_FILE')); print(data['contracts']['withdrawalManager'])")
fi

echo "✅ 合约地址提取成功:"
echo "   AssetToken:        $ASSET_TOKEN"
echo "   DepositVault:      $DEPOSIT_VAULT"
echo "   WithdrawalManager: $WITHDRAWAL_MANAGER"
echo ""

# 更新 application.yml 中的合约地址
APPLICATION_YML="src/main/resources/application.yml"

echo "📝 正在更新 $APPLICATION_YML ..."

# 使用 sed 替换合约地址（Windows 下可能需要调整）
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" ]]; then
    # Windows Git Bash
    sed -i "s|asset-token:.*|asset-token: $ASSET_TOKEN|" "$APPLICATION_YML"
    sed -i "s|deposit-vault:.*|deposit-vault: $DEPOSIT_VAULT|" "$APPLICATION_YML"
    sed -i "s|withdrawal-manager:.*|withdrawal-manager: $WITHDRAWAL_MANAGER|" "$APPLICATION_YML"
else
    # Linux/Mac
    sed -i '' "s|asset-token:.*|asset-token: $ASSET_TOKEN|" "$APPLICATION_YML"
    sed -i '' "s|deposit-vault:.*|deposit-vault: $DEPOSIT_VAULT|" "$APPLICATION_YML"
    sed -i '' "s|withdrawal-manager:.*|withdrawal-manager: $WITHDRAWAL_MANAGER|" "$APPLICATION_YML"
fi

echo "✅ 配置文件更新完成！"
echo ""
echo "🚀 下一步："
echo "   1. 检查配置文件: cat $APPLICATION_YML"
echo "   2. 启动应用: mvn spring-boot:run"
echo ""
