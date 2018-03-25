from csv import DictReader
import sys
import ntpath
from xlwt import Workbook, easyxf

data = {}

#CTBase;CTCompared;dist

with open(sys.argv[1]) as dataFile:
    dataSheet = DictReader(dataFile, delimiter=';')
    for row in dataSheet:
        if row['CTBase'] not in data:
            data[row['CTBase']] = {}
        data[row['CTBase']][row['CTCompared']] = float(row['dist'])

book = Workbook(style_compression=2)
dataSheet = book.add_sheet("data")

rowPos = 1
colPos = 1
setColNames = set()
orderedRowNames = sorted(data.keys())
print orderedRowNames
for setId in orderedRowNames:
    for id in data[setId].keys():
        setColNames.add(id)
    print len(setColNames)
orderedColNames = sorted(setColNames)

print orderedColNames


colPos = 2
for colName in orderedColNames:
    dataSheet.write(rowPos, colPos, colName,
                          easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    colPos +=1
colPos=1
rowPos+=1
for setId in orderedRowNames:
    dataSheet.write(rowPos, colPos, setId,
                    easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    for otherId in orderedColNames:
        if otherId in data[setId]:
            colPos += 1
            dataSheet.write(rowPos, colPos, data[setId][otherId],
                            easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
        else:
            colPos +=1
    rowPos+=1
    colPos=1

book.save(sys.argv[2])

