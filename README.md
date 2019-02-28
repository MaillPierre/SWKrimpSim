
# SWKrimpSim: Structural similarity of RDF graphs using frequent pattern mining #

## Content of the repository

Source code of the approach for a structural similarity of RDF graphs based on frequent patterns, and its related experiments. In this project, it is included the code for the proposal to evaluate the evolution of a dataset currently submitted to ISWC'18.   

+ The [SWPattern folder](https://github.com/MaillPierre/SWKrimpSim/tree/master/SWPattern) contains the source code for the conversion of RDF datasets and updates to transaction, and their comparison and evaluation using codetables. 

+ The [Scripts folder](https://github.com/MaillPierre/SWKrimpSim/tree/master/scripts) contains the scripts used to retrieve code tables from Vreeken's implementations of KRIMP and SLIM.

+ The [Slim archive](https://github.com/MaillPierre/SWKrimpSim/blob/master/SlimBinSource-20120607mod.tar.gz) contains our modification of Vreeken's SLIM implementation to be able to handle very large number of items (by removing a hard coded limit).

+ The [PythonCode folder](https://github.com/MaillPierre/SWKrimpSim/tree/master/pythonCode) contains the code used for the data analysis relative to our experiments.

## Citation

To cite the structural similiarity measure related to this approach:
```
@inproceedings{Maillot2018, 
    author = {Pierre Maillot and Carlos Bobed}, 
    title = {Measuring Structural Similarity Between RDF Graphs}, 
    booktitle = {Proc. of 33rd ACM/SIGAPP Symposium On Applied Computing (SAC), SWA track}, 
    publisher={ACM}, 
    pages = {1960--1967}, 
    month = {April}, 
    year = {2018}, 
}
```

