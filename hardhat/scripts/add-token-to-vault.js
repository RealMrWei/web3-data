const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  console.log(" 添加代币到 DepositVault 支持列表\n");

  // 优先使用环境变量，否则从配置文件读取
  let TOKEN_ADDRESS = process.env.TOKEN_ADDRESS;
  let VAULT_ADDRESS = process.env.VAULT_ADDRESS;
  
  if (!TOKEN_ADDRESS || !VAULT_ADDRESS) {
    console.log("⚠️  未设置环境变量，从配置文件读取...\n");
    const configPath = path.join(__dirname, "../config/address.json");
    const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
    
    TOKEN_ADDRESS = TOKEN_ADDRESS || config.token;
    VAULT_ADDRESS = VAULT_ADDRESS || config.depositVault;
  }
  
  console.log("Token:", TOKEN_ADDRESS);
  console.log("Vault:", VAULT_ADDRESS);
  console.log();

  const [deployer] = await hre.ethers.getSigners();
  const vault = await hre.ethers.getContractAt("DepositVault", VAULT_ADDRESS);

  // 检查是否已添加
  const tokens = await vault.getSupportedTokens();
  if (tokens.includes(TOKEN_ADDRESS)) {
    console.log("✅ 代币已在支持列表中");
    return;
  }

  console.log("⚠️ 代币不在列表中，正在添加...");
  const tx = await vault.addSupportedToken(TOKEN_ADDRESS);
  await tx.wait();
  console.log("✅ 代币添加成功!");
  console.log("TxHash:", tx.hash);
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
