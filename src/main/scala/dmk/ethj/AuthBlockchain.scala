package dmk.ethj

import java.math.BigInteger
import java.util.function.Function
import org.ethereum.config.SystemProperties
import org.ethereum.config.blockchain.FrontierConfig
import org.ethereum.util.blockchain.SolidityContract
import org.ethereum.util.blockchain.StandaloneBlockchain
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.Arrays
import org.spongycastle.util.encoders.Hex

/**
 * 
 */
class AuthsBlockchain extends BaseStandaloneBlockchain {
  override val logger = LoggerFactory.getLogger(classOf[AuthsBlockchain]);

  def init(): Unit = {

    resetBlockMiningLevel()
    val bc = startStandaloneChain()

    logger.debug("sender key=" + bc.getSender.toString)
    logger.debug("sender key address=" + Hex.toHexString(bc.getSender.getAddress))
    val contract = createContract(bc, AuthsBlockchain.contractSrc, "Auths")

    logger.debug(s"contract = $contract")
    // Check the contract state with a constant call which returns auths
    // but doesn't generate any transactions and remain the contract state unchanged
    val currentAuths = (contract: SolidityContract) => contract.callConstFunction("auths").mkString("");
    
    val curAuths = currentAuths(contract) 
		logger.debug(s"initial auths: $curAuths")
    val newAuths = curAuths + "&I"
    contract.callFunction("recalc", newAuths)
    logger.debug(s"recalculated info: ${currentAuths(contract)}")
    logger.debug("reset auths...")
    contract.callFunction("reset")
    logger.debug(currentAuths(contract))

    // We are done - the Solidity contract worked as expected.
    logger.debug("Done.")
  }

}

object AuthsBlockchain {
  
  // try event and Print
  val contractSrc = """
    contract Auths {
      string public auths = "C&P";
    
      function recalc(string newAuths) public returns (string) {
        auths = newAuths;
      }
    
      function reset() public returns (string) {
        auths = "C&P";
      }
    }
"""
  .stripMargin
  .toString 

  def main(args: Array[String]): Unit = {
    new AuthsBlockchain().init
    System.exit(0);
  }

}