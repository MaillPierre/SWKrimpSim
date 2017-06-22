# Semantic Web Krimp Similarity

Project to use the Krimp algorithm to extract a codetable form a RDF base and use it to compress another RDF base. The comparison of the compression rates should gives us a measure of the similarity between the two bases.

1) Extraction of transactions from both RDF bases, either:
	- Transactions based on instance descriptions
	- Transactions based on paths (TBD better)
2) Generation of candidates itemsets from one base
	- Using frequent itemsets - FPGrowth for frequents of FPClose for closed ones
	- using itemset sampling (TBD)
3) Extraction of a codetable from one base
4) Compression of the second base with the codetable
5) Conclusion regarding the two obtained compression rates

Uses SMPF for the some of the frequent itemsets algorithms.
Includes JenaUtils classes.

FOR TEST: Launch with options "-file iswc-2013-complete.nt -otherFile iswc-2013-complete.nt -rank1"

TODO :
- Implement KRIMP
	- Pruning TBD

TESTS :
- Comparison with official KRIMP
- Comparison with different codifications 
- Comparison with different scales of data
- Comparison of partitions of the same base, of different sizes (10% 90%, etc.)
- Domain-based comparison

DONE : 
- Hide the passage through files of SMPF to passe only transactions, transactions indexes and Itemsets between classes (14/06/17)
- Implement KRIMP
	- Cover DONE
		- Usage seems to work
	- Compression rate of a Codetable
	- KRIMP (22/06/2017)

Idea :
- Apply pattern sampling to the maximal frequent patterns ?
	- Loosing the support information -> use weighting technique of pattern sampling -> need to retrieve number of apparition of each element of the maximal itemsets