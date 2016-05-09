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
import org.apache.commons.lang3.time.StopWatch
import org.ethereum.crypto.ECKey
import scala.collection.JavaConversions._

/**
 * 
 */
class AuthsBlockchain2 extends BaseStandaloneBlockchain {
  override val logger = LoggerFactory.getLogger(classOf[AuthsBlockchain2]);

  def init(): Unit = {

    resetBlockMiningLevel()
    val bc = startStandaloneChain()
    
    logger.debug("sender key=" + bc.getSender.toString)
    logger.debug("sender key address=" + Hex.toHexString(bc.getSender.getAddress))
    logger.debug("\n")
    // Check the contract state with a constant call which returns auths
    // but doesn't generate any transactions and remain the contract state unchanged
    val contractOwner = (contract: SolidityContract) => Arrays.toString(contract.callConstFunction("owner"))
    val currentAuths = (contract: SolidityContract) => contract.callConstFunction("auths").mkString("")
    val contractAccount = (contract: SolidityContract) => { 
      val arr = contract.callConstFunction("account")
      arr.mkString
    }
    val setAccount = (contract: SolidityContract, addr: String) => contract.callFunction("setAccount", addr)
    
    var abi:String = ""
    val coinbase = bc.getBlockchain.getBestBlock.getCoinbase
    val allAccounts = listAccounts(bc)
    val accounts = allAccounts.filter { x => !x.sameElements(coinbase) }
    logger.debug(Hex.toHexString(coinbase))
    logger.debug(accounts.size.toString)
    
    val contractAddresses = accounts.map( accArr => {  
      val contract = createContract(bc, AuthsBlockchain2.contractSrc, "Auths")
      val acc = Hex.toHexString(accArr)
      setAccount(contract, acc)
//       owner
//      logger.debug(s"contract owner is ${contractOwner(contract)}")
      // account
      logger.debug(s"contract account is ${contractAccount(contract)}")
      val curAuths = currentAuths(contract) 
      logger.debug(s"current auths: $curAuths")
      val newAuths = curAuths + "&I"
      contract.callFunction("recalc", newAuths)
      logger.debug(s"recalculated info: ${currentAuths(contract)}")
      listLatestBlock(bc)
      abi = contract.getABI
      contract.getAddress
    })
    
    System.out.println(abi)
    
    contractAddresses.map( contractAddress => {
      val contract = getExistingContract(bc, abi, contractAddress)
      logger.debug(s"contract account is ${contractAccount(contract)}")
      val curAuths = currentAuths(contract) 
      logger.debug(s"current auths: $curAuths")
    })
    
    contractAddresses.map( contractAddress => {
      val contract = getExistingContract(bc, abi, contractAddress)
      logger.debug(s"contract account is ${contractAccount(contract)}")
      logger.debug("reset auths...")
      contract.callFunction("reset")
      logger.debug(currentAuths(contract))
    })
    
    contractAddresses.map( contractAddress => {
      val contract = getExistingContract(bc, abi, contractAddress)
      logger.debug(s"contract account is ${contractAccount(contract)}")
      logger.debug(s"killing contract...")
      contract.callFunction("kill")
      logger.debug("done")
    })
    
    contractAddresses.map( contractAddress => {
      val contract = getExistingContract(bc, abi, contractAddress)
      val contractDetails = bc.getBlockchain.getRepository.getContractDetails(contractAddress)
      val deleted = contractDetails.isDeleted()
      logger.debug(s"${Hex.toHexString(contractAddress)} is deleted? $deleted")
    })
    
    listLatestBlock(bc)
    
    // We are done - the Solidity contract worked as expected.
    logger.debug("Done.")
  }
  
  override def createContract(bc: StandaloneBlockchain, contractSrc: String, name: String): SolidityContract = {
    logger.debug("creating a contract...")
    val stopWatch = new StopWatch()
    stopWatch.start()
    val contract = bc.submitNewContract(contractSrc, name)
    stopWatch.stop()
    logger.debug(s"finish in ${stopWatch.toString}")
    logger.debug(contract.toString)
    logger.debug(s"contract @ address: ${Hex.toHexString(contract.getAddress)}")
    logger.debug(s"contract abi: ${contract.getABI}")
    contract
  }
  
  def getExistingContract(bc: StandaloneBlockchain, abi: String, address: Array[Byte]) : SolidityContract = {
    // stacktrace when trying by abi
//    bc.createExistingContractFromABI(abi, address)
    bc.createExistingContractFromSrc(AuthsBlockchain2.contractSrc, "Auths", address)
  }
}

object AuthsBlockchain2 {
  
  // try event and Print
  val contractSrc = 
    TestContracts.baseTestContracts +
    """

    contract Auths is owned, mortal {

      event Print(string out);
      
      address public account;
      string public auths;
    
     function Auths(address acc) {
        account = acc;
        reset();
      }

      function setAccount(address addr) public returns (address) {
        account = addr;
      }

      function recalc(string newAuths) public returns (string) {
        Print(" has new auths ");
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
    new AuthsBlockchain2().init
    System.exit(0);
  }

}