package dmk.ethj;

import java.io.IOException;

import org.ethereum.solidity.compiler.CompilationResult;
import org.ethereum.solidity.compiler.CompilationResult.ContractMetadata;
import org.ethereum.solidity.compiler.SolidityCompiler;
import org.junit.Assert;
import org.junit.Test;

/**
 * Borrowed from 
 * https://github.com/ethereum/ethereumj/blob/develop/ethereumj-core/src/test/java/org/ethereum/solidity/CompilerTest.java
 * 
 * 
 * Created by Anton Nashatyrev on 03.03.2016.
 */
public class CompilerTest {

    @Test
    public void simpleTest() throws IOException {
        String contract =
            "contract a {" +
                    "  int i1;" +
                    "  function i() returns (int) {" +
                    "    return i1;" +
                    "  }" +
                    "}";
        SolidityCompiler.Result res = SolidityCompiler.compile(
                contract.getBytes(), true, SolidityCompiler.Options.ABI, SolidityCompiler.Options.BIN, SolidityCompiler.Options.INTERFACE);
        System.out.println("Out: '" + res.output + "'");
        System.out.println("Err: '" + res.errors + "'");
        CompilationResult result = CompilationResult.parse(res.output);

        ContractMetadata contractMeta = result.contracts.get("a");
        if (contractMeta == null)
        	Assert.fail();
        	
         System.out.println(contractMeta.bin);
         System.out.println(contractMeta.getInterface());
    }
}
