select ct, 
	sum(compRatioPrev<compRatioPost) as pre, 
	sum(compRatioPrev=compRatioPost) as equ, 
	sum(compRatioPrev>compRatioPost) as post, 
	count(*) 
from updates
where prevCodSize != 0.0 and postCodSize != 0
group by ct