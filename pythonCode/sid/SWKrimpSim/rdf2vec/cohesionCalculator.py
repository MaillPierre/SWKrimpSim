from rdflib import Graph, Literal, RDF
import sys
from rdflib.namespace import OWL
import gensim, logging, os, gzip
import numpy as np
import pickle

# python cohesionCalculator.py c:/Users/CBobed/workingDir/projects/graphs/UniversityDatasets/Universities0-9_complete.nt LUBM-200v-200w-5-5.model testCohesion.csv testMatrix.csv prev_cohesion-LUBM-200v-200w-5-5.pickle

###### MAIN #######
if __name__ == "__main__":

	#reading the original data
	g=Graph()
	g.parse(sys.argv[1], format="nt")
	
	concepts=set()
	for sbj,obj in g.subject_objects(RDF.type): 
		concepts.add(obj)
		
	#reading the rdf2vec model passed as argv[2]
	modelLoaded = gensim.models.Word2Vec.load(sys.argv[2])    
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
		print (str(len(instances_list)) + ' instances in '+ str(obj))
		print (str(OOV) + ' out of vocab for '+str(obj))
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
	with open(sys.argv[3], 'w') as out: 
		for cpt in cohesion_info.keys(): 
			out.write(str(cpt)+';'+str(cohesion_info[cpt]['SSE'])+';'+
				str(cohesion_info[cpt]['meanEuclideanDist'])+';'+
				str(cohesion_info[cpt]['meanCosDist'])+';'+
				str(cohesion_info[cpt]['#inst'])+'\n')
		out.flush()
	
	pickle.dump(cohesion_info, open(sys.argv[5], 'wb'))
	
	dist_matrix = {}
	cos_dist_matrix = {}
	for cpt1 in (sorted(cohesion_info.keys())): 
		dist_matrix[cpt1] = {}
		cos_dist_matrix[cpt1] = {}
		for cpt2 in (sorted(cohesion_info.keys())): 
			if cohesion_info[cpt1]['#inst'] != 0 and cohesion_info[cpt2]['#inst'] != 0: 
				dist_matrix[cpt1][cpt2] = np.linalg.norm(cohesion_info[cpt1]['centroid']-cohesion_info[cpt2]['centroid'])
				cos_dist_matrix[cpt1][cpt2] = np.dot(cohesion_info[cpt1]['centroid'], cohesion_info[cpt2]['centroid']) / (np.linalg.norm(cohesion_info[cpt1]['centroid'])*np.linalg.norm(cohesion_info[cpt2]['centroid']))
			else: 
				dist_matrix[cpt1][cpt2] = float('nan')
				cos_dist_matrix[cpt1][cpt2] = float('nan')
	
	with open(sys.argv[4], 'w') as out: 
		for cpt in sorted(dist_matrix.keys()): 
			out.write(';'+str(cpt))
		out.write('\n') 
		for cpt1 in sorted(dist_matrix.keys()): 
			out.write(str(cpt1))
			for cpt2 in sorted(dist_matrix[cpt1]): 
				out.write(';'+str(dist_matrix[cpt1][cpt2]))
			out.write('\n')
			
		for cpt in sorted(cos_dist_matrix.keys()): 
			out.write(';'+str(cpt))
		out.write('\n') 
		for cpt1 in sorted(cos_dist_matrix.keys()): 
			print (cpt1)
			out.write(str(cpt1))
			for cpt2 in sorted(cos_dist_matrix[cpt1]): 
				print ('cpt2:'+str(cpt2))
				out.write(';'+str(cos_dist_matrix[cpt1][cpt2]))
			out.write('\n')
		out.flush()

	