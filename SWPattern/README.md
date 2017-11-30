# Semantic Web Krimp Similarity

Project to use the Krimp algorithm to extract a codetable form a RDF base and use it to compress another RDF base. The comparison of the compression rates should gives us a measure of the similarity between the two bases.

1) Extraction of transactions from both RDF bases, either:
	- Transactions based on instance descriptions
		- Property based: presence of properties in the in ou out-going triples
		- Property-class based: property-based plus presence of properties linked to instances of classes in the in ou out-going triples
2) Extraction of a codetable from one base is done using script based on Vreeken et al. implementation
3) The similarity measure is given from the codetables and the datasets
	- Regular: based on only the size on the data
	- Using length: Based on the size of the data and the size of the codetable
	- 

Mains:
- BigDataTransactionExtractorMain: Data extractor adapted to big files, do several pass over the file depending on the conversion
- OrientedMeasuresCalculator: Measure of the structural similarity of one dataset compared to another
- TestMeasures: (Test class) Measure on the structural direction in both direction
