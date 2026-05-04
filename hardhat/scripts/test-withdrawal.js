const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  console.log("🧪 测试提现流程\n");

  // 读取配置
  const configPath = path.join(__dirname, "../../config/address.json");
  const contractsConfigPath = path.join(__dirname, "../../config/asset-contracts.json");
  
  const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
  const contractsConfig = JSON.parse(fs.readFileSync(contractsConfigPath, "utf8"));
  
  const TOKEN_ADDRESS = contractsConfig.contracts.assetToken;  // 从asset-contracts.json读取
  const WITHDRAWAL_MANAGER_ADDRESS = config.withdrawalManager;
  
  console.log("Token:", TOKEN_ADDRESS);
  console.log("WithdrawalManager:", WITHDRAWAL_MANAGER_ADDRESS);
  console.log();

  // 获取账户 - 使用特定测试账户
  const [, user] = await hre.ethers.getSigners(); // 获取第二个账户，即0x70997970c51812dc3a010c7d01b50e0d17dc79c8
  const specificUser = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266"; // 指定的测试账户
  
  // 使用 provider 来获取指定地址的 signer
  const userSigner = new hre.ethers.Wallet("0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80", hre.ethers.provider);
  
  console.log("使用账户:", userSigner.address);
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

  // 步骤 1: 检查用户余额
  console.log("[1] 检查用户 Token 余额...");
  const balance = await token.balanceOf(user.address);
  console.log("  余额:", hre.ethers.utils.formatEther(balance), "ATK");
  console.log();

  if (balance.eq(0)) {
    console.log("⚠️ 余额为 0，请先充值或铸造代币");
    return;
  }

  // 步骤 2: 授权 WithdrawalManager
  console.log("[2] 授权 WithdrawalManager 使用代币...");
  const approveAmount = hre.ethers.utils.parseEther("50");
  const approveTx = await token.connect(userSigner).approve(WITHDRAWAL_MANAGER_ADDRESS, approveAmount);
  await approveTx.wait();
  console.log("  ✅ 授权成功:", hre.ethers.utils.formatEther(approveAmount), "ATK");
  console.log("  📄 交易哈希:", approveTx.hash);
  console.log("  📏 区块高度:", approveTx.blockNumber);
  console.log();

  // 步骤 3: 存入 WithdrawalManager（记录余额）
  console.log("[3] 存入 WithdrawalManager...");
  try {
    const depositTx = await withdrawalManager.connect(userSigner).depositToVault(TOKEN_ADDRESS, approveAmount);
    const depositReceipt = await depositTx.wait();
    console.log("  ✅ 存入成功");
    console.log("  📄 交易哈希:", depositTx.hash);
    console.log("  📏 区块高度:", depositReceipt.blockNumber);
    console.log("  ⛽ Gas 使用量:", depositReceipt.gasUsed.toString());
    console.log();
  } catch (error) {
    console.error("❌ 存入失败:", error.message);
    console.log("提示: 请确认合约中存在 depositToVault 函数且合约已正确部署");
    return;
  }

  // 步骤 4: 检查 WithdrawalManager 中的余额
  console.log("[4] 检查 WithdrawalManager 中的余额...");
  const wmBalance = await withdrawalManager.getUserBalance(userSigner.address, TOKEN_ADDRESS);
  console.log("  余额:", hre.ethers.utils.formatEther(wmBalance), "ATK");
  console.log();

  // 步骤 5: 请求提现
  console.log("[5] 请求提现 10 ATK...");
  const withdrawAmount = hre.ethers.utils.parseEther("10");
  const requestTx = await withdrawalManager.connect(userSigner).requestWithdrawal(TOKEN_ADDRESS, withdrawAmount);
  const requestReceipt = await requestTx.wait();
  console.log("  ✅ 提现请求已提交");
  console.log("  📄 交易哈希:", requestTx.hash);
  console.log("  📏 区块高度:", requestReceipt.blockNumber);
  console.log("  ⛽ Gas 使用量:", requestReceipt.gasUsed.toString());
  console.log();

  // 步骤 6: 查询提现记录
  console.log("[6] 查询提现记录...");
  const withdrawalCount = await withdrawalManager.withdrawalCount();
  const lastWithdrawalId = withdrawalCount.sub(1);
  const record = await withdrawalManager.getWithdrawalRecord(lastWithdrawalId);
  console.log("  提现ID:", lastWithdrawalId.toString());
  console.log("  状态:", ["Pending", "Approved", "Rejected", "Completed"][record.status]);
  console.log("  金额:", hre.ethers.utils.formatEther(record.amount), "ATK");
  console.log("  用户:", record.user);
  console.log("  代币:", record.token);
  console.log("  时间戳:", record.timestamp.toString());
  console.log();

  console.log("✅ 提现测试完成！下一步需要 Operator 审批并执行提现。");
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });