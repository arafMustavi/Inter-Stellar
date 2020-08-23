package net.corda.examples.tokenizedhouse.flows;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.r3.corda.lib.tokens.contracts.states.FungibleToken;
import com.r3.corda.lib.tokens.contracts.types.TokenType;
import com.r3.corda.lib.tokens.workflows.flows.rpc.*;
import com.r3.corda.lib.tokens.workflows.utilities.FungibleTokenBuilder;
import net.corda.examples.tokenizedhouse.states.FungibleHouseTokenState;
import kotlin.Unit;
import net.corda.core.contracts.*;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;

import java.math.BigDecimal;

/**
 * Create,Issue,Move,Redeem token flows for a house asset on ledger
 */
public class RealEstateEvolvableFungibleTokenFlow {

    private RealEstateEvolvableFungibleTokenFlow() {
        //Instantiation not allowed
    }

    /**
     * Create Fungible Token for a house asset on ledger
     */
    @StartableByRPC
    public static class CreateToken extends FlowLogic<SignedTransaction> {

        // valuation property of a house can change hence we are considering house as a evolvable asset
        private final BigDecimal valuation;
        private final String symbol;

        public CreateToken(String symbol, BigDecimal valuation) {
            this.valuation = valuation;
            this.symbol = symbol;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            // Obtain a reference to a notary we wish to use.
            /** METHOD 1: Take first notary on network, WARNING: use for test, non-prod environments, and single-notary networks only!*
             *  METHOD 2: Explicit selection of notary by CordaX500Name - argument can by coded in flow or parsed from config (Preferred)
             *
             *  * - For production you always want to use Method 2 as it guarantees the expected notary is returned.
             */
            final Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0); // METHOD 1
            // final Party notary = getServiceHub().getNetworkMapCache().getNotary(CordaX500Name.parse("O=Notary,L=London,C=GB")); // METHOD 2

            //create token type
            FungibleHouseTokenState evolvableTokenType = new FungibleHouseTokenState(valuation, getOurIdentity(),
                    new UniqueIdentifier(), 0, this.symbol);

            //wrap it with transaction state specifying the notary
            TransactionState<FungibleHouseTokenState> transactionState = new TransactionState<>(evolvableTokenType, notary);

            //call built in sub flow CreateEvolvableTokens. This can be called via rpc or in unit testing
            return subFlow(new CreateEvolvableTokens(transactionState));
        }
    }

    /**
     *  Issue Fungible Token against an evolvable house asset on ledger
     */
    @StartableByRPC
    public static class IssueToken extends FlowLogic<SignedTransaction>{
        private final String currency;
        private final int quantity;
        private final Party receiver;

        public IssueToken(String currency, int quantity, Party receiver) {
            this.currency = currency;
            this.quantity = quantity;
            this.receiver = receiver;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //get house states on ledger with uuid as input tokenId
            StateAndRef<FungibleHouseTokenState> stateAndRef = getServiceHub().getVaultService().
                    queryBy(FungibleHouseTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(currency)).findAny()
                    .orElseThrow(()-> new IllegalArgumentException("FungibleTokenType=\""+ currency +"\" not found from vault"));

            //get the RealEstateEvolvableTokenType object
            FungibleHouseTokenState evolvableTokenType = stateAndRef.getState().getData();

            //create fungible token for the house token type
            FungibleToken fungibleToken = new FungibleTokenBuilder()
                    .ofTokenType(evolvableTokenType.toPointer(FungibleHouseTokenState.class)) // get the token pointer
                    .issuedBy(getOurIdentity())
                    .heldBy(receiver)
                    .withAmount(quantity)
                    .buildFungibleToken();

            //use built in flow for issuing tokens on ledger
            return subFlow(new IssueTokens(ImmutableList.of(fungibleToken)));
        }
    }

    /**
     *  Move created fungible tokens to other party
     */
    @StartableByRPC
    @InitiatingFlow
    public static class MoveToken extends FlowLogic<SignedTransaction>{
        private final String currency;
        private final Party receiver;
        private final int quantity;

        public MoveToken(String currency, Party receiver, int quantity) {
            this.currency = currency;
            this.receiver = receiver;
            this.quantity = quantity;
        }

        @Override
        @Suspendable
        public SignedTransaction call() throws FlowException {
            //get house states on ledger with uuid as input tokenId
            StateAndRef<FungibleHouseTokenState> stateAndRef = getServiceHub().getVaultService().
                    queryBy(FungibleHouseTokenState.class).getStates().stream()
                    .filter(sf->sf.getState().getData().getSymbol().equals(currency)).findAny()
                    .orElseThrow(()-> new IllegalArgumentException("FungibleTokenType=\""+ currency +"\" not found from vault"));

            //get the RealEstateEvolvableTokenType object
            FungibleHouseTokenState tokenstate = stateAndRef.getState().getData();

            /*  specify how much amount to transfer to which holder
             *  Note: we use a pointer of tokenstate because it of type EvolvableTokenType
             */
            Amount<TokenType> amount = new Amount<>(quantity, tokenstate.toPointer(FungibleHouseTokenState.class));
            //PartyAndAmount partyAndAmount = new PartyAndAmount(holder, amount);

            //use built in flow to move fungible tokens to holder
            return subFlow(new MoveFungibleTokens(amount, receiver));
        }
    }

    @InitiatedBy(MoveToken.class)
    public static class MoveEvolvableFungibleTokenFlowResponder extends FlowLogic<Unit>{

        private FlowSession counterSession;

        public MoveEvolvableFungibleTokenFlowResponder(FlowSession counterSession) {
            this.counterSession = counterSession;
        }

        @Suspendable
        @Override
        public Unit call() throws FlowException {
            // Simply use the MoveFungibleTokensHandler as the responding flow
            return subFlow(new MoveFungibleTokensHandler(counterSession));
        }
    }

    /**
     *  Holder Redeems fungible token issued by issuer. The code below is a demonstration for how to redeem a toke.
     *
     *  Or we have to define an issuance celling for the fungible token,
     *  and you can redeem for the non-fungible asset, the house in this case, when you have all the fungible tokens.
     */
//    @StartableByRPC
//    public static class RedeemHouseFungibleTokenFlow extends FlowLogic<SignedTransaction> {
//
//        private final String symbol;
//        private final Party issuer;
//        private final int quantity;
//
//        public RedeemHouseFungibleTokenFlow(String symbol, Party issuer, int quantity) {
//            this.symbol = symbol;
//            this.issuer = issuer;
//            this.quantity = quantity;
//        }
//
//        @Override
//        @Suspendable
//        public SignedTransaction call() throws FlowException {
//            //get house states on ledger with uuid as input tokenId
//            StateAndRef<FungibleHouseTokenState> stateAndRef = getServiceHub().getVaultService().
//                    queryBy(FungibleHouseTokenState.class).getStates().stream()
//                    .filter(sf->sf.getState().getData().getSymbol().equals(symbol)).findAny()
//                    .orElseThrow(()-> new IllegalArgumentException("FungibleHouseTokenState symbol=\""+symbol+"\" not found from vault"));
//
//            //get the RealEstateEvolvableTokenType object
//            FungibleHouseTokenState evolvableTokenType = stateAndRef.getState().getData();
//
//            //specify how much amount quantity of tokens of type token parameter
//            Amount<TokenType> amount =
//                    new Amount<>(quantity, evolvableTokenType.toPointer(FungibleHouseTokenState.class));
//
//            //call built in redeem flow to redeem tokens with issuer
//            return subFlow(new RedeemFungibleTokens(amount, issuer));
//        }
//    }
}

