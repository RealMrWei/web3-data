const { ethers, upgrades } = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  console.log("🧪 开始测试 UserPointsV1 合约...\n");

  // 获取签名者（Sepolia 测试网通常只有一个配置的账户）
  const [deployer] = await ethers.getSigners();
  
  // 创建测试用的随机地址（仅用于演示，实际测试需要使用真实地址）
  const operator = deployer; // 使用部署者作为操作员
  const user1 = ethers.Wallet.createRandom().connect(ethers.provider);
  const user2 = ethers.Wallet.createRandom().connect(ethers.provider);
  
  console.log("📋 测试账户:");
  console.log("  Deployer/Owner:", deployer.address);
  console.log("  Operator:", operator.address);
  console.log("  User1:", user1.address);
  console.log("  User2:", user2.address);
  console.log("");

  // 读取已部署的合约地址
  console.log("📖 读取已部署的合约地址...");
  const configPath = path.join(__dirname, "../../config/address.json");
  
  if (!fs.existsSync(configPath)) {
    throw new Error("❌ 未找到配置文件，请先运行部署脚本: npx hardhat run scripts/deploy-v1.js --network sepolia");
  }
  
  const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
  const proxyAddress = config.proxy;
  
  if (!proxyAddress) {
    throw new Error("❌ 配置文件中未找到合约地址");
  }
  
  console.log("✅ 合约地址:", proxyAddress);
  console.log("");

  // 连接到已部署的合约
  console.log("🔗 连接到合约...");
  const UserPointsV1 = await ethers.getContractFactory("UserPointsV1");
  const proxy = UserPointsV1.attach(proxyAddress);
  console.log("✅ 连接成功");
  console.log("");

  // 给 operator 添加 OPERATOR_ROLE（如果还没有）
  console.log("🔑 检查 operator 权限...");
  const OPERATOR_ROLE = await proxy.OPERATOR_ROLE();
  const hasRole = await proxy.hasRole(OPERATOR_ROLE, operator.address);
  
  if (!hasRole) {
    console.log("  正在为 operator 添加 OPERATOR_ROLE...");
    const tx = await proxy.grantRole(OPERATOR_ROLE, operator.address);
    await tx.wait();
    console.log("  ✅ 授权成功");
  } else {
    console.log("  ✅ operator 已有权限");
  }
  console.log("");

  // 测试1: 查询初始积分（应该为0）
  console.log("📊 测试1: 查询初始积分");
  let user1Points = await proxy.getUserPoints(user1.address);
  console.log("  User1 初始积分:", user1Points.totalPoints.toString());
  console.log("  User1 最后更新:", new Date(user1Points.lastUpdate.toNumber() * 1000).toLocaleString());
  console.log("");

  // 测试2: 添加积分
  console.log("➕ 测试2: 添加积分");
  const tx1 = await proxy.connect(operator).addPoints(user1.address, 1000);
  await tx1.wait();
  console.log("  ✅ 给 User1 添加 1000 积分");
  
  user1Points = await proxy.getUserPoints(user1.address);
  console.log("  User1 当前积分:", user1Points.totalPoints.toString());
  console.log("");

  // 测试3: 再次添加积分（累加）
  console.log("➕ 测试3: 累加积分");
  const tx2 = await proxy.connect(operator).addPoints(user1.address, 500);
  await tx2.wait();
  console.log("  ✅ 给 User1 再添加 500 积分");
  
  user1Points = await proxy.getUserPoints(user1.address);
  console.log("  User1 当前积分:", user1Points.totalPoints.toString());
  console.log("");

  // 测试4: 消耗积分
  console.log("💸 测试4: 消耗积分");
  const tx3 = await proxy.connect(operator).spendPoints(user1.address, 300);
  await tx3.wait();
  console.log("  ✅ User1 消耗 300 积分");
  
  user1Points = await proxy.getUserPoints(user1.address);
  console.log("  User1 剩余积分:", user1Points.totalPoints.toString());
  console.log("");

  // 测试5: 给另一个用户添加积分
  console.log("➕ 测试5: 多用户测试");
  await proxy.connect(operator).addPoints(user2.address, 2000);
  let user2Points = await proxy.getUserPoints(user2.address);
  console.log("  ✅ 给 User2 添加 2000 积分");
  console.log("  User2 当前积分:", user2Points.totalPoints.toString());
  console.log("");

  // 测试6: 清零积分
  console.log("🔄 测试6: 清零积分");
  const tx4 = await proxy.connect(operator).resetPoints(user1.address);
  await tx4.wait();
  console.log("  ✅ User1 积分清零");
  
  user1Points = await proxy.getUserPoints(user1.address);
  console.log("  User1 剩余积分:", user1Points.totalPoints.toString());
  console.log("");

  // 测试7: 权限测试 - 非操作员不能添加积分
  console.log("🔒 测试7: 权限测试");
  try {
    await proxy.connect(user1).addPoints(user2.address, 100);
    console.log("  ❌ 测试失败：非操作员不应该能添加积分");
  } catch (error) {
    console.log("  ✅ 正确拒绝：非操作员无法添加积分");
    console.log("  错误信息:", error.reason || error.message);
  }
  console.log("");

  // 测试8: 余额不足测试
  console.log("⚠️ 测试8: 余额不足测试");
  try {
    await proxy.connect(operator).spendPoints(user2.address, 99999);
    console.log("  ❌ 测试失败：不应该允许超额消耗");
  } catch (error) {
    console.log("  ✅ 正确拒绝：积分不足时交易回滚");
    console.log("  错误信息:", error.reason || error.message);
  }
  console.log("");

  // 测试9: 暂停功能测试
  console.log("⏸️ 测试9: 暂停功能测试");
  await proxy.pause();
  console.log("  ✅ 合约已暂停");
  
  try {
    await proxy.connect(operator).addPoints(user2.address, 100);
    console.log("  ❌ 测试失败：暂停后不应该能添加积分");
  } catch (error) {
    console.log("  ✅ 正确拒绝：合约暂停时无法操作");
    console.log("  错误信息:", error.reason || error.message);
  }
  
  await proxy.unpause();
  console.log("  ✅ 合约已恢复");
  console.log("");

  // 最终状态
  console.log("📈 最终状态:");
  user1Points = await proxy.getUserPoints(user1.address);
  user2Points = await proxy.getUserPoints(user2.address);
  console.log("  User1 积分:", user1Points.totalPoints.toString());
  console.log("  User2 积分:", user2Points.totalPoints.toString());
  console.log("");

  console.log("🎉 所有测试完成！");
}

main()
  .then(() => process.exit(0))
  .catch((err) => {
    console.error("❌ 测试失败:", err);
    process.exit(1);
  });
