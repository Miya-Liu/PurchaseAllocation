# Best Bang for the Buck: Purchase Allocation Under Access Dependent Pricing Functions
This repository is the implementation of the research work: 

**"Best Bang for the Buck: Purchase Allocation Under Access Dependent Pricing Functions "** [paper](https://tbd) </br>
Mengya Liu, Luis-Daniel Ibáñez, Elena Simperl, Adriane Chapman

**Abstract** Research in data marketplaces has focused on centralization/distribution and guarantees for sellers. This work considers a buyers’ budget and the pricing function of a dataset when performing the dataset discovery
task. Instead of assuming that all sellers assign fixed prices to each data tuple, we analyse the impact of adopting different functions to price data, i.e. pricing functions. We show that most pricing functions are
access-dependent – the price of data depends on the amount of it that is accessed or revealed. We capture this class of pricing functions as access dependent pricing functions (ADPFs) and formulate a purchase allocation
(PA) problem with the above market setting aiming to maximise the utility of the purchased query answers within a budget. Our work proves the problem is NP-Hard and develops two heuristic-based algorithms, 3DDP
and Greedy. We compare our algorithms against a brute-force method on a setup based on nine data sources and ten queries from the FedBench benchmark, using different pricing functions and prices. We find that in
average, 3DDP ties or outperforms Greedy in all queries, but is two to three orders of magnitude slower. We observe that the difference is larger in favour of 3DDP as the access-dependent price approaches the budget
value, but sharply decreases when it surpasses the budget.

---

## Repository Structure
We leverage the project of FedMark ([paper](https://arxiv.org/abs/1808.06298), [code](https://github.com/dice-group/CostFed)) in this implementation.
```bash
├── FedMark/                                  # Implementation of Algorithms and Experiments
    ├── solver/src/main.../serviceMarket             # Implementation of Algorithms
    ├── market/src/main.../serviceMarket             # Implementation of Price Manager and Query Value
    ├── evaluation/src/main.../evaluation            # Implementation of Evaluations
├── PricingFunctionSettings/                  # Pricing Functions of Data Sources for Each Query
├── QueryResults/                             # Allocated Purchases for Each Query
├── input/                                    # Algorithm Inputs -- Queries
├── out/                                      # folder of java artifacts
├── output/                                   # Experiment Outputs
    ├── results                                      # experiment results of configured algorithms
├── bash-updated.sh                           # shell file to run experiments
├── serviceMarket.jar                         # java package generated from FedMark
├── README.md                                 # Project overview and instructions
└── LICENSE                                   # License details
```

---

## Usage

### 1. Using shell file
Download the source code and run the experiment with
```bash
chmod +xv bash-updated.sh
./bash-updated.sh
```

### 2. Using source code
Download the source code, run .jar with parameters:
```bash
java -jar serviceMarket.jar -pf [file of pricing functions] -v [value] -i [query index] -a [algorithm name]
```
NB: algorithm name list is ['3ddp' 'greedy' 'brute'], value list is ['l' 'm' 's' 'e'], examples of pricing function files can be found in **PricingFunctionSettings** folder and queries in **input** foler

---
## Benchmark

### Benchmark Sources
The queries and datasets used in the evaluation are from [FedBench](http://fedbench.fluidops.net/). They can also be downloaded from [LargeRDFBech](https://github.com/AKSW/largerdfbench).

FedBench has 25 queries and 9 related data sources covering the field of Cross Domain Queries (CD), Life Science Queries (LS) and Linked Data (LD). 

### Simulated Competitive Market
To simulate a competitive market, we create overlap between above sources. The approach of generating overlaps has been explained in the paper with details.

---


## Citation

If you use this code in your research, please cite the paper:

Mengya Liu, Luis-Daniel Ibáñez, Elena Simperl, Adriane Chapman. Best Bang for the Buck: Purchase Allocation Under Access Dependent Pricing Functions. 


---

## Acknowledgements

We acknowledge the support of Chinese Scholarship Council and []. 

For questions or feedback, please feel free to open an issue or email us directly!

---

