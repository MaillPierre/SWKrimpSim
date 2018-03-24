###############################################################################
# File: generateScholarlyDataComparisons.py
# Author: Carlos Bobed
# Date: March 2018
# Comments: script to import the data of the codified transactions into a
#       sqlite database
# Modifications:
###############################################################################

###############################################################################
#HEADERS
# filename;originalDBSize;compressedSize;ratio
#originalCT;comparedCT;comparedDB;ourFormat;measure;measureValue;execTime

from csv import DictReader
import math

import sys
import ntpath
from xlwt import Workbook, easyxf


class RatioLocalHeaders:
    filenameHeader = 'filename'
    originalDBSizeHeader = 'originalDBSize'
    compressedSizeHeader = 'compressedSize'
    ratioHeader = 'ratio'

class MeasuresLocalHeaders:
    originalCTHeader = 'originalCT'
    comparedCTHeader = 'comparedCT'
    comparedDBHeader = 'comparedDB'
    ourFormatHeader = 'ourFormat'
    measureHeader = 'measure'
    measureValueHeader = 'measureValue'
    exectimeHeader = 'execTime'

class FileStrings:
    prefixCT = 'scholarlyOutputs/'
    suffixCT = '-output/ct-latest.ct'

def writeComparisonData(sheet, measuresData, ratiosData, initialRow, initialCol ):

    rowPos = initialRow
    colPos = initialCol

    orderedKeys = sorted(measuresData.keys())
    print orderedKeys
    print sorted(ratiosData.keys())
    colPos += 2
    for key in orderedKeys:
        sheet.write(rowPos,colPos, key, easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        colPos += 1
    rowPos += 1
    for key in orderedKeys:
        colPos = initialCol
        sheet.write(rowPos,colPos, key, easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        colPos += 1
        sheet.write(rowPos,colPos, float(ratiosData[key][RatioLocalHeaders.ratioHeader]),
                    easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
        colPos += 1
        for keyCol in orderedKeys:
            if key == keyCol:
                sheet.write(rowPos, colPos, 1.0,
                            easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            else:
                sheet.write(rowPos, colPos, float(measuresData[key][keyCol][MeasuresLocalHeaders.measureValueHeader]),
                            easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            colPos += 1
        rowPos += 1
    colPos = initialCol
    return (rowPos, colPos)

def writeCrossComparisonData(sheet, measuresData, ratiosData, initialRow, initialCol ):

    rowPos = initialRow
    colPos = initialCol

    orderedKeys = sorted([key for key in measuresData.keys() if "Align" in key])

    for key in orderedKeys:
        colPos = initialCol+2
        compKey = key.replace("Align", '')

        if (compKey in measuresData[key]):
            sheet.write(rowPos, colPos, compKey, easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
            colPos+=1
            sheet.write(rowPos, colPos, key, easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
            rowPos +=1

            colPos = initialCol
            sheet.write(rowPos, colPos, compKey, easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
            colPos +=1
            sheet.write(rowPos, colPos, float(ratios[compKey][RatioLocalHeaders.ratioHeader]),
                        easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            colPos +=1
            sheet.write(rowPos, colPos, 1.0,easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25') )
            colPos +=1
            sheet.write(rowPos, colPos, measuresData[compKey][key][MeasuresLocalHeaders.measureValueHeader],
                        easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            rowPos +=1

            colPos = initialCol
            sheet.write(rowPos, colPos, key,
                        easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
            colPos += 1

            sheet.write(rowPos, colPos, float(ratios[key][RatioLocalHeaders.ratioHeader]),
                        easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            colPos += 1
            sheet.write(rowPos, colPos, measuresData[key][compKey][MeasuresLocalHeaders.measureValueHeader],
                        easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            colPos += 1
            sheet.write(rowPos, colPos, 1.0,
                        easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            rowPos += 2
        else:
            sheet.write(rowPos, colPos, key + ' skipped')
            rowPos +=2
    return (rowPos, initialCol)

###### MAIN #######

if __name__ == "__main__":
    print sys.argv
    if (len(sys.argv) != 4):
        print ("uso: python generateScholarlyDataComparisons.py inputCSVFilenameRatios inputCSVMeasureRatios outFile")
        sys.exit()

    ratios = {}
    with open(sys.argv[1]) as dataFile:
        dataSheet = DictReader(dataFile, delimiter=';')

        for row in dataSheet:
            # if "Align" in row[RatioLocalHeaders.filenameHeader]:
            #     alignedDictRatios[RatioLocalHeaders.filenameHeader] = row
            # else:
            #     completeDictRatios[RatioLocalHeaders.filenameHeader] = row
            ratios[row[RatioLocalHeaders.filenameHeader]] = row

    measures = {}
    with open(sys.argv[2]) as dataFile:
        dataSheet = DictReader(dataFile, delimiter=';')
        for row in dataSheet:
            auxOriginalCT = row[MeasuresLocalHeaders.originalCTHeader].replace(FileStrings.prefixCT, '').replace(FileStrings.suffixCT,'')
            auxComparedCT = row[MeasuresLocalHeaders.comparedCTHeader].replace(FileStrings.prefixCT, '').replace(FileStrings.suffixCT,'')

            if auxOriginalCT not in measures:
                measures[auxOriginalCT] = {}
            measures[auxOriginalCT][auxComparedCT] = row

    spreadSheetFilename = sys.argv[2]
    book = Workbook(style_compression=2)
    print "processing data table ... "
    outputSheet = book.add_sheet("CompleteMatrix")

    position = (0,0)
    position = writeComparisonData(outputSheet, measures, ratios, position[0], position[1])

    ratiosComplete = {k : v  for k,v in ratios.items() if not "Align" in k}
    ratiosAlign = {k:v for k,v in ratios.items() if "Align" in k}

    measuresComplete = { k : v for k, v in measures.items() if not "Align" in k}
    measuresAlign = { k : v for k, v in measures.items() if "Align" in k}

    for key in measuresComplete:
        auxDict = measuresComplete[key]
        measuresComplete[key] = { k : v for k, v in auxDict.items() if not "Align" in k }

    for key in measuresAlign:
        auxDict = measuresAlign[key]
        measuresAlign[key] = { k : v for k, v in auxDict.items() if "Align" in k }

    outputSheet = book.add_sheet("SeparatedMatrix")
    position = (0,0)
    position = writeComparisonData(outputSheet, measuresComplete, ratiosComplete, position[0], position[1])
    position = writeComparisonData(outputSheet, measuresAlign, ratiosAlign, position[0]+1, position[1])

    outputSheet = book.add_sheet("Cross Comparisons")
    position = (0,0)
    writeCrossComparisonData(outputSheet, measures, ratios, position[0], position[1])

    book.save(sys.argv[3])

