const hre = require("hardhat");

async function main() {
  const [deployer] = await hre.ethers.getSigners();
  
  // 获取合约实例
  const AssetToken = await hre.ethers.getContractFactory("AssetToken");
  const tokenAddress = process.env.TOKEN_ADDRESS;
  
  if (!tokenAddress) {
    console.error("请设置 TOKEN_ADDRESS 环境变量");
    return;
  }
  
  const token = AssetToken.attach(tokenAddress);
  
  // 为指定的用户地址铸造代币
  const specificUser = "0xf39Fd6e51aad88F6F4ce6aB8827279cffFb92266";
  const mintAmount = hre.ethers.utils.parseUnits("1000", 18);
  
  console.log(`为账户 ${specificUser} 铸造代币...\n`);
  
  const tx = await token.mint(specificUser, mintAmount);
  await tx.wait();
  console.log(`已为账户 ${specificUser} 铸造 1000 个代币`);
  
  console.log("\n代币铸造完成！");
}

main().catch((error) => {
  console.error(error);
  process.exitCode = 1;
});
