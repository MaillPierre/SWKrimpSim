import csv
from xlwt import Workbook, easyxf
import sys

if __name__ == "__main__":

	book = Workbook(style_compression=2)
	cohesion_sheet = book.add_sheet ("cohesion")
	cohesion_delta_sheet = book.add_sheet ("cohesion_delta")
	distance_sheet = book.add_sheet ("distance")
	time_sheet = book.add_sheet("times")

	row_cohesion = 1
	row_cohesion_delta = 1
	row_distance = 1
	row_times = 1

	# we prepare the data_sheet 
	cohesion_sheet.write(0,0, 'updateID')
	cohesion_sheet.write(0,1, 'concept')
	cohesion_sheet.write(0,2, 'disruption')
	cohesion_sheet.write(0,3, 'SSE')
	cohesion_sheet.write(0,4, 'meanEuclideanDist')
	cohesion_sheet.write(0,5, 'meanCosDist')
	cohesion_sheet.write(0,6, '#inst')

	cohesion_delta_sheet.write(0,0, 'updateID')
	cohesion_delta_sheet.write(0,1, 'concept')
	cohesion_delta_sheet.write(0,2, 'disruption')
	cohesion_delta_sheet.write(0,3, 'SSE')
	cohesion_delta_sheet.write(0,4, 'meanEuclideanDist')
	cohesion_delta_sheet.write(0,5, 'meanCosDist')
	cohesion_delta_sheet.write(0,6, '#inst')

	distance_sheet.write(0,0, 'updateID')
	distance_sheet.write(0,1, 'instance')
	distance_sheet.write(0,2, 'concept') 
	distance_sheet.write(0,3, 'disruption')
	distance_sheet.write(0,4, 'euclidean_dist_centroid') 
	distance_sheet.write(0,5, 'prev_euclidean_dist_centroid') 
	distance_sheet.write(0,6, 'delta_euc_dist_centroid') 
	distance_sheet.write(0,7, 'cos_dist_centroid') 
	distance_sheet.write(0,8, 'prev_cos_dist_centroid') 
	distance_sheet.write(0,9, 'delta_cos_dist_centroid') 

	time_sheet.write(0,0, 'updateID')
	time_sheet.write(0,1, 'disruption')
	time_sheet.write(0,2, 'loadTime')
	time_sheet.write(0,3, 'trainingTime')
	time_sheet.write(0,4, 'cohesionTime')
	time_sheet.write(0,5, 'measuresTime')

	for i in range(1,6): 
		with open('list-0.'+str(i)+'-100upd.txt', 'r') as input_file: 
			current_file_list = input_file.read().splitlines()
			
		for current_file_base in current_file_list: 
			if current_file_base.endswith('0'):
				# we have 4 files to process
				# -accum-False-cohesion.csv
				# -accum-False-cohesion_deltas.csv
				# -accum-False-distances.csv
				# -accum-False-times.csv
				with open(current_file_base+'-accum-False-cohesion.csv', 'r') as cohesion_file: 
					cohesion_values = cohesion_file.read().splitlines()		
				
				for cv in cohesion_values: 
					values = cv.split(';')
					cohesion_sheet.write(row_cohesion, 0, current_file_base)
					cohesion_sheet.write(row_cohesion, 1, values[0]) 
					cohesion_sheet.write(row_cohesion, 2, float('0.'+str(i)))
					cohesion_sheet.write(row_cohesion, 3, values[1])
					cohesion_sheet.write(row_cohesion, 4, values[2])
					cohesion_sheet.write(row_cohesion, 5, values[3])
					cohesion_sheet.write(row_cohesion, 6, values[4])
					row_cohesion += 1
				
				with open(current_file_base+'-accum-False-cohesion_deltas.csv', 'r') as cohesion_delta_file: 
					cohesion_delta_values = cohesion_delta_file.read().splitlines()
				
				for cv in cohesion_delta_values: 
					values = cv.split(';')
					cohesion_delta_sheet.write(row_cohesion_delta, 0, current_file_base)
					cohesion_delta_sheet.write(row_cohesion_delta, 1, values[0]) 
					cohesion_delta_sheet.write(row_cohesion_delta, 2, float('0.'+str(i)))
					cohesion_delta_sheet.write(row_cohesion_delta, 3, values[1])
					cohesion_delta_sheet.write(row_cohesion_delta, 4, values[2])
					cohesion_delta_sheet.write(row_cohesion_delta, 5, values[3])
					cohesion_delta_sheet.write(row_cohesion_delta, 6, values[4])
					row_cohesion_delta += 1
				
				with open(current_file_base+'-accum-False-distances.csv', 'r') as distances_file: 
					distance_values = distances_file.read().splitlines()
							
				for dv in distance_values: 
					values = dv.split(';')
					distance_sheet.write(row_distance,0, current_file_base)
					distance_sheet.write(row_distance,1, values[0])
					distance_sheet.write(row_distance,2, values[1]) 
					distance_sheet.write(row_distance,3, float('0.'+str(i)))
					distance_sheet.write(row_distance,4, values[2]) 
					distance_sheet.write(row_distance,5, values[3]) 
					distance_sheet.write(row_distance,6, values[4]) 
					distance_sheet.write(row_distance,7, values[5]) 
					distance_sheet.write(row_distance,8, values[6]) 
					distance_sheet.write(row_distance,9, values[7]) 
					row_distance += 1
			
				with open(current_file_base+'-accum-False-times.csv', 'r') as times_file: 
					time_values = times_file.read().splitlines()
					
				times = []
				for cv in time_values: 
					values = cv.split(';')
					times.append(values[1])		
				time_sheet.write(row_times, 0, current_file_base) 
				time_sheet.write(row_times, 1, float('0.'+str(i)))
				time_sheet.write(row_times, 2, times[0])
				time_sheet.write(row_times, 3, times[1])
				time_sheet.write(row_times, 4, times[2])
				time_sheet.write(row_times, 5, times[3])
				row_times += 1
			
	book.save(sys.argv[1])