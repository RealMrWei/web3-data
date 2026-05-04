const { ethers, upgrades } = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  console.log("🚀 开始部署 V1 合约...");

  const UserPointsV1 = await ethers.getContractFactory("UserPointsV1");
  
  // UUPS 代理部署（生产标准）
  const proxy = await upgrades.deployProxy(UserPointsV1, [], {
    kind: "uups",
    unsafeAllow: ['constructor'],  // 允许构造函数（仅用于 _disableInitializers）
  });

  await proxy.deployed();
  console.log("✅ 代理地址(固定不变) =", proxy.address);

  // 保存地址到上层 config 给 Java 使用
  const configDir = path.join(__dirname, "../../config");
  if (!fs.existsSync(configDir)) fs.mkdirSync(configDir);

  const addr = {
    proxy: proxy.address,
  };

  fs.writeFileSync(
    path.join(configDir, "address.json"),
    JSON.stringify(addr, null, 2)
  );

  console.log("✅ 地址已保存到 ../config/address.json");
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });