package bootcamp;

import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.transactions.LedgerTransaction;
import static net.corda.core.contracts.ContractsDSL.requireSingleCommand;
import static net.corda.core.contracts.ContractsDSL.requireThat;

import java.util.List;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TokenContract implements Contract {
    public static String ID = "bootcamp.TokenContract";


    public void verify(LedgerTransaction tx) throws IllegalArgumentException {
        if(tx.getInputStates().size() != 0) {
            throw new IllegalArgumentException("Zero Inputs Expected"); }

        if( tx.getOutputStates().size() != 1 ) {
            throw new IllegalArgumentException("One Output Expected"); }

        if(tx.getCommands().size() != 1 ) {
            throw new IllegalArgumentException("One Command Expected"); }
    /* This Block of code Checks that our Contract has at least one command.
    If not,the code will raise Exception

    if(tx.getCommands().size() != 0 ) {
        throw new IllegalArgumentException("No Command Expected");}

    This code block will surely fail the test case of transaction .
    Because the code expects to have no command but a healthy code will have a command.
    */
        if(!(tx.getOutput(0) instanceof TokenState)) {
            throw new IllegalArgumentException("Token State Expected");
        }

        TokenState tokenState= (TokenState) tx.getOutput(0);

        if (!(tokenState.getAmount() > 0)){
            throw new IllegalArgumentException("Positive Expected");
        }

        if (!(tx.getCommand(0).getValue() instanceof Commands.Issue)){
            throw new IllegalArgumentException("Issue Command Expected");
        }

        if(!(tx.getCommand(0).getSigners().contains(tokenState.getIssuer().getOwningKey()))){
            throw new IllegalArgumentException("Issuer Must Sign");
        }


    }


    public interface Commands extends CommandData {
        class Issue implements Commands { }
    }
}
