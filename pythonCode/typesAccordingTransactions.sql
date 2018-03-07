select sum(prevTransactions < postTransactions) as additions, 
	sum(prevTransactions > postTransactions) as deletions, 
	sum(prevTransactions == postTransactions) as modifications
from updates 
group by ct 