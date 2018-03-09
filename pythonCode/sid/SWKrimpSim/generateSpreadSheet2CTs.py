###############################################################################
# File: generateSpreadSheet2CTs.py
# Author: Carlos Bobed
# Date: March 2018
# Comment: script to build the spreadsheets with the data grouped in
#       different ways
# Modifications:
###############################################################################

import sys
from utils.CSVHeaders import BasicHeaders as BH
import sqlite3 as lite
from xlwt import Workbook, easyxf
import math


def loadData (databaseName, tablename, block, executionData):
    con = lite.connect(databaseName)
    if (executionData not in block):
        block[executionData] = {}
    with con:
        con.row_factory = lite.Row
        cur = con.cursor()
        statement = "SELECT * FROM "+tablename+" WHERE "+BH.CTTable+" LIKE '"+\
            executionData+"'"
        print statement
        cur.execute(statement)
        rows = cur.fetchall()
        for row in rows:
            block[executionData][row[BH.updateIDTable]] = row


def write3x3MatrixData(sheet, compResults, rowPos, rowHeaders, colHeaders):

    sheet.write(rowPos, 1, colHeaders[0],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 2, colHeaders[1],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 3, colHeaders[2],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    rowPos += 1
    sheet.write(rowPos, 0, rowHeaders[0],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BH.prevCT1Header][BH.postCT1Header], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BH.prevCT1Header][BH.postBothHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BH.prevCT1Header][BH.postCT2Header] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

    rowPos += 1
    sheet.write(rowPos, 0, rowHeaders[1],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BH.prevBothHeader][BH.postCT1Header],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BH.prevBothHeader][BH.postBothHeader],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BH.prevBothHeader][BH.postCT2Header],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

    rowPos += 1
    sheet.write(rowPos, 0, rowHeaders[2],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BH.prevCT2Header][BH.postCT1Header], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BH.prevCT2Header][BH.postBothHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BH.prevCT2Header][BH.postCT2Header] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

    return rowPos + 2

def write2x2MatrixData(sheet, compResults, rowPos, rowHeaders, colHeaders):

    sheet.write(rowPos, 1, colHeaders[0],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 2, colHeaders[1],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    rowPos += 1
    sheet.write(rowPos, 0, rowHeaders[0],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BH.prevCT1Header][BH.postCT1Header], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BH.prevCT1Header][BH.postCT2Header], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

    rowPos += 1
    sheet.write(rowPos, 0, rowHeaders[1],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BH.prevCT2Header][BH.postCT1Header],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BH.prevCT2Header][BH.postCT2Header],
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

    return rowPos + 2


def writeRowData(sheet, compResults, rowPos, rowName):

    sheet.write(rowPos, 1, BH.beforeHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 2, BH.equalHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 3, BH.afterHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    rowPos += 1
    sheet.write(rowPos, 0, rowName,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 1, compResults[BH.beforeHeader], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 2, compResults[BH.equalHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    sheet.write(rowPos, 3, compResults[BH.afterHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))

    return rowPos + 2

def writeDescription (sheet, CT1Name, CT2Name, rowPos):

    sheet.write_merge(rowPos, rowPos, 0, 3, 'CT1:'+CT1Name,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    rowPos += 1
    sheet.write_merge(rowPos, rowPos, 0, 3, 'CT2:'+CT2Name,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    return rowPos + 2

def writeLine(sheet, message, colIni, colEnd, rowPos):
    sheet.write_merge(rowPos, rowPos, colIni, colEnd, message,
        easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
    return rowPos + 1

def calculateComparisonUnified (dataCT1, dataCT2, id, fieldPrev, fieldPost, results):
    if dataCT1[id][fieldPrev] <= dataCT2[id][fieldPrev]:
        if dataCT1[id][fieldPost] <= dataCT2[id][fieldPost]:
            results[BH.prevCT1Header][BH.postCT1Header] += 1
        else:
            results[BH.prevCT1Header][BH.postCT2Header] += 1
    elif dataCT1[id][fieldPrev] == dataCT2[id][fieldPrev]:
        if dataCT1[id][fieldPost] <= dataCT2[id][fieldPost]:
            results[BH.prevBothHeader][BH.postCT1Header] += 1
        else:
            results[BH.prevBothHeader][BH.postCT2Header] += 1
    else:
        if dataCT1[id][fieldPost] <= dataCT2[id][fieldPost]:
            results[BH.prevCT2Header][BH.postCT1Header] += 1
        else:
            results[BH.prevCT2Header][BH.postCT2Header] += 1

def calculateCrossComparisonUnified (dataCT1, dataCT2, id, fieldPrev, fieldPost, results):
    if dataCT1[id][fieldPrev] <= dataCT1[id][fieldPost]:
        if dataCT2[id][fieldPrev] <= dataCT2[id][fieldPost]:
            results[BH.prevCT1Header][BH.postCT1Header] += 1
        else:
            results[BH.prevCT1Header][BH.postCT2Header] += 1
    else:
        if dataCT2[id][fieldPrev] <= dataCT2[id][fieldPost]:
            results[BH.prevCT2Header][BH.postCT1Header] += 1
        else:
            results[BH.prevCT2Header][BH.postCT2Header] += 1

####### MAIN #######
if __name__ == "__main__":

    print sys.argv
    print len(sys.argv)
    if (len(sys.argv) != 5):
        print ("uso: python generateSpreadsheetBase.py databaseName spreadSheetFilename CT1 CT2")
        sys.exit()

    databaseFilename = sys.argv[1]
    spreadSheetFilename = sys.argv[2]
    book = Workbook(style_compression=2)
    print "processing data table ... "
    dataSheet = book.add_sheet("data")

    parameters = (sys.argv[3], sys.argv[4])
    print parameters
    print "writing the parameters"
    rowPos = 1
    percentage = 0
    data = {}
    count = 0
    anyParam = []
    for executionParams in parameters:
        anyParam.append(executionParams)
        loadData(databaseFilename, "updates", data, executionParams)
    print anyParam

    compResults = {}
    for i in (BH.prevCT1Header, BH.prevBothHeader, BH.prevCT2Header):
        compResults[i] = {}
        for j in (BH.postCT1Header, BH.postBothHeader, BH.postCT2Header):
            compResults[i][j] = 0

    compResultsRatio = {}
    for i in (BH.prevCT1Header, BH.prevBothHeader, BH.prevCT2Header):
        compResultsRatio[i] = {}
        for j in (BH.postCT1Header, BH.postBothHeader, BH.postCT2Header):
            compResultsRatio[i][j] = 0

    compResultsUnifiedCT1 = {}
    for i in (BH.prevCT1Header, BH.prevCT2Header):
        compResultsUnifiedCT1[i] = {}
        for j in (BH.postCT1Header, BH.postCT2Header):
            compResultsUnifiedCT1[i][j] = 0

    compResultsUnifiedCT2 = {}
    for i in (BH.prevCT1Header, BH.prevCT2Header):
        compResultsUnifiedCT2[i] = {}
        for j in (BH.postCT1Header, BH.postCT2Header):
            compResultsUnifiedCT2[i][j] = 0

    compResultsEvolCT1 = {BH.beforeHeader: 0, BH.equalHeader: 0, BH.afterHeader: 0}

    compResultsEvolCT2 = {BH.beforeHeader: 0, BH.equalHeader: 0, BH.afterHeader: 0}

    compResultsCross = {}
    for i in (BH.prevCT1Header, BH.prevBothHeader, BH.prevCT2Header):
        compResultsCross[i] = {}
        for j in (BH.postCT1Header, BH.postBothHeader, BH.postCT2Header):
            compResultsCross[i][j] = 0

    compResultsCrossUnifiedCT1 = {}
    for i in (BH.prevCT1Header, BH.prevCT2Header):
        compResultsCrossUnifiedCT1[i] = {}
        for j in (BH.postCT1Header, BH.postCT2Header):
            compResultsCrossUnifiedCT1[i][j] = 0

    compResultsCrossUnifiedCT2 = {}
    for i in (BH.prevCT1Header, BH.prevCT2Header):
        compResultsCrossUnifiedCT2[i] = {}
        for j in (BH.postCT1Header, BH.postCT2Header):
            compResultsCrossUnifiedCT2[i][j] = 0

    listIds = data[anyParam[0]].keys()

    dataCT1 = data[anyParam[0]]
    dataCT2 = data[anyParam[1]]

    for id in listIds:
        if dataCT1[id][BH.prevCodSizeHeader] < dataCT2[id][BH.prevCodSizeHeader]:
            if dataCT1[id][BH.postCodSizeHeader] < dataCT2[id][BH.postCodSizeHeader]:
                compResults[BH.prevCT1Header][BH.postCT1Header] += 1
            elif dataCT1[id][BH.postCodSizeHeader] == dataCT2[id][BH.postCodSizeHeader]:
                compResults[BH.prevCT1Header][BH.postBothHeader] += 1
            else:
                compResults[BH.prevCT1Header][BH.postCT2Header] += 1
        elif dataCT1[id][BH.prevCodSizeHeader] == dataCT2[id][BH.prevCodSizeHeader]:
            if dataCT1[id][BH.postCodSizeHeader] < dataCT2[id][BH.postCodSizeHeader]:
                compResults[BH.prevBothHeader][BH.postCT1Header] += 1
            elif dataCT1[id][BH.postCodSizeHeader] == dataCT2[id][BH.postCodSizeHeader]:
                compResults[BH.prevBothHeader][BH.postBothHeader] += 1
            else:
                compResults[BH.prevBothHeader][BH.postCT2Header] += 1
        else:
            if dataCT1[id][BH.postCodSizeHeader] < dataCT2[id][BH.postCodSizeHeader]:
                compResults[BH.prevCT2Header][BH.postCT1Header] += 1
            elif dataCT1[id][BH.postCodSizeHeader] == dataCT2[id][BH.postCodSizeHeader]:
                compResults[BH.prevCT2Header][BH.postBothHeader] += 1
            else:
                compResults[BH.prevCT2Header][BH.postCT2Header] += 1

        if dataCT1[id][BH.compressionRatioPrevTable] < dataCT2[id][BH.compressionRatioPrevTable]:
            if dataCT1[id][BH.compressionRatioPostTable] < dataCT2[id][BH.compressionRatioPostTable]:
                compResultsRatio[BH.prevCT1Header][BH.postCT1Header] += 1
            elif dataCT1[id][BH.compressionRatioPostTable] == dataCT2[id][BH.compressionRatioPostTable]:
                compResultsRatio[BH.prevCT1Header][BH.postBothHeader] += 1
            else:
                compResultsRatio[BH.prevCT1Header][BH.postCT2Header] += 1
        elif dataCT1[id][BH.compressionRatioPrevTable] == dataCT2[id][BH.compressionRatioPrevTable]:
            if dataCT1[id][BH.compressionRatioPostTable] < dataCT2[id][BH.compressionRatioPostTable]:
                compResultsRatio[BH.prevBothHeader][BH.postCT1Header] += 1
            elif dataCT1[id][BH.compressionRatioPostTable] == dataCT2[id][BH.compressionRatioPostTable]:
                compResultsRatio[BH.prevBothHeader][BH.postBothHeader] += 1
            else:
                compResultsRatio[BH.prevBothHeader][BH.postCT2Header] += 1
        else:
            if dataCT1[id][BH.compressionRatioPostTable] < dataCT2[id][BH.compressionRatioPostTable]:
                compResultsRatio[BH.prevCT2Header][BH.postCT1Header] += 1
            elif dataCT1[id][BH.compressionRatioPostTable] == dataCT2[id][BH.compressionRatioPostTable]:
                compResultsRatio[BH.prevCT2Header][BH.postBothHeader] += 1
            else:
                compResultsRatio[BH.prevCT2Header][BH.postCT2Header] += 1

        if dataCT1[id][BH.compressionRatioPrevTable] < dataCT1[id][BH.compressionRatioPostTable]:
            compResultsEvolCT1[BH.beforeHeader] += 1
        elif dataCT1[id][BH.compressionRatioPrevTable] == dataCT1[id][BH.compressionRatioPostTable]:
            compResultsEvolCT1[BH.equalHeader] += 1
        else:
            compResultsEvolCT1[BH.afterHeader] += 1

        if dataCT2[id][BH.compressionRatioPrevTable] < dataCT2[id][BH.compressionRatioPostTable]:
            compResultsEvolCT2[BH.beforeHeader] += 1
        elif dataCT2[id][BH.compressionRatioPrevTable] == dataCT2[id][BH.compressionRatioPostTable]:
            compResultsEvolCT2[BH.equalHeader] += 1
        else:
            compResultsEvolCT2[BH.afterHeader] += 1

        # cross comparison
        if dataCT1[id][BH.compressionRatioPrevTable] < dataCT1[id][BH.compressionRatioPostTable]:
            if dataCT2[id][BH.compressionRatioPrevTable] < dataCT2[id][BH.compressionRatioPostTable]:
                compResultsCross[BH.prevCT1Header][BH.postCT1Header] += 1
            elif dataCT2[id][BH.compressionRatioPrevTable] == dataCT2[id][BH.compressionRatioPostTable]:
                compResultsCross[BH.prevCT1Header][BH.postBothHeader] += 1
            else:
                compResultsCross[BH.prevCT1Header][BH.postCT2Header] += 1
        elif dataCT1[id][BH.compressionRatioPrevTable] == dataCT1[id][BH.compressionRatioPostTable]:
            if dataCT2[id][BH.compressionRatioPrevTable] < dataCT2[id][BH.compressionRatioPostTable]:
                compResultsCross[BH.prevBothHeader][BH.postCT1Header] += 1
            elif dataCT2[id][BH.compressionRatioPrevTable] == dataCT2[id][BH.compressionRatioPostTable]:
                compResultsCross[BH.prevBothHeader][BH.postBothHeader] += 1
            else:
                compResultsCross[BH.prevBothHeader][BH.postCT2Header] += 1
        else:
            if dataCT2[id][BH.compressionRatioPrevTable] < dataCT2[id][BH.compressionRatioPostTable]:
                compResultsCross[BH.prevCT2Header][BH.postCT1Header] += 1
            elif dataCT2[id][BH.compressionRatioPrevTable] == dataCT2[id][BH.compressionRatioPostTable]:
                compResultsCross[BH.prevCT2Header][BH.postBothHeader] += 1
            else:
                compResultsCross[BH.prevCT2Header][BH.postCT2Header] += 1

        calculateComparisonUnified(dataCT1, dataCT2, id, BH.prevCodSizeHeader, BH.postCodSizeHeader, compResultsUnifiedCT1)
        calculateComparisonUnified(dataCT2, dataCT1, id, BH.prevCodSizeHeader, BH.postCodSizeHeader, compResultsUnifiedCT2)

        calculateCrossComparisonUnified(dataCT1, dataCT2, id,
                                        BH.compressionRatioPrevTable, BH.compressionRatioPostTable,
                                        compResultsCrossUnifiedCT1)
        calculateCrossComparisonUnified(dataCT2, dataCT1, id,
                                    BH.compressionRatioPrevTable, BH.compressionRatioPostTable,
                                    compResultsCrossUnifiedCT2)

    headersComp = [(BH.prevCT1Header, BH.prevBothHeader, BH.prevCT2Header),
                   (BH.postCT1Header, BH.postBothHeader, BH.postCT2Header)]
    headersCross = [(BH.beforeHeader, BH.equalHeader, BH.afterHeader),
                    (BH.beforeHeader, BH.equalHeader, BH.afterHeader)]

    headersUnified = [(BH.prevCT1Header, BH.prevCT2Header),
                      (BH.postCT1Header, BH.postCT2Header)]

    rowPos = writeDescription(dataSheet, anyParam[0], anyParam[1], rowPos)

    rowPos = write3x3MatrixData(dataSheet, compResults, rowPos, headersComp[0], headersComp[1])

    rowPos = writeLine(dataSheet, 'CT1 is '+anyParam[0], 0, 3, rowPos)
    rowPos = write2x2MatrixData(dataSheet, compResultsUnifiedCT1, rowPos, headersUnified[0], headersUnified[1])

    rowPos = writeLine(dataSheet, 'CT1 is ' + anyParam[1], 0, 3, rowPos)
    rowPos = write2x2MatrixData(dataSheet, compResultsUnifiedCT2, rowPos, headersUnified[0], headersUnified[1])

    rowPos = write3x3MatrixData(dataSheet, compResultsRatio, rowPos, headersComp[0], headersComp[1])

    rowPos = writeRowData(dataSheet, compResultsEvolCT1, rowPos, anyParam[0])

    rowPos = writeRowData(dataSheet, compResultsEvolCT2, rowPos, anyParam[1])

    rowPos = write3x3MatrixData(dataSheet, compResultsCross, rowPos, headersCross[0], headersCross[1])

    rowPos = writeLine(dataSheet, 'CT1 is ' + anyParam[0], 0, 3, rowPos)
    rowPos = write2x2MatrixData(dataSheet, compResultsCrossUnifiedCT1, rowPos, headersUnified[0], headersUnified[1])

    rowPos = writeLine(dataSheet, 'CT1 is ' + anyParam[1], 0, 3, rowPos)
    rowPos = write2x2MatrixData(dataSheet, compResultsCrossUnifiedCT2, rowPos, headersUnified[0], headersUnified[1])

    book.save(spreadSheetFilename)


    # dataSheet = book.add_sheet("data-FGraph-graph")
    # writeDataGraph(dataSheet,data, BH.FCoverTable)
    # book.save(spreadSheetFilename)
    #
    # dataSheet = book.add_sheet("data-ExecTime-graph")
    # writeDataGraph(dataSheet,data,BH.execTimeTable)
    # book.save(spreadSheetFilename)