package dmk.ethj

object TestContracts {

  val baseTestContracts = """
    contract owned {

      address owner;

      function owned() {
        owner = msg.sender;
      }

      function changeOwner(address newOwner) onlyowner {
        owner = newOwner;
      }

      modifier onlyowner() {
        if (msg.sender==owner) _
      }
    }
    
    contract mortal is owned {

      function kill() onlyowner {
        if (msg.sender == owner) suicide(owner);
      }

    }
""".stripMargin.toString

}