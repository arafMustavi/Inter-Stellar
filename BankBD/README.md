# fungible and nonfungible realestate token sample CorDapp 

This CorDapp serves as a basic example to create, issue, and move [Fungible](https://training.corda.net/libraries/tokens-sdk/#fungibletoken) tokens in Corda utilizing the Token SDK. In this specific fungible token sample, we will not 
talk about the redeem method of the Token SDK because the redeem process will take the physical asset off the [ledger](https://training.corda.net/prepare-and-discover/design-corda/#orchestration-and-ledger-layers) and destroy the token. Thus, this sample will be a 
simple walk though of the creation, issuance, and transfer of the tokens.



## Concepts


### Flows

There are a few flows that enable this project.


## Pre-Requisites

See https://docs.corda.net/getting-set-up.html.

For a brief introduction to Token SDK in Corda, see https://medium.com/corda/introduction-to-token-sdk-in-corda-9b4dbcf71025

## Usage

### Running the CorDapp

Open a terminal and go to the project root directory and type: (to deploy the nodes using bootstrapper)
```
./gradlew clean deployNodes
```
Then type: (to run the nodes)
```
./build/nodes/runnodes
```

### Interacting with the nodes

#### Shell

When started via the command line, each node will display an interactive shell:

    Welcome to the Corda interactive shell.
    Useful commands include 'help' to see what is available, and 'bye' to shut down the node.

    Tue July 09 11:58:13 GMT 2019>>>

You can use this shell to interact with your node.


Create taka on the ledger using CentralBank's terminal

    flow start CreateToken symbol: taka, valuation: 100000

This will create a linear state of type HouseTokenState in CentralBank's vault

CentralBank will now issue some tokens to BankA. run below command via CentralBank's terminal.

```
flow start IssueToken currency: taka, quantity: 300, receiver: BankA
flow start IssueToken currency: taka, quantity: 300, receiver: BankB

```
Now at BankA's terminal, we can check the tokens by running:
```
flow start GetTokenBalance currency: taka
```
Since BankA now has 300 tokens, Move tokens to BankB from BankA's terminal

```
flow start MoveToken currency: taka, receiver: BankB, quantity: 170
flow start MoveToken currency: taka, receiver: BankA, quantity: 100
```
You can now view the number of Tokens held by both the Buyer and the friend by executing the following Query flow in their respective terminals.
```
flow start GetTokenBalance currency: taka
```
