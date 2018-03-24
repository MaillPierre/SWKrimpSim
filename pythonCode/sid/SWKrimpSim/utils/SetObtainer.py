import sys

with open(sys.argv[1]) as dataFile:
    lines = dataFile.readlines()

numberSet = set()

for line in lines:
    for number in line.split(" "):
        numberSet.add(int(number))

with open(sys.argv[2], "w") as outputFile:
    for number in sorted(numberSet):
        outputFile.write(str(number))
