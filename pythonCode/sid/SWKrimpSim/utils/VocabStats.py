import sys
import os.path

itemsOne = set()
itemsTwo = set()
with open(sys.argv[1]) as dataFileOne: 
    lines = dataFileOne.readlines()
    for line in lines:
        if "" != line:
            itemsOne.add(int(line))
with open(sys.argv[2]) as dataFileTwo:
    lines = dataFileTwo.readlines()
    for line in lines:
        if "" != line:
            itemsTwo.add(int(line))

if not os.path.exists(sys.argv[3]):
    with open(sys.argv[3], "w") as out:
        out.write("setA;setB;sizeA;sizeB;A and B; A-B; B-A\n")

with open(sys.argv[3], "a") as out:
    out.write(sys.argv[1]+';'+sys.argv[2]+';'+
              str(len(itemsOne))+';'+str(len(itemsTwo))+';'+
              str(len(itemsOne.intersection(itemsTwo)))+';'+
              str(len(itemsOne.difference(itemsTwo)))+';'+
              str(len(itemsTwo.difference(itemsOne)))+ '\n')
