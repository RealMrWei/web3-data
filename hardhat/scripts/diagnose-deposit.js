const hre = require("hardhat");
const fs = require("fs");
const path = require("path");

async function main() {
  console.log("========== 自动修复并测试充值 ==========\n");

  // 从配置文件读取合约地址
  const configPath = path.join(__dirname, "../config/address.json");
  const config = JSON.parse(fs.readFileSync(configPath, "utf8"));
  
  const TOKEN_ADDRESS = config.token;
  const VAULT_ADDRESS = config.depositVault;
  
  console.log("使用合约地址:");
  console.log("  Token:", TOKEN_ADDRESS);
  console.log("  Vault:", VAULT_ADDRESS);
  console.log();

  const [owner] = await hre.ethers.getSigners();
  const testAccount = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";

  const token = await hre.ethers.getContractAt("AssetToken", TOKEN_ADDRESS);
  const vault = await hre.ethers.getContractAt("DepositVault", VAULT_ADDRESS);

  // 1. Vault 初始化状态
  console.log("[1] Vault 初始化状态:");
  try {
    const vaultOwner = await vault.owner();
    console.log("  ✅ 已初始化, owner:", vaultOwner);
  } catch (e) {
    console.log("  ⚠️ 未初始化，正在初始化...");
    await vault.initialize();
    console.log("  ✅ Vault 初始化完成");
  }

  // 2. 支持的代币列表
  console.log("\n[2] 支持的代币:");
  const tokens = await vault.getSupportedTokens();
  if (!tokens.includes(TOKEN_ADDRESS)) {
    console.log("  ⚠️ 代币不在列表中，正在添加...");
    await vault.addSupportedToken(TOKEN_ADDRESS);
    console.log("  ✅ 代币已添加");
  } else {
    console.log("  ✅ 代币已在列表中");
  }

  // 3. 用户余额（如果为 0 则铸造）
  console.log("\n[3] 测试账户余额:");
  let balance = await token.balanceOf(testAccount);
  console.log("  当前余额:", hre.ethers.utils.formatEther(balance));
  
  if (balance.eq(0)) {
    console.log("  ⚠️ 余额为 0，正在铸造 1000 代币...");
    
    // 直接使用部署者账户铸造（部署者应该有 MINTER_ROLE）
    const mintAmount = hre.ethers.utils.parseEther("1000");
    
    try {
      await token.mint(testAccount, mintAmount);
      balance = await token.balanceOf(testAccount);
      console.log("  ✅ 铸造完成，当前余额:", hre.ethers.utils.formatEther(balance));
    } catch (error) {
      console.log("  ❌ 铸造失败:", error.message);
      console.log("  💡 提示：可能需要重新部署合约或手动授权");
      return;
    }
  } else {
    console.log("  ✅ 余额充足");
  }

  // 4. 授权额度
  console.log("\n[4] 对 Vault 的授权:");
  let allowance = await token.allowance(testAccount, VAULT_ADDRESS);
  console.log("  当前授权:", hre.ethers.utils.formatEther(allowance));
  
  const requiredAmount = hre.ethers.utils.parseEther("100");
  if (allowance.lt(requiredAmount)) {
    console.log("  ⚠️ 授权不足，正在授权 1000 代币...");
    const signer = await hre.ethers.getSigner(testAccount);
    const tokenAsUser = token.connect(signer);
    await tokenAsUser.approve(VAULT_ADDRESS, hre.ethers.utils.parseEther("1000"));
    allowance = await token.allowance(testAccount, VAULT_ADDRESS);
    console.log("  ✅ 授权完成，当前授权:", hre.ethers.utils.formatEther(allowance));
  } else {
    console.log("  ✅ 授权充足");
  }

  // 5. 模拟充值
  console.log("\n[5] 执行充值操作:");
  try {
    const signer = await hre.ethers.getSigner(testAccount);
    const vaultAsUser = vault.connect(signer);
    
    console.log("  发送充值交易 (100 代币)...");
    const tx = await vaultAsUser.deposit(TOKEN_ADDRESS, requiredAmount);
    console.log("  ✅ 交易已提交:", tx.hash);
    
    console.log("  等待确认...");
    const receipt = await tx.wait();
    console.log("  ✅ 充值成功!");
    console.log("  Gas 消耗:", receipt.gasUsed.toString());
    
    // 验证结果
    const vaultBalance = await vault.getUserBalance(testAccount, TOKEN_ADDRESS);
    console.log("\n[6] 验证结果:");
    console.log("  Vault 中余额:", hre.ethers.utils.formatEther(vaultBalance));
    
    const remainingBalance = await token.balanceOf(testAccount);
    console.log("  用户剩余余额:", hre.ethers.utils.formatEther(remainingBalance));
    
    console.log("\n========== ✅ 所有测试通过，Java API 应该可以正常工作 ==========");
    
  } catch (error) {
    console.log("  ❌ 充值失败!");
    console.log("  错误信息:", error.message);
    if (error.data) {
      console.log("  错误数据:", error.data);
    }
  }
}

main()
  .then(() => process.exit(0))
  .catch((error) => {
    console.error(error);
    process.exit(1);
  });
