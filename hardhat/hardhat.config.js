require("@nomiclabs/hardhat-ethers");
require("@openzeppelin/hardhat-upgrades");

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: "0.8.20",
  networks: {
    hardhat: {
      chainId: 31337,
    },
    localhost: {
      url: "http://127.0.0.1:8545",
      chainId: 31337,
    },
    localhost2: {
      url: "http://127.0.0.1:8546",
      chainId: 31337, // Hardhat 节点默认都是 31337，无法通过命令行修改
      accounts: [
        "0xac0974bec39a17e36ba4a6b4d238ff944bacb478cbed5efcae784d7bf4f2ff80", // 账户 0
        "0x59c6995e998f97a5a0044966f0945389dc9e86dae88c7a8412f4603b6b78690d", // 账户 1
        "0x5de4111afa1a4b94908f83103eb1f1706367c2e68ca870fc3fb9a804cdab365a", // 账户 2
      ],
    },
    sepolia: {
      url: "https://ethereum-sepolia-rpc.publicnode.com",
      accounts: ["31f56a89c9bdd5c6e2f34e6e1dbc39b26ca0aeadb6c710ce49b97b7ae0b7e858"],
      gas: 5000000,
      timeout: 600000,
      chainId: 11155111,
      maxFeePerGas: 100000000000,      // 100 gwei - 提高最大费用
      maxPriorityFeePerGas: 5000000000  // 5 gwei - 提高优先费
    },
  },
};
