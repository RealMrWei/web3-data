require("@nomiclabs/hardhat-ethers");
require("@openzeppelin/hardhat-upgrades");

/** @type import('hardhat/config').HardhatUserConfig */
module.exports = {
  solidity: "0.8.20",
  networks: {
    hardhat: {
      chainId: 31338,
    },
    localhost: {
      url: "http://127.0.0.1:8546",
      chainId: 31338,
    },
  },
};
