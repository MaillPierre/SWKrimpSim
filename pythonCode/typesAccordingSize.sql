select sum(prevCodSize == 0.0 and postCodSize != 0) as additions, 
	sum(prevCodSize != 0.0 and postCodSize == 0) as deletions, 
	sum(prevCodSize != 0.0 and postCodSize != 0) as modifications, 
	sum(prevCodSize != 0.0 and postCodSize == 0) as nonRelevant
from updates 
group by ct 