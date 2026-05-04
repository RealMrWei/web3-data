const { ethers, upgrades } = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  console.log("🚀 开始部署资产管理合约...");

  const [deployer] = await ethers.getSigners();
  console.log("部署账户:", deployer.address);

  // 读取已有地址（如果存在）
  const configPath = path.join(__dirname, "../../config/address.json");
  let addresses = {};
  if (fs.existsSync(configPath)) {
    addresses = JSON.parse(fs.readFileSync(configPath, "utf8"));
  }

  // 1. 部署 DepositVault（充值金库）
  console.log("\n 部署 DepositVault...");
  const DepositVault = await ethers.getContractFactory("DepositVault");
  const depositVaultProxy = await upgrades.deployProxy(DepositVault, [], {
    kind: "uups",
    unsafeAllow: ['constructor'],
  });
  await depositVaultProxy.deployed();
  console.log("✅ DepositVault 代理地址 =", depositVaultProxy.address);
  addresses.depositVault = depositVaultProxy.address;

  // 2. 部署 WithdrawalManager（提现管理）
  console.log("\n📦 部署 WithdrawalManager...");
  const WithdrawalManager = await ethers.getContractFactory("WithdrawalManager");
  const withdrawalManagerProxy = await upgrades.deployProxy(WithdrawalManager, [], {
    kind: "uups",
    unsafeAllow: ['constructor'],
  });
  await withdrawalManagerProxy.deployed();
  console.log("✅ WithdrawalManager 代理地址 =", withdrawalManagerProxy.address);
  addresses.withdrawalManager = withdrawalManagerProxy.address;

  // 3. 授权角色（给部署者授予 OPERATOR_ROLE）
  console.log("\n🔑 配置角色权限...");
  
  // DepositVault 授权
  const OPERATOR_ROLE = await depositVaultProxy.OPERATOR_ROLE();
  await depositVaultProxy.grantRole(OPERATOR_ROLE, deployer.address);
  console.log("✅ DepositVault OPERATOR_ROLE 已授权给:", deployer.address);

  // WithdrawalManager 授权
  const withdrawalOperatorRole = await withdrawalManagerProxy.OPERATOR_ROLE();
  const withdrawalApproverRole = await withdrawalManagerProxy.APPROVER_ROLE();
  await withdrawalManagerProxy.grantRole(withdrawalOperatorRole, deployer.address);
  await withdrawalManagerProxy.grantRole(withdrawalApproverRole, deployer.address);
  console.log("✅ WithdrawalManager OPERATOR_ROLE 和 APPROVER_ROLE 已授权给:", deployer.address);

  // 保存地址到配置文件
  const configDir = path.join(__dirname, "../../config");
  if (!fs.existsSync(configDir)) fs.mkdirSync(configDir, { recursive: true });

  fs.writeFileSync(configPath, JSON.stringify(addresses, null, 2));
  console.log("\n✅ 地址已保存到 config/address.json");

  console.log("\n📊 部署总结:");
  console.log(JSON.stringify(addresses, null, 2));
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error(err);
    process.exit(1);
  });
