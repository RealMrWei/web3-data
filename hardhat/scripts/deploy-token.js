const hre = require("hardhat");

async function main() {
  console.log("🚀 开始部署 AssetToken 合约...\n");

  const [deployer] = await hre.ethers.getSigners();
  console.log("部署账户:", deployer.address);

  // 部署 AssetToken（使用代理模式）
  console.log("\n📦 部署 AssetToken...");
  const AssetToken = await hre.ethers.getContractFactory("AssetToken");
  
  // 使用 upgrades.deployProxy 部署（会自动调用 initialize）
  const token = await hre.upgrades.deployProxy(AssetToken, 
    ["Asset Token", "ATK", hre.ethers.utils.parseEther("10000")], // name, symbol, initialSupply
    { 
      initializer: 'initialize',
      unsafeAllow: ['constructor']
    }
  );
  
  await token.deployed();
  console.log("✅ AssetToken 代理地址:", token.address);

  // 保存地址到配置文件
  const fs = require("fs");
  const path = require("path");
  
  const configPath = path.join(__dirname, "../config/address.json");
  let config = {};
  
  if (fs.existsSync(configPath)) {
    config = JSON.parse(fs.readFileSync(configPath, "utf8"));
  }
  
  config.token = token.address;
  
  fs.writeFileSync(configPath, JSON.stringify(config, null, 2));
  console.log("\n✅ 地址已保存到 config/address.json");

  console.log("\n📊 部署总结:");
  console.log(JSON.stringify({
    token: token.address
  }, null, 2));
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
