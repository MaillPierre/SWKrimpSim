###############################################################################
# File: generateSpreadSheetGranules-CH-T-D.py
# Author: Carlos Bobed
# Date: June 2016
# Comment: script to build the spreadsheets with the data grouped in
#       different ways
# Modifications:
#       * Feb 2017: 23-2-2017 => modifid to handle the data of the MOBICOM
#           Experiments
###############################################################################

import sys
from utils.CSVHeaders import BasicHeaders
import sqlite3 as lite
from xlwt import Workbook, easyxf
import math

rangeSentence = " CASE WHEN "+BasicHeaders.occupiedPercentageTable +" BETWEEN 0.0 and 0.1 then ' 0.0 - 0.1 ' "+\
         " WHEN "+BasicHeaders.occupiedPercentageTable +" BETWEEN 0.1 and 0.2 then ' 0.1 - 0.2 ' "+\
         " WHEN "+BasicHeaders.occupiedPercentageTable +" BETWEEN 0.2 and 0.3 then ' 0.2 - 0.3 ' "+\
         " WHEN "+BasicHeaders.occupiedPercentageTable +" BETWEEN 0.3 and 0.4 then ' 0.3 - 0.4 ' "+\
         " WHEN "+BasicHeaders.occupiedPercentageTable +" BETWEEN 0.4 and 0.5 then ' 0.4 - 0.5 ' "+\
         " WHEN "+BasicHeaders.occupiedPercentageTable +" BETWEEN 0.5 and 0.6 then ' 0.5 - 0.6 ' "+\
         " WHEN "+BasicHeaders.occupiedPercentageTable +" BETWEEN 0.6 and 0.7 then ' 0.6 - 0.7 ' "+\
         " ELSE 'other' END AS "+BasicHeaders.occupiedRangeTable

def obtainExecutionParameters (databaseName, tableName):
    con = lite.connect(databaseName)
    with con:
        con.row_factory = lite.Row
        cur = con.cursor()
        statement = "SELECT DISTINCT "+rangeSentence+", "+BasicHeaders.numObjectsTable+", "+ \
                    BasicHeaders.alphaTable +" FROM "+tableName
        print statement
        cur.execute(statement)
        parameters = cur.fetchall()
    return parameters

def obtainAlphas (databaseName, tableName):
    con = lite.connect(databaseName)
    with con:
        con.row_factory = lite.Row
        cur = con.cursor()
        statement = "SELECT DISTINCT "+\
                    BasicHeaders.alphaTable +" FROM "+tableName
        print statement
        cur.execute(statement)
        parameters = cur.fetchall()
    return parameters


def groupExecutionStatistics (databaseName, tableName, executionParams):
    con = lite.connect(databaseName)
    with con:
        con.row_factory = lite.Row
        cur = con.cursor()
        statement = "SELECT "+rangeSentence+", count(*), sum("+ BasicHeaders.FCoverTable+") / count(*), "+\
                    "sum("+BasicHeaders.execTimeTable+") / count(*) FROM "+tableName+" WHERE "+ \
            BasicHeaders.occupiedRangeTable +" = ? AND "+\
            BasicHeaders.numObjectsTable+ " = ? AND "+ \
            BasicHeaders.alphaTable+ " >= ? AND "+ \
            BasicHeaders.alphaTable+ " <= ? "

        cur.execute(statement, (executionParams[BasicHeaders.occupiedRangeTable],
                                executionParams[BasicHeaders.numObjectsTable],
                                executionParams[BasicHeaders.alphaTable]-0.01,
                                executionParams[BasicHeaders.alphaTable]+0.01))
        executionRow = cur.fetchone()
        return executionRow

def insertDataStatistics (block, executionParams, executionData):
    numObjects = executionParams[BasicHeaders.numObjectsTable]
    alpha = executionParams[BasicHeaders.alphaTable]
    range = executionParams[BasicHeaders.occupiedRangeTable]
    if (range not in block):
        block[range] = {}
    if (numObjects not in block[range]):
        block[range][numObjects] = {}
    if (alpha not in block[range][numObjects]):
        block[range][numObjects][alpha] = {}
    block[range][numObjects][alpha][BasicHeaders.FCoverTable] = executionData[2]
    block[range][numObjects][alpha][BasicHeaders.execTimeTable] = executionData[3]

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

####### MAIN #######
if __name__ == "__main__":

    print sys.argv

    if (len(sys.argv) != 4):
        print ("uso: python generateSpreadsheetGranules-CH-T.py databaseName [both|convex|nonConvex] spreadSheetFilename")
        sys.exit()

    databaseFilename = sys.argv[1]
    spreadSheetFilename = sys.argv[3]
    book = Workbook(style_compression=2)
    print "processing data table ... "
    dataSheet = book.add_sheet("data")
    writeHeaders(dataSheet)
    parameters = obtainExecutionParameters(databaseFilename, "data")
    print "writing the parameters"
    rowPos = 1
    percentage = 0
    data = {}
    count = 0
    for executionParams in parameters:
        if (count % 10 == 0):
            print str(count) +" out of "+str(len(parameters)) + " sets of parameters"
        executionData = groupExecutionStatistics(databaseFilename, "data", executionParams)
        insertDataStatistics(data, executionParams, executionData)
        writeData(dataSheet, executionParams, executionData, rowPos)
        rowPos += 1
        count+=1
    book.save(spreadSheetFilename)

    alphas = obtainAlphas(databaseFilename, "data")
    for alpha in alphas:
        dataSheet = book.add_sheet("data - Alpha "+str(alpha[BasicHeaders.alphaTable]))
        # writeHeaders(dataSheet)
        # rowPos = 1
        # for executionParams in parameters:
        #     if executionParams[BasicHeaders.alphaHeader] ==  alpha[BasicHeaders.alphaTable]:
        #         executionData = groupExecutionStatistics(databaseFilename, "data", executionParams)
        #         writeData(dataSheet, executionParams, executionData, rowPos)
        #         rowPos+=1

        writeDataGraphGroupedAlpha(dataSheet, data, alpha[BasicHeaders.alphaTable])

    book.save(spreadSheetFilename)

    # dataSheet = book.add_sheet("data-FGraph-graph")
    # writeDataGraph(dataSheet,data, BasicHeaders.FCoverTable)
    # book.save(spreadSheetFilename)
    #
    # dataSheet = book.add_sheet("data-ExecTime-graph")
    # writeDataGraph(dataSheet,data,BasicHeaders.execTimeTable)
    # book.save(spreadSheetFilename)