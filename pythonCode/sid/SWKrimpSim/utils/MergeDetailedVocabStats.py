import sys
import os.path

itemsOne = {}
itemsTwo = {}
with open(sys.argv[1]) as dataFileOne: 
    lines = dataFileOne.readlines()
    for line in lines:
        if "" != line:
            lineSplit = line.split(" ")
            itemsOne[int(lineSplit[0])] = int(lineSplit[1])
with open(sys.argv[2]) as dataFileTwo:
    lines = dataFileTwo.readlines()
    for line in lines:
        if "" != line:
            lineSplit = line.split(" ")
            itemsTwo[int(lineSplit[0])] = int(lineSplit[1])

keySet = set(itemsOne.keys())
for key in itemsTwo.keys():
    keySet.add(key)

with open(sys.argv[3]) as outputFile:
    for key in sorted(keySet):
        value = 0
        if key in itemsOne:
            value+=itemsOne[key]
        if key in itemsTwo:
            value+=itemsTwo[key]
        outputFile.write(str(key) + ' ' + str(value)+'\n')