package dmk.ethj

import java.math.BigInteger

import scala.annotation.migration
import scala.collection.JavaConversions.asScalaSet

import org.apache.commons.lang3.time.StopWatch
import org.ethereum.config.SystemProperties
import org.ethereum.config.blockchain.HomesteadConfig
import org.ethereum.util.blockchain.SolidityContract
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.slf4j.LoggerFactory
import org.spongycastle.util.encoders.Hex

class BaseStandaloneBlockchain {
  val logger = LoggerFactory.getLogger(classOf[BaseStandaloneBlockchain]);
  
  def resetBlockMiningLevel(): Unit = {
    // need to modify the default Frontier settings to keep the blocks difficulty
    // low to not waste a lot of time for block mining
//    SystemProperties.CONFIG.setBlockchainConfig(new FrontierConfig(
//      new FrontierConfig.FrontierConstants() {
//        override def getMINIMUM_DIFFICULTY(): BigInteger = {
//          return BigInteger.ONE;
//        }
//      }));
    SystemProperties.CONFIG.setBlockchainConfig(new HomesteadConfig(
      new HomesteadConfig.HomesteadConstants {
        override def getMINIMUM_DIFFICULTY(): BigInteger = {
          return BigInteger.ONE;
        }
    }));

    logger.debug("genisis: " + SystemProperties.CONFIG.genesisInfo())
    logger.debug("coinbase:" + Hex.toHexString(SystemProperties.CONFIG.getMinerCoinbase))
  }

  def startStandaloneChain(): StandaloneBlockchain = {
    // Creating a blockchain which generates a new block for each transaction
    // just not to call createBlock() after each call transaction
    val bc = new StandaloneBlockchain().withAutoblock(true)
    logger.debug("creating first empty block (need some time to generate DAG)...")
    // warning up the block miner just to understand how long
    // the initial miner dataset is generated
    val stopWatch = new StopWatch()
    stopWatch.start()
    var block = bc.createBlock()
    stopWatch.stop()
    logger.debug(s"finished block in ${stopWatch}")
    
    val coinBase = Hex.toHexString(block.getCoinbase)
    val difficulty = Hex.toHexString(block.getDifficulty)
    val isGenesis = block.isGenesis
    val gasUsed = block.getGasUsed
    val nonce = Hex.toHexString(block.getNonce)
    val ts = block.getTimestamp
    val hash = Hex.toHexString(block.getHash)
    logger.debug(s"isGenesis: $isGenesis")
    logger.debug(s"difficulty: $difficulty")
    logger.debug(s"coinBase: $coinBase")
    logger.debug(s"gasUsed: $gasUsed")
    logger.debug(s"nonce: $nonce")
    logger.debug(s"ts: $ts")
    logger.debug(s"hash: $hash")
    
    bc
  }

  def createContract(bc: StandaloneBlockchain, contractSrc: String, contractName: String): SolidityContract = {
    logger.debug("creating a contract...")
    // This compiles our Solidity contract, submits it to the blockchain
    // internally generates the block with this transaction and returns the
    // contract interface
    val stopWatch = new StopWatch()
    stopWatch.start()
    val contract = bc.submitNewContract(contractSrc, contractName)
    stopWatch.stop()
    logger.debug(s"finish in ${stopWatch.toString}")

    logger.debug(contract.toString)
    logger.debug(s"contract @ address: ${Hex.toHexString(contract.getAddress)}")
    // Creates the contract call transaction, submits it to the blockchain
    // and generates a new block which includes this transaction
    // After new block is generated the contract state is changed
    contract
  }
  
  def listLatestBlock(bc: StandaloneBlockchain) : Unit = {
    val blockStore = bc.getBlockchain.getBlockStore
    logger.debug(s"--- latest block")
    logger.debug(s"total difficulty: ${blockStore.getTotalDifficulty.toString}")
    logger.debug(s"best block: ${blockStore.getBestBlock.toString()}")
  }
  
  def listAccounts(bc: StandaloneBlockchain) : java.util.Set[Array[Byte]] = {
    logger.debug("--- block chain accounts")
    val accounts = bc.getBlockchain.getRepository.getAccountsKeys
    accounts.map { x =>
      val balance = bc.getBlockchain.getRepository.getBalance(x)
      val acc = Hex.toHexString(x)
      logger.debug(s"$acc => $balance")
    }
    
    accounts
  }
}