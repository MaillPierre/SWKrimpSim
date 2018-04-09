# Semantic Web Krimp Similarity

Project to use the KRIMP and SLIM algorithms to extract a codetable form a RDF base and use it to compress another RDF base. The comparison of the compression rates gives us a measure of the similarity between the two bases.

The [scripts](https://github.com/MaillPierre/SWKrimpSim/tree/master/SWPattern/scripts) and [ExperimentalResults](https://github.com/MaillPierre/SWKrimpSim/tree/master/SWPattern/experimentalResults) folders contain respectively the scripts used, and the results of the experiments realized for the experiments presented in SAC2018.

Mains:
- BigDataTransactionExtractorMain: Data extractor adapted to big files, do several pass over the file depending on the conversion
- OrientedMeasuresCalculator: Measure of the structural similarity of one dataset compared to another
- TestMeasures: (Test class) Measure on the structural direction in both direction
