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

FOR TEST: 
- To get all option, launch with -help
- Launching with one RDF File: -inputRDF <file>
- with two RDF files to be compared:  -inputRDF <file1> -inputOtherRDFFile <file2>
- with one transactions file:  -inputTransaction <file1>
- with two transactions files to be compared:  -inputTransaction <file1> -inputOtherTransaction <file2>
- output the index: -outputConversionIndex <file>
- output the code table: -outputCodeTable


TESTS :
- Comparison with different scales of data
- Domain-based comparison


DONE : 
- Hide the passage through files of SMPF to passe only transactions, transactions indexes and Itemsets between classes (14/06/17)
- Implement KRIMP
	- Cover DONE
		- Usage seems to work
	- Compression rate of a Codetable
	- KRIMP (22/06/2017)
	- Pruning (27/06/2017)
	- KRIMPSLIM (in a dedicated branch for now)
- Conversion from RDF to transaction possible: instance-based, containing either:
	- instance type and property
	- instance type, property and other resource type if existing
	- instance type, property and other resource that in an outlier in/out degree-wise
