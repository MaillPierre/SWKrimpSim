import gensim, logging, os, gzip
from rdflib import Graph, Literal, RDF, URIRef
import numpy as np
import sys
import pickle 
import time 

# python evolutionCalculator.py test.txt  c:/Users/CBobed/workingDir/projects/graphs/UniversityDatasets/Universities0-9_complete.nt noAccum  prev_cohesion-LUBM-200v-200w-5-5.pickle LUBM-200v-200w-5-5.model 


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

if __name__ == '__main__': 
	
	start_total_time = time.time() 
	
	update_list_filename = sys.argv[1]
	graph_filename = sys.argv[2]
	# Do we start from scratch always? 
	accumulative = sys.argv[3] == 'accum'
	cohesion_filename = sys.argv[4]
	model_filename = sys.argv[5]

	with open(update_list_filename, 'r') as update_file_list: 
		update_dirs = update_file_list.read().splitlines()
		for dir in update_dirs: 
			print (dir)
	
	#reading the original data
	g=Graph()
	g.parse(graph_filename, format="nt")
	
	concepts=set()
	for sbj,obj in g.subject_objects(RDF.type): 
		concepts.add(obj)
		
	print (str(len(update_dirs)) + ' to be processed ') 
	prev_cohesion_info = pickle.load(open(cohesion_filename, 'rb'))
	if accumulative: 
		start_load = time.time()
		modelLoaded = gensim.models.Word2Vec.load(model_filename) 
		load_time = time.time() - start_load 
		
	for i in range (len(update_dirs)): 
	
		out_cohesion_filename = update_dirs[i]+'-accum-'+str(accumulative)+'-cohesion.csv'
		out_cohesion_deltas_filename = update_dirs[i]+'-accum-'+str(accumulative)+'-cohesion_deltas.csv'
		out_distances_filename = update_dirs[i]+'-accum-'+str(accumulative)+'-distances.csv'
		out_times_filename = update_dirs[i]+'-accum-'+str(accumulative)+'-times.csv'
		print ('processing '+update_dirs[i]+'...'+str(update_dirs[i].endswith('0')))
		# it's an actual update that we have to calculate 
		if update_dirs[i].endswith('0'): 
		
			with open(update_dirs[i]+'.affected', 'r') as affected_file: 
				affected_resources_plain = affected_file.read().splitlines()
			
			sentences = MySentences(update_dirs[i])
			
			if not accumulative: 
				#reading the rdf2vec model passed as argv[2]
				start_load = time.time()
				modelLoaded = gensim.models.Word2Vec.load(model_filename) 
				load_time = time.time() - start_load
			
			# we get the initial vectors of the affected resources
			start_training = time.time()
			affected_resources = {}
			for af in affected_resources_plain: 
				try: 
					aux_vect = modelLoaded.wv.get_vector(af)
					affected_resources[af] = {}
					affected_resources[af]['vector'] = aux_vect
					affected_resources[af]['types'] = []
					for s,p,o in g.triples ( (URIRef(af), RDF.type, None) ): 
						print (str(s) + " a " +str(o))
						affected_resources[af]['types'].append(str(o))
				except KeyError: 
					print (af + " not in vocab") 
			# we update the model
			print ('building ...')
			modelLoaded.build_vocab(sentences, update=True)
			print ('training ...')
			modelLoaded.train(sentences, total_examples=modelLoaded.corpus_count, epochs=5, report_delay=1)
			# We don't save the model 
			# We calculate the different measures 
			training_time = time.time()-start_training 
			
			start_cohesion = time.time()
			cohesion_info ={}
			for obj in concepts:
				#print ('processing '+str(obj)+' ...')
				instances_set = set()
				instances_list = []
				OOV = 0
				for instance in g.subjects(RDF.type, obj): 
					try: 
						#beware, otherwise you are passing the URIRef, not the actual word
						if not instance in instances_set: 
							instances_list.append(modelLoaded.wv.get_vector(str(instance)))
							instances_set.add(instance)
					except KeyError: 
						OOV += 1
				centroid = np.mean(instances_list, axis=0)
				cohesion_info[str(obj)] = {}
				cohesion_info[str(obj)]['centroid'] = centroid
				accum = 0.0
				accum_cos_dist = 0.0
				accum_dist = 0.0
				for instance_vector in instances_list: 
					accum += sum((instance_vector-cohesion_info[str(obj)]['centroid'])**2)
					accum_cos_dist += np.dot(instance_vector, cohesion_info[str(obj)]['centroid'])/(np.linalg.norm(instance_vector) * np.linalg.norm(cohesion_info[str(obj)]['centroid'])) 
					accum_dist += np.linalg.norm(instance_vector-cohesion_info[str(obj)]['centroid'])
					
				cohesion_info[str(obj)]['SSE'] = accum
				cohesion_info[str(obj)]['#inst'] = len(instances_list)
				if len(instances_list) != 0: 
					cohesion_info[str(obj)]['meanEuclideanDist'] = accum_dist / len(instances_list)
					cohesion_info[str(obj)]['meanCosDist'] = accum_cos_dist/len(instances_list)
				else: 
					cohesion_info[str(obj)]['meanEuclideanDist'] = float('nan')
					cohesion_info[str(obj)]['meanCosDist'] = float('nan')
			cohesion_time = time.time()-start_cohesion
			# with cohesionCalculator we have the original values
			# we have to compare them to these ones
			with open(out_cohesion_filename, 'w') as out: 
				print ('writing '+out_cohesion_filename)
				for cpt in cohesion_info.keys(): 
					out.write(str(cpt)+';'+str(cohesion_info[cpt]['SSE'])+';'+
						str(cohesion_info[cpt]['meanEuclideanDist'])+';'+
						str(cohesion_info[cpt]['meanCosDist'])+';'+
						str(cohesion_info[cpt]['#inst'])+'\n')
				out.flush()
				
			with open(out_cohesion_deltas_filename, 'w') as out: 
				print ('writing '+out_cohesion_deltas_filename)
				for cpt in cohesion_info.keys(): 
					out.write(str(cpt)+';'+str(cohesion_info[cpt]['SSE']-prev_cohesion_info[cpt]['SSE'])+';'+ str(cohesion_info[cpt]['meanEuclideanDist']-prev_cohesion_info[cpt]['meanEuclideanDist'])+';'+
						str(cohesion_info[cpt]['meanCosDist']-prev_cohesion_info[cpt]['meanCosDist'])+';'+str(cohesion_info[cpt]['#inst'])+'\n')
				out.flush()
			
			start_measures = time.time()
			# calculate the measures at affected resource level
			for af in affected_resources: 
				current_vect = modelLoaded.wv.get_vector(af)
				prev_vect = affected_resources[af]['vector']
				affected_resources[af]['data']={}	
				for t in range(len(affected_resources[af]['types'])): 
					aux_type = affected_resources[af]['types'][t]
					prev_euc_dist_centroid = np.linalg.norm(prev_vect-prev_cohesion_info[aux_type]['centroid'])
					euc_dist_centroid = np.linalg.norm(current_vect-cohesion_info[aux_type]['centroid'])
					
					prev_cos_dist_centroid = np.dot(prev_vect, prev_cohesion_info[aux_type]['centroid']) / (np.linalg.norm(prev_vect)*np.linalg.norm(prev_cohesion_info[aux_type]['centroid']))
					cos_dist_centroid = np.dot(current_vect, cohesion_info[aux_type]['centroid']) / (np.linalg.norm(current_vect)*np.linalg.norm(cohesion_info[aux_type]['centroid']))
					
					affected_resources[af]['data'][aux_type] = {}
					affected_resources[af]['data'][aux_type]['prev_euc_dist_centroid'] = prev_euc_dist_centroid
					affected_resources[af]['data'][aux_type]['euc_dist_centroid'] = euc_dist_centroid
					affected_resources[af]['data'][aux_type]['prev_cos_dist_centroid'] = prev_cos_dist_centroid
					affected_resources[af]['data'][aux_type]['cos_dist_centroid'] = cos_dist_centroid
					affected_resources[af]['data'][aux_type]['delta_euc_dist_centroid'] = euc_dist_centroid -  prev_euc_dist_centroid
					affected_resources[af]['data'][aux_type]['delta_cos_dist_centroid'] = cos_dist_centroid - prev_cos_dist_centroid
			measures_time = time.time() - start_measures	
			with open(out_distances_filename, 'w') as out: 
				print ('writing '+out_distances_filename)
				for af in affected_resources: 
					for t in affected_resources[af]['data']: 
						out.write(af+';'+t+
							';'+str(affected_resources[af]['data'][t]['euc_dist_centroid'])+
							';'+str(affected_resources[af]['data'][t]['prev_euc_dist_centroid'])+
							';'+str(affected_resources[af]['data'][t]['delta_euc_dist_centroid'])+
							';'+str(affected_resources[af]['data'][t]['cos_dist_centroid'])+
							';'+str(affected_resources[af]['data'][t]['prev_cos_dist_centroid'])+
							';'+str(affected_resources[af]['data'][t]['delta_cos_dist_centroid'])+'\n')
			with open(out_times_filename, 'w') as out: 
				out.write('writing '+out_times_filename)
				out.write('loadTime;'+str(load_time)+'\n') 
				out.write('trainingTime;'+str(training_time)+'\n')
				out.write('cohesionTime;'+str(cohesion_time)+'\n') 
				out.write('measures_time;'+str(measures_time))
	print ("Took ..." +str(time.time()-start_total_time))