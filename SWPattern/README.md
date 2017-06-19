# Semantic Web Krimp Similarity

Project to use the Krimp algorithm to extract a codetable form a RDF base and use it to compress another RDF base. The comparison of the compression rates should gives us a measure of the similarity between the two bases.

1) Extraction of transactions from both RDF bases, either:
	- Transactions based on instance descriptions
	- Transactions based on paths (TBD better)
2) Generation of candidates itemsets from one base
	- Using frequent itemsets - FPGrowth for frequents of FPClose for closed ones
	- using itemset sampling (TBD)
3) Extraction of a codetable from one base (TBD)
4) Compression of the second base with the codetable (TBD)
5) Conclusion regarding the two obtained compression rates (TBD)

Uses SMPF for the some of the frequent itemsets algorithms.
Includes JenaUtils classes.

TODO:
- Implement KRIMP
	- Cover DONE
		- Usage seems to work
	- Compression rate of a Codetable TBD
	- KRIMP TBD
	- Pruning TBD

DONE: 
- Hide the passage through files of SMPF to passe only transactions, transactions indexes and Itemsets between classes (14/06/17)

Idea:
- Apply pattern sampling to the maximal frequent patterns ?
	- Loosing the support information -> use weighting technique of pattern sampling -> need to retrieve number of apparition of each element of the maximal itemsets