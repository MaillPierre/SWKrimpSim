import sys

with open(sys.argv[1]) as dataFile:
    lines = dataFile.readlines()

numberSet = {}

for line in lines:
    for number in line.split(" "):
        item = int(number)
        if item in numberSet:
            numberSet[item] = numberSet[item] + 1
        else:
            numberSet[item] = 1
with open(sys.argv[2], "w") as outputFile:
    for number in sorted(numberSet):
        outputFile.write(str(number) + ' ' + str(numberSet[number]))
