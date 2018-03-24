from csv import DictReader
import sys
import ntpath
from xlwt import Workbook, easyxf

data = {}

#setA,setB,sizeA,sizeB,A and B, A-B, B-A

with open(sys.argv[1]) as dataFile:
    dataSheet = DictReader(dataFile, delimiter=',')
    for row in dataSheet:
        if row['setA'] not in data:
            data[row['setA']] = {}
        if row['setB'] not in data[row['setA']]:
            data[row['setA']][row['setB']] = {}

        data[row['setA']][row['setB']]['sizeA'] = row['sizeA']
        data[row['setA']][row['setB']]['sizeB'] = row['sizeB']
        data[row['setA']][row['setB']]['A and B'] = row['A and B']
        data[row['setA']][row['setB']]['A-B'] = row['A-B']
        data[row['setA']][row['setB']]['B-A'] = row['B-A']

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
    dataSheet.write_merge(rowPos, rowPos, colPos, colPos+2, colName,
                          easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    colPos +=3
colPos=1
rowPos+=1
for setId in orderedRowNames:
    dataSheet.write(rowPos, colPos, setId,
                    easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    for otherId in orderedColNames:
        print setId + ','+otherId
        if otherId in data[setId]:
            colPos += 1
            dataSheet.write(rowPos, colPos, data[setId][otherId]['A and B'],
                            easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            colPos += 1
            dataSheet.write(rowPos, colPos, data[setId][otherId]['A-B'],
                            easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            colPos += 1
            dataSheet.write(rowPos, colPos, data[setId][otherId]['B-A'],
                            easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
        else:
            colPos +=3
    rowPos+=1
    colPos=1

book.save(sys.argv[2])

