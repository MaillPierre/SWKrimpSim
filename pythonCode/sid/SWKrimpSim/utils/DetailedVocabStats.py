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

if not os.path.exists(sys.argv[3]):
    with open(sys.argv[3], "w") as out:
        out.write("setA;setB;sizeA;sizeB;A and B;A-B;B-A\n")

sizeA = 0
for itemId in itemsOne:
    sizeA += itemsOne[itemId]
sizeB = 0
for itemId in itemsTwo:
    sizeB += itemsTwo[itemId]
AAndB = 0
for itemId in itemsOne:
    if itemId in itemsTwo:
        AAndB += min(itemsOne[itemId], itemsTwo[itemId])
AminusB = 0
for itemId in itemsOne:
    if itemId in itemsTwo:
        AminusB += (itemsOne[itemId] - min(itemsOne[itemId], itemsTwo[itemId]))
    else:
        AminusB += itemsOne[itemId]
BminusA = 0
for itemId in itemsTwo:
    if itemId in itemsOne:
        BminusA += (itemsTwo[itemId] - min(itemsOne[itemId], itemsTwo[itemId]))
    else:
        BminusA += itemsTwo[itemId]

with open(sys.argv[3], "a") as out:
    out.write(sys.argv[1]+';'+sys.argv[2]+';'+
              str(sizeA)+';'+str(sizeB)+';'+
              str(AAndB)+';'+
              str(AminusB)+';'+
              str(BminusA)+ '\n')
