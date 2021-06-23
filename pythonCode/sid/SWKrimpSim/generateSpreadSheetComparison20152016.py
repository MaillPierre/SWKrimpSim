###############################################################################
# File: generateSpreadSheetComparison20152016.py
# Author: Carlos Bobed
# Date: March 2018
# Comment: script to build the spreadsheets with the data grouped in
#       different ways
# Modifications:
###############################################################################

import sys
from utils.CSVHeaders import BasicHeaders
import sqlite3 as lite
from xlwt import Workbook, easyxf
import math

def obtainExecutionParameters (databaseName, tableName):
    con = lite.connect(databaseName)
    with con:
        con.row_factory = lite.Row
        cur = con.cursor()
        statement = "SELECT DISTINCT "+BasicHeaders.CTTable+" FROM "+tableName
        print statement
        cur.execute(statement)
        parameters = cur.fetchall()
    return parameters

def loadData (databaseName, tablename, block, executionData):
    con = lite.connect(databaseName)
    if (executionData[BasicHeaders.CTTable] not in block):
        block[executionData[BasicHeaders.CTTable]] = {}
    with con:
        con.row_factory = lite.Row
        cur = con.cursor()
        statement = "SELECT * FROM "+tablename+" WHERE "+BasicHeaders.CTTable+" LIKE '"+\
            executionData[BasicHeaders.CTTable]+"'"
        print statement
        cur.execute(statement)
        rows = cur.fetchall()
        for row in rows:
            block[executionData[BasicHeaders.CTTable]][row[BasicHeaders.updateIDTable]] = row

def writeDataGraphGroupedAlpha(sheet, data, alpha):

    rowPos = 0
    colPos = 0
    sheet.write(rowPos, 0, BasicHeaders.FCoverTable)
    rowPos +=1
    #escribo la cabecera de la grafica
    numObjectsList = sorted(data.itervalues().next().keys())
    colPos = 1
    for aux in numObjectsList:
        sheet.write(rowPos, colPos, aux, easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        colPos +=1
    rowPos += 1
    for range in sorted(data):
        colPos = 0
        sheet.write(rowPos, colPos, range, easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        colPos +=1
        for numObjects in sorted(data[range]):

            sheet.write(rowPos,colPos,data[range][numObjects][alpha][BasicHeaders.FCoverTable],easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            colPos +=1
        rowPos+=1

    rowPos +=1
    sheet.write(rowPos, 0, BasicHeaders.execTimeTable)
    rowPos +=1
    #escribo la cabecera de la grafica
    numObjectsList = sorted(data.itervalues().next().keys())
    colPos = 1
    for aux in numObjectsList:
        sheet.write(rowPos, colPos, aux, easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        colPos +=1
    rowPos += 1
    for range in sorted(data):
        colPos = 0
        sheet.write(rowPos, colPos, range, easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        colPos +=1
        for numObjects in sorted(data[range]):
            sheet.write(rowPos,colPos,data[range][numObjects][alpha][BasicHeaders.execTimeTable],easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            colPos +=1
        rowPos+=1


def writeHeaders(sheet):

    sheet.write(0, 0, "occupiedRange", easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(0, 1, "numObjects", easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(0, 2, "alpha", easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(0, 3, "numExecutions", easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(0, 4, "F", easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(0, 5, "execTime", easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))

def writeData(sheet, executionParams, executionData, rowPos):
    sheet.write(rowPos, 0, executionParams[BasicHeaders.occupiedRangeTable] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 1, executionParams[BasicHeaders.numObjectsTable] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, executionParams[BasicHeaders.alphaTable] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, executionData[1], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 4, executionData[2] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 5, executionData[3] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

def writeMatrixData(sheet, compResults, rowPos):

    sheet.write(rowPos, 1, 'Post 2015',
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 2, 'Post Both',
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 3, 'Post 2016',
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    rowPos += 1
    sheet.write(rowPos, 0, 'Prev 2015',
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BasicHeaders.prev2015Post2015Header], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BasicHeaders.prev2015PostBothHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BasicHeaders.prev2015Post2016Header] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

    rowPos += 1
    sheet.write(rowPos, 0, 'Prev Both',
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BasicHeaders.prevBothPost2015Header],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BasicHeaders.prevBothPostBothHeader],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BasicHeaders.prevBothPost2016Header],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

    rowPos += 1
    sheet.write(rowPos, 0, 'Prev 2016',
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BasicHeaders.prev2016Post2015Header], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BasicHeaders.prev2016PostBothHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BasicHeaders.prev2016Post2016Header] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

def writeMatrixDataCross(sheet, compResults, rowPos):

    sheet.write(rowPos, 1, BasicHeaders.beforeHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 2, BasicHeaders.equalHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 3, BasicHeaders.afterHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    rowPos += 1
    sheet.write(rowPos, 0, BasicHeaders.beforeHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BasicHeaders.prevBeforePostBeforeHeader], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BasicHeaders.prevBeforePostBothHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BasicHeaders.prevBeforePostAfterHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

    rowPos += 1
    sheet.write(rowPos, 0, BasicHeaders.equalHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BasicHeaders.prevBothPostBeforeHeader],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BasicHeaders.prevBothPostBothHeader],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BasicHeaders.prevBothPostAfterHeader],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

    rowPos += 1
    sheet.write(rowPos, 0, BasicHeaders.afterHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BasicHeaders.prevAfterPostBeforeHeader], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BasicHeaders.prevAfterPostBothHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BasicHeaders.prevAfterPostAfterHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))


def writeRowData(sheet, compResults, rowPos, rowName):

    sheet.write(rowPos, 1, BasicHeaders.beforeHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 2, BasicHeaders.equalHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 3, BasicHeaders.afterHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    rowPos += 1
    sheet.write(rowPos, 0, rowName,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BasicHeaders.beforeHeader], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BasicHeaders.equalHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BasicHeaders.afterHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))


####### MAIN #######
if __name__ == "__main__":

    print sys.argv

    if (len(sys.argv) != 3):
        print ("uso: python generateSpreadsheetBase.py databaseName spreadSheetFilename")
        sys.exit()

    databaseFilename = sys.argv[1]
    spreadSheetFilename = sys.argv[2]
    book = Workbook(style_compression=2)
    print "processing data table ... "
    dataSheet = book.add_sheet("data")
    parameters = obtainExecutionParameters(databaseFilename, "updates")
    print parameters
    print "writing the parameters"
    rowPos = 1
    percentage = 0
    data = {}
    count = 0
    anyParam = []
    for executionParams in parameters:
        anyParam.append(executionParams[BasicHeaders.CTTable])
        loadData(databaseFilename, "updates", data, executionParams)
    print anyParam

    compResults = {}
    compResults[BasicHeaders.prev2015Post2015Header] = 0
    compResults[BasicHeaders.prev2015PostBothHeader] = 0
    compResults[BasicHeaders.prev2015Post2016Header] = 0

    compResults[BasicHeaders.prevBothPost2015Header] = 0
    compResults[BasicHeaders.prevBothPostBothHeader] = 0
    compResults[BasicHeaders.prevBothPost2016Header] = 0

    compResults[BasicHeaders.prev2016Post2015Header] = 0
    compResults[BasicHeaders.prev2016PostBothHeader] = 0
    compResults[BasicHeaders.prev2016Post2016Header] = 0

    compResultsRatio = {}
    compResultsRatio[BasicHeaders.prev2015Post2015Header] = 0
    compResultsRatio[BasicHeaders.prev2015PostBothHeader] = 0
    compResultsRatio[BasicHeaders.prev2015Post2016Header] = 0

    compResultsRatio[BasicHeaders.prevBothPost2015Header] = 0
    compResultsRatio[BasicHeaders.prevBothPostBothHeader] = 0
    compResultsRatio[BasicHeaders.prevBothPost2016Header] = 0

    compResultsRatio[BasicHeaders.prev2016Post2015Header] = 0
    compResultsRatio[BasicHeaders.prev2016PostBothHeader] = 0
    compResultsRatio[BasicHeaders.prev2016Post2016Header] = 0

    print data.keys()
    print len(data[anyParam[0]])
    print len(data[anyParam[1]])

    compResultsEvol2015 = {}
    compResultsEvol2015[BasicHeaders.beforeHeader] = 0
    compResultsEvol2015[BasicHeaders.equalHeader] = 0
    compResultsEvol2015[BasicHeaders.afterHeader] = 0

    compResultsEvol2016 = {}
    compResultsEvol2016[BasicHeaders.beforeHeader] = 0
    compResultsEvol2016[BasicHeaders.equalHeader] = 0
    compResultsEvol2016[BasicHeaders.afterHeader] = 0

    compResultsCross= {}
    compResultsCross[BasicHeaders.prevBeforePostBeforeHeader] = 0
    compResultsCross[BasicHeaders.prevBeforePostBothHeader] = 0
    compResultsCross[BasicHeaders.prevBeforePostAfterHeader] = 0

    compResultsCross[BasicHeaders.prevBothPostBeforeHeader] = 0
    compResultsCross[BasicHeaders.prevBothPostBothHeader] = 0
    compResultsCross[BasicHeaders.prevBothPostAfterHeader] = 0

    compResultsCross[BasicHeaders.prevAfterPostBeforeHeader] = 0
    compResultsCross[BasicHeaders.prevAfterPostBothHeader] = 0
    compResultsCross[BasicHeaders.prevAfterPostAfterHeader] = 0


    for id in data[anyParam[0]]:

        if data[anyParam[0]][id][BasicHeaders.prevCodSizeHeader] < data[anyParam[1]][id][BasicHeaders.prevCodSizeHeader]:
            if data[anyParam[0]][id][BasicHeaders.postCodSizeHeader] < data[anyParam[1]][id][BasicHeaders.postCodSizeHeader]:
                compResults[BasicHeaders.prev2015Post2015Header] += 1
            elif data[anyParam[0]][id][BasicHeaders.postCodSizeHeader] == data[anyParam[1]][id][BasicHeaders.postCodSizeHeader]:
                compResults[BasicHeaders.prev2015PostBothHeader] += 1
            else:
                compResults[BasicHeaders.prev2015Post2016Header] += 1
        elif data[anyParam[0]][id][BasicHeaders.prevCodSizeHeader] == data[anyParam[1]][id][BasicHeaders.prevCodSizeHeader]:
            if data[anyParam[0]][id][BasicHeaders.postCodSizeHeader] < data[anyParam[1]][id][BasicHeaders.postCodSizeHeader]:
                compResults[BasicHeaders.prevBothPost2015Header] += 1
            elif data[anyParam[0]][id][BasicHeaders.postCodSizeHeader] == data[anyParam[1]][id][BasicHeaders.postCodSizeHeader]:
                compResults[BasicHeaders.prevBothPostBothHeader] += 1
            else:
                compResults[BasicHeaders.prevBothPost2016Header] += 1
        else:
            if data[anyParam[0]][id][BasicHeaders.postCodSizeHeader] < data[anyParam[1]][id][BasicHeaders.postCodSizeHeader]:
                compResults[BasicHeaders.prev2016Post2015Header] += 1
            elif data[anyParam[0]][id][BasicHeaders.postCodSizeHeader] == data[anyParam[1]][id][BasicHeaders.postCodSizeHeader]:
                compResults[BasicHeaders.prev2016PostBothHeader] += 1
            else:
                compResults[BasicHeaders.prev2016Post2016Header] += 1

        if data[anyParam[0]][id][BasicHeaders.compressionRatioPrevTable] < data[anyParam[1]][id][BasicHeaders.compressionRatioPrevTable]:
            if data[anyParam[0]][id][BasicHeaders.compressionRatioPostTable] < data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsRatio[BasicHeaders.prev2015Post2015Header] += 1
            elif data[anyParam[0]][id][BasicHeaders.compressionRatioPostTable] == data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsRatio[BasicHeaders.prev2015PostBothHeader] += 1
            else:
                compResultsRatio[BasicHeaders.prev2015Post2016Header] += 1
        elif data[anyParam[0]][id][BasicHeaders.compressionRatioPrevTable] == data[anyParam[1]][id][BasicHeaders.compressionRatioPrevTable]:
            if data[anyParam[0]][id][BasicHeaders.compressionRatioPostTable] < data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsRatio[BasicHeaders.prevBothPost2015Header] += 1
            elif data[anyParam[0]][id][BasicHeaders.compressionRatioPostTable] == data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsRatio[BasicHeaders.prevBothPostBothHeader] += 1
            else:
                compResultsRatio[BasicHeaders.prevBothPost2016Header] += 1
        else:
            if data[anyParam[0]][id][BasicHeaders.compressionRatioPostTable] < data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsRatio[BasicHeaders.prev2016Post2015Header] += 1
            elif data[anyParam[0]][id][BasicHeaders.compressionRatioPostTable] == data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsRatio[BasicHeaders.prev2016PostBothHeader] += 1
            else:
                compResultsRatio[BasicHeaders.prev2016Post2016Header] += 1

        if data[anyParam[0]][id][BasicHeaders.compressionRatioPrevTable] < data[anyParam[0]][id][BasicHeaders.compressionRatioPostTable]:
            compResultsEvol2015[BasicHeaders.beforeHeader] += 1
        elif data[anyParam[0]][id][BasicHeaders.compressionRatioPrevTable] == data[anyParam[0]][id][BasicHeaders.compressionRatioPostTable]:
            compResultsEvol2015[BasicHeaders.equalHeader] += 1
        else:
            compResultsEvol2015[BasicHeaders.afterHeader] += 1

        if data[anyParam[1]][id][BasicHeaders.compressionRatioPrevTable] < data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
            compResultsEvol2016[BasicHeaders.beforeHeader] += 1
        elif data[anyParam[1]][id][BasicHeaders.compressionRatioPrevTable] == data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
            compResultsEvol2016[BasicHeaders.equalHeader] += 1
        else:
            compResultsEvol2016[BasicHeaders.afterHeader] += 1

        # cross comparison
        if data[anyParam[0]][id][BasicHeaders.compressionRatioPrevTable] < data[anyParam[0]][id][BasicHeaders.compressionRatioPostTable]:
            if data[anyParam[1]][id][BasicHeaders.compressionRatioPrevTable] < data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsCross[BasicHeaders.prevBeforePostBeforeHeader] += 1
            elif data[anyParam[1]][id][BasicHeaders.compressionRatioPrevTable] == data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsCross[BasicHeaders.prevBeforePostBothHeader] += 1
            else:
                compResultsCross[BasicHeaders.prevBeforePostAfterHeader] += 1
        elif data[anyParam[0]][id][BasicHeaders.compressionRatioPrevTable] == data[anyParam[0]][id][BasicHeaders.compressionRatioPostTable]:
            if data[anyParam[1]][id][BasicHeaders.compressionRatioPrevTable] < data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsCross[BasicHeaders.prevBothPostBeforeHeader] += 1
            elif data[anyParam[1]][id][BasicHeaders.compressionRatioPrevTable] == data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsCross[BasicHeaders.prevBothPostBothHeader] += 1
            else:
                compResultsCross[BasicHeaders.prevBothPostAfterHeader] += 1
        else:
            if data[anyParam[1]][id][BasicHeaders.compressionRatioPrevTable] < data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsCross[BasicHeaders.prevAfterPostBeforeHeader] += 1
            elif data[anyParam[1]][id][BasicHeaders.compressionRatioPrevTable] == data[anyParam[1]][id][BasicHeaders.compressionRatioPostTable]:
                compResultsCross[BasicHeaders.prevAfterPostBothHeader] += 1
            else:
                compResultsCross[BasicHeaders.prevAfterPostAfterHeader] += 1

    writeMatrixData(dataSheet, compResults, rowPos)
    rowPos += 5
    writeMatrixData(dataSheet, compResultsRatio, rowPos)
    rowPos += 5
    writeRowData(dataSheet, compResultsEvol2015, rowPos, "201510-CT")
    rowPos += 3
    writeRowData(dataSheet, compResultsEvol2016, rowPos, "201610-CT")
    rowPos +=5
    writeMatrixDataCross(dataSheet, compResultsCross, rowPos)
    book.save(spreadSheetFilename)