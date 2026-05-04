const { ethers, upgrades } = require("hardhat");

async function main() {
  console.log(" 开始测试资产管理系统...\n");

  const [owner, operator, user1, user2] = await ethers.getSigners();
  console.log("📋 测试账户:");
  console.log("  Owner:", owner.address);
  console.log("  Operator:", operator.address);
  console.log("  User1:", user1.address);
  console.log("  User2:", user2.address);
  console.log("");

  // ============================================
  // 1. 部署所有合约
  // ============================================
  console.log("🚀 部署合约...\n");

  // 部署 AssetToken
  console.log("📦 部署 AssetToken...");
  const AssetToken = await ethers.getContractFactory("AssetToken");
  const assetToken = await upgrades.deployProxy(
    AssetToken,
    ["Web3 Asset Token", "WAT", ethers.utils.parseEther("1000000")],
    { kind: "uups", unsafeAllow: ['constructor'] }
  );
  await assetToken.deployed();
  console.log("✅ AssetToken:", assetToken.address);

  // 部署 DepositVault
  console.log("📦 部署 DepositVault...");
  const DepositVault = await ethers.getContractFactory("DepositVault");
  const depositVault = await upgrades.deployProxy(
    DepositVault,
    [],
    { kind: "uups", unsafeAllow: ['constructor'] }
  );
  await depositVault.deployed();
  console.log("✅ DepositVault:", depositVault.address);

  // 部署 WithdrawalManager
  console.log(" 部署 WithdrawalManager...");
  const WithdrawalManager = await ethers.getContractFactory("WithdrawalManager");
  const withdrawalManager = await upgrades.deployProxy(
    WithdrawalManager,
    [],
    { 
      kind: "uups", 
      unsafeAllow: ['constructor']  // 添加这行
    }
  );
  await withdrawalManager.deployed();
  console.log("✅ WithdrawalManager:", withdrawalManager.address);
  console.log("");

  // ============================================
  // 2. 配置权限
  // ============================================
  console.log("🔑 配置权限...\n");

  // DepositVault 添加支持的代币
  console.log("添加 AssetToken 到 DepositVault 白名单...");
  await depositVault.addSupportedToken(assetToken.address);
  console.log("✅ 配置完成");

  // 给 operator 添加 DepositVault 的 OPERATOR_ROLE
  console.log("\n授予 Operator 角色...");
  const OPERATOR_ROLE = await depositVault.OPERATOR_ROLE();
  await depositVault.grantRole(OPERATOR_ROLE, operator.address);
  console.log("✅ Operator 已添加 DepositVault 权限");

  // 给 operator 添加 WithdrawalManager 的角色
  const wmOperatorRole = await withdrawalManager.OPERATOR_ROLE();
  const wmApproverRole = await withdrawalManager.APPROVER_ROLE();
  await withdrawalManager.grantRole(wmOperatorRole, operator.address);
  await withdrawalManager.grantRole(wmApproverRole, operator.address);
  console.log("✅ Operator 已添加 WithdrawalManager 权限");
  console.log("");

  // ============================================
  // 3. 测试充值流程
  // ============================================
  console.log("💰 测试1: 充值流程\n");

  // Owner 给 user1 转代币
  console.log("Owner 给 User1 转账 1000 WAT...");
  const transferAmount = ethers.utils.parseEther("1000");
  await assetToken.transfer(user1.address, transferAmount);
  console.log("✅ 转账成功");

  // 查询 user1 余额
  let user1Balance = await assetToken.balanceOf(user1.address);
  console.log("User1 WAT 余额:", ethers.utils.formatEther(user1Balance));

  // User1 批准 DepositVault 使用代币
  console.log("\nUser1 批准 DepositVault 使用代币...");
  await assetToken.connect(user1).approve(depositVault.address, transferAmount);
  console.log("✅ 批准成功");

  // User1 充值到 DepositVault
  console.log("\nUser1 充值 500 WAT 到 DepositVault...");
  const depositAmount = ethers.utils.parseEther("500");
  await depositVault.connect(user1).deposit(assetToken.address, depositAmount);
  console.log("✅ 充值成功");

  // 查询 DepositVault 中的余额
  const vaultBalance = await depositVault.getUserBalance(user1.address, assetToken.address);
  console.log("User1 在 DepositVault 中的余额:", ethers.utils.formatEther(vaultBalance));
  console.log("");

  // ============================================
  // 4. 测试批量充值
  // ============================================
  console.log(" 测试2: 批量充值\n");

  // Owner 给 user2 转代币
  console.log("Owner 给 User2 转账 2000 WAT...");
  await assetToken.transfer(user2.address, ethers.utils.parseEther("2000"));

  // User2 批准并批量充值
  console.log("User2 批准并充值...");
  await assetToken.connect(user2).approve(depositVault.address, ethers.utils.parseEther("800"));
  await depositVault.connect(user2).batchDeposit(
    [assetToken.address, assetToken.address],
    [ethers.utils.parseEther("300"), ethers.utils.parseEther("500")]
  );

  const user2VaultBalance = await depositVault.getUserBalance(user2.address, assetToken.address);
  console.log("User2 在 DepositVault 中的余额:", ethers.utils.formatEther(user2VaultBalance));
  console.log("");

  // ============================================
  // 5. 测试提现流程
  // ============================================
  console.log("💸 测试3: 提现流程\n");

  // 将资产从 DepositVault 转到 WithdrawalManager（模拟）
  console.log("配置 WithdrawalManager...");
  const withdrawalAmount = ethers.utils.parseEther("200");

  // 直接在 WithdrawalManager 中存入资产（通过 depositToVault）
  await assetToken.connect(user1).approve(withdrawalManager.address, withdrawalAmount);
  await withdrawalManager.connect(user1).depositToVault(assetToken.address, withdrawalAmount);

  const wmBalance = await withdrawalManager.getUserBalance(user1.address, assetToken.address);
  console.log("User1 在 WithdrawalManager 中的余额:", ethers.utils.formatEther(wmBalance));

  // User1 请求提现
  console.log("\nUser1 请求提现 100 WAT...");
  const txRequest = await withdrawalManager.connect(user1).requestWithdrawal(
    assetToken.address,
    ethers.utils.parseEther("100")
  );
  const receipt = await txRequest.wait();
  const event = receipt.events?.find(e => e.event === "WithdrawalRequested");
  const withdrawalId = event?.args?.withdrawalId;
  console.log("✅ 提现请求成功, ID:", withdrawalId.toString());

  // Operator 审批提现
  console.log("\nOperator 审批提现...");
  await withdrawalManager.connect(operator).approveWithdrawal(withdrawalId);
  console.log("✅ 审批通过");

  // Operator 执行提现
  console.log("\nOperator 执行提现...");
  await withdrawalManager.connect(operator).executeWithdrawal(withdrawalId);
  console.log("✅ 提现完成");

  // 查询最终余额
  const finalBalance = await withdrawalManager.getUserBalance(user1.address, assetToken.address);
  console.log("User1 在 WithdrawalManager 中的余额:", ethers.utils.formatEther(finalBalance));
  console.log("");

  // ============================================
  // 6. 测试每日限额
  // ============================================
  console.log("📊 测试4: 每日提现限额\n");

  const todayWithdrawal = await withdrawalManager.getTodayWithdrawal(user1.address, assetToken.address);
  console.log("User1 今日已提现:", ethers.utils.formatEther(todayWithdrawal));

  const dailyLimit = await withdrawalManager.dailyLimit();
  console.log("每日限额:", ethers.utils.formatEther(dailyLimit));
  console.log("");

  // ============================================
  // 7. 权限测试
  // ============================================
  console.log("🔒 测试5: 权限控制\n");

  try {
    await depositVault.connect(user1).addSupportedToken(user2.address);
    console.log("❌ 测试失败：普通用户不应该能添加代币");
  } catch (error) {
    console.log("✅ 正确拒绝：普通用户无法添加支持的代币");
  }
  console.log("");

  // ============================================
  // 8. 最终状态汇总
  // ============================================
  console.log("📈 最终状态汇总:\n");
  console.log("AssetToken 总供应量:", ethers.utils.formatEther(await assetToken.totalSupply()));
  console.log("User1 WAT 余额:", ethers.utils.formatEther(await assetToken.balanceOf(user1.address)));
  console.log("User2 WAT 余额:", ethers.utils.formatEther(await assetToken.balanceOf(user2.address)));
  console.log("DepositVault 余额:", ethers.utils.formatEther(await assetToken.balanceOf(depositVault.address)));
  console.log("WithdrawalManager 余额:", ethers.utils.formatEther(await assetToken.balanceOf(withdrawalManager.address)));
  console.log("");

  console.log(" 所有测试完成！");
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error("❌ 测试失败:", error);
    process.exit(1);
  });
