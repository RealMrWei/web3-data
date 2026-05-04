const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  console.log("🔧 批准并执行提现请求\n");

  // 读取配置
  const configPath = path.join(__dirname, "../../config/address.json");
  const contractsConfigPath = path.join(__dirname, "../../config/asset-contracts.json");
  
  const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
  const contractsConfig = JSON.parse(fs.readFileSync(contractsConfigPath, "utf8"));
  
  const TOKEN_ADDRESS = contractsConfig.contracts.assetToken;
  const WITHDRAWAL_MANAGER_ADDRESS = config.withdrawalManager;
  
  console.log("Token:", TOKEN_ADDRESS);
  console.log("WithdrawalManager:", WITHDRAWAL_MANAGER_ADDRESS);
  console.log();

  // 获取账户 - 使用部署者账户（通常具有所有角色）
  const [deployer, user] = await hre.ethers.getSigners();
  console.log("操作员账户 (deployer):", deployer.address);
  console.log("用户账户 (user):", user.address);
  console.log();

  // 加载合约
  const token = await hre.ethers.getContractAt("AssetToken", TOKEN_ADDRESS);
  const withdrawalManager = await hre.ethers.getContractAt("WithdrawalManager", WITHDRAWAL_MANAGER_ADDRESS);

  // 验证合约地址是否正确
  try {
    const owner = await withdrawalManager.owner();
    console.log("✅ WithdrawalManager 合约验证成功，所有者:", owner);
  } catch (error) {
    console.error("❌ 无法验证 WithdrawalManager 合约:", error.message);
    return;
  }
  console.log();

  // 获取待处理的提现记录
  console.log("[1] 查询待处理的提现记录...");
  const withdrawalCount = await withdrawalManager.withdrawalCount();
  const lastWithdrawalId = withdrawalCount.sub(1);
  const record = await withdrawalManager.getWithdrawalRecord(lastWithdrawalId);
  
  console.log("  提现ID:", lastWithdrawalId.toString());
  console.log("  状态:", ["Pending", "Approved", "Rejected", "Completed"][record.status]);
  console.log("  用户:", record.user);
  console.log("  金额:", hre.ethers.utils.formatEther(record.amount), "ATK");
  console.log("  代币:", record.token);
  console.log();

  if (record.status !== 0) { // 0 = Pending
    console.log("⚠️ 提现记录状态不是待批准状态，跳过批准步骤");
  } else {
    // 步骤 1: 批准提现请求
    console.log("[2] 批准提现请求...");
    try {
      const approveTx = await withdrawalManager.connect(deployer).approveWithdrawal(lastWithdrawalId);
      await approveTx.wait();
      console.log("  ✅ 批准成功");
      console.log("  交易哈希:", approveTx.hash);
      
      // 重新查询记录确认状态更新
      const updatedRecord = await withdrawalManager.getWithdrawalRecord(lastWithdrawalId);
      console.log("  新状态:", ["Pending", "Approved", "Rejected", "Completed"][updatedRecord.status]);
      console.log();
    } catch (error) {
      console.error("❌ 批准失败:", error.message);
      return;
    }
  }

  // 步骤 2: 执行提现
  console.log("[3] 执行提现...");
  try {
    // 检查执行前的各方余额
    const recipientBalanceBefore = await token.balanceOf(record.user);
    const wmBalanceBefore = await withdrawalManager.getUserBalance(record.user, TOKEN_ADDRESS);
    const contractBalanceBefore = await token.balanceOf(WITHDRAWAL_MANAGER_ADDRESS);
    
    console.log("  提现前 - 接收方余额:", hre.ethers.utils.formatEther(recipientBalanceBefore), "ATK");
    console.log("  提现前 - WM中用户余额:", hre.ethers.utils.formatEther(wmBalanceBefore), "ATK");
    console.log("  提现前 - 合约余额:", hre.ethers.utils.formatEther(contractBalanceBefore), "ATK");
    console.log();

    const executeTx = await withdrawalManager.connect(deployer).executeWithdrawal(lastWithdrawalId);
    await executeTx.wait();
    console.log("  ✅ 提现执行成功");
    console.log("  交易哈希:", executeTx.hash);
    
    // 检查执行后的各方余额
    const recipientBalanceAfter = await token.balanceOf(record.user);
    const wmBalanceAfter = await withdrawalManager.getUserBalance(record.user, TOKEN_ADDRESS);
    const contractBalanceAfter = await token.balanceOf(WITHDRAWAL_MANAGER_ADDRESS);
    
    console.log("  提现后 - 接收方余额:", hre.ethers.utils.formatEther(recipientBalanceAfter), "ATK");
    console.log("  提现后 - WM中用户余额:", hre.ethers.utils.formatEther(wmBalanceAfter), "ATK");
    console.log("  提现后 - 合约余额:", hre.ethers.utils.formatEther(contractBalanceAfter), "ATK");
    console.log();

    // 验证余额变化
    console.log("  验证 - 接收方增加:", hre.ethers.utils.formatEther(recipientBalanceAfter.sub(recipientBalanceBefore)), "ATK");
    console.log("  验证 - WM用户余额减少:", hre.ethers.utils.formatEther(wmBalanceBefore.sub(wmBalanceAfter)), "ATK");
    console.log("  验证 - 合约余额减少:", hre.ethers.utils.formatEther(contractBalanceBefore.sub(contractBalanceAfter)), "ATK");
    console.log();

  } catch (error) {
    console.error("❌ 执行提现失败:", error.message);
    return;
  }

  // 步骤 3: 最终验证
  console.log("[4] 最终验证...");
  const finalRecord = await withdrawalManager.getWithdrawalRecord(lastWithdrawalId);
  console.log("  最终状态:", ["Pending", "Approved", "Rejected", "Completed"][finalRecord.status]);
  console.log("  完成时间戳:", finalRecord.completedAt.toString());
  console.log();

  console.log("✅ 提现批准和执行流程完成！");
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });