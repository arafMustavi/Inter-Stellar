# Project Inter-Stellar
Inter-Bank settlement system ideation and demo for the Banks in Bangladesh

Prototype Built using r3 Corda
---
To Deploy the Network:

```
gradlew.bat deployNodes
```

To run the Network:

```
build\nodes\runnodes.bat
```

To check all the flows that started with the node, Just go to any of the four terminal and type:

`flow list`

To issue a Token Flow:

```

flow list

flow 

start TokenIssueFlowInitiator owner: BankA, amount: 5000000000 

run vaultQuery contractStateType: bootcamp.TokenState

```