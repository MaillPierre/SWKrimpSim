###############################################################################
# File: obtainModel.py
# Author: Carlos Bobed
# Date: June 2019
# Comments: script to load and train an rdf2vec model, adapted from 
# 		RDF2Vec original code 
#		All the files with the random walks must be in an isolated directory
# 		which is passed as argv[1]
# Modifications:
###############################################################################

import gensim, logging, os, sys, gzip
import time 

logging.basicConfig(format='%(asctime)s : %(levelname)s : %(message)s',filename='word2vec.out', level=logging.INFO)

class MySentences(object):
    def __init__(self, dirname):
        self.dirname = dirname
 
    def __iter__(self):
        for fname in os.listdir(self.dirname):
            try:
                for line in open(os.path.join(self.dirname, fname), mode='rt', encoding='UTF-8'): 
                #for line in gzip.open(os.path.join(self.dirname, fname), mode='rt'):
                    line = line.rstrip('\n')
                    words = line.split("->")
                    yield words
            except Exception:
                print("Failed reading file:")
                print(fname)
				
###### MAIN #######
if __name__ == "__main__":
	dir_name = sys.argv[1]
	dim = int(sys.argv[2]) 
	win = int(sys.argv[3])
	epochs = int(sys.argv[4]) 
	out_name = sys.argv[5]
	
	sentences = MySentences(dir_name)
	
	model = gensim.models.Word2Vec(size=dim, workers=12, window=win, sg=1, negative=15, iter=epochs)
	model.build_vocab(sentences, progress_per=10000)
	start_time = time.time()
	model.train(sentences, total_examples=model.corpus_count, epochs=model.epochs, report_delay=1)
	end_time = time.time()
	
	model.save(out_name)
	with open(out_name+'.time', 'w') as out: 
		print (str(end_time-start_time))
		out.write(' time to train: '+str(end_time-start_time))
	