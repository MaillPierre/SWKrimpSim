import sys
import os.path
import operator

items = {}
totalSize = 0

with open(sys.argv[1]) as dataFileOne:
    lines = dataFileOne.readlines()
    for line in lines:
        if "" != line:
            lineSplit = line.split(" ")
            items[int(lineSplit[0])] = int(lineSplit[1])
            totalSize += int(lineSplit[1])


sortedItems = sorted(items.items(), key=operator.itemgetter(1))

targetPercentage = float(sys.argv[2])
if targetPercentage>1.0:
    targetPercentage /= 100.0;

targetSize = totalSize * targetPercentage

currentSize = 0

for id in sortedItems:
    if currentSize<targetSize:
        currentSize+=items[id]
    else:
        print 'limit reached at '+str(currentSize/targetSize)
        print 'current usage: '+str(items[id])
        break
