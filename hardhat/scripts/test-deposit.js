const hre = require("hardhat");

async function main() {
  // 使用指定的测试账户
  const specificUser = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";
  
  // 使用 provider 来获取指定地址的 signer
  const userSigner = new hre.ethers.Wallet("0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80", hre.ethers.provider);
  
  // 获取合约实例
  const AssetToken = await hre.ethers.getContractFactory("AssetToken");
  const DepositVault = await hre.ethers.getContractFactory("DepositVault");
  
  const tokenAddress = process.env.TOKEN_ADDRESS;
  const vaultAddress = process.env.VAULT_ADDRESS;
  
  if (!tokenAddress || !vaultAddress) {
    console.error("请设置 TOKEN_ADDRESS 和 VAULT_ADDRESS 环境变量");
    return;
  }
  
  const token = AssetToken.attach(tokenAddress);
  const vault = DepositVault.attach(vaultAddress);
  
  // 1. 授权 Vault 使用代币
  const approveAmount = hre.ethers.utils.parseUnits("100", 18);  // ethers v5
  const approveTx = await token.connect(userSigner).approve(vaultAddress, approveAmount);
  await approveTx.wait();
  console.log(`✓ 已授权 Vault 使用 ${hre.ethers.utils.formatUnits(approveAmount, 18)} 个代币`);  // ethers v5
  
  // 2. 调用 deposit 函数
  const depositAmount = hre.ethers.utils.parseUnits("10", 18);  // ethers v5
  const depositTx = await vault.connect(userSigner).deposit(tokenAddress, depositAmount);
  const receipt = await depositTx.wait();
  
  console.log(`✓ 充值成功！`);
  console.log(`  交易哈希: ${receipt.transactionHash}`);  // ethers v5 正确字段
  console.log(`  区块号: ${receipt.blockNumber}`);
  console.log(`  充值金额: ${hre.ethers.utils.formatUnits(depositAmount, 18)} 代币`);  // ethers v5
  console.log(`  用户地址: ${userSigner.address}`);
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
