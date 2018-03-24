###############################################################################
# File: generateSpreadSheetMatrixEv.py
# Author: Carlos Bobed
# Date: March 2018
# Comment: script to build the spreadsheets with the data grouped in
#       different ways
# Modifications:
###############################################################################

import sys

from sid.SWKrimpSim.generateSpreadSheetBase import obtainExecutionParameters
from utils.CSVHeaders import BasicHeaders as BH
import sqlite3 as lite
from xlwt import Workbook, easyxf
import math


def obtainExecutionParameters (databaseName, tableName):
    con = lite.connect(databaseName)
    with con:
        con.row_factory = lite.Row
        cur = con.cursor()
        statement = "SELECT DISTINCT "+BH.CTTable+" FROM "+tableName
        print statement
        cur.execute(statement)
        parameters = cur.fetchall()
    return parameters

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


def writeRowDataCT(sheet, CTNames, compResults, rowPos, rowName):

    colPos = 1
    for ct in CTNames:
        sheet.write(rowPos, colPos, ct,
                    easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        colPos += 1
    rowPos += 1
    sheet.write(rowPos, 0, rowName,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    colPos = 1
    for ct in CTNames:
        sheet.write(rowPos, colPos, compResults[ct], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
        colPos += 1

    return rowPos + 2

def writeDataEvol(sheet, CTNames, compResults, rowPos):

    sheet.write(rowPos, 1, BH.beforeHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 2, BH.equalHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    sheet.write(rowPos, 3, BH.afterHeader,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
    rowPos += 1

    for ct in CTNames:
        sheet.write(rowPos, 0, ct,
                    easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        sheet.write(rowPos, 1, compResults[ct][BH.beforeHeader], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
        sheet.write(rowPos, 2, compResults[ct][BH.equalHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
        sheet.write(rowPos, 3, compResults[ct][BH.afterHeader] , easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
        rowPos += 1
    return rowPos + 1

def writeClassificationEvol(sheet, CTNames, compResults, rowPos):

    colPos = 1
    for ct in CTNames:
        sheet.write(rowPos, colPos, ct,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        colPos +=1

    rowPos += 1

    for ct in CTNames:
        sheet.write(rowPos, 0, ct,
                    easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        colPos =1
        for colName in CTNames:
            sheet.write(rowPos, colPos, compResults[ct][colName], easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray25'))
            colPos+=1
        rowPos += 1
    return rowPos + 1

def writeDescription (sheet, CTNames, rowPos):

    id = 1
    for ct in CTNames:
        sheet.write_merge(rowPos, rowPos, 0, 3, 'CT'+str(id)+':'+ct,
                easyxf('borders: bottom medium, right medium; pattern: pattern solid, fore_colour gray40'))
        rowPos += 1
        id+=1
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


def calculateDisjointClassification (data, cts, id, field, result):
    min = cts[0]
    for ct in cts:
        if data[ct][id][field] <= data[min][id][field]:
            min = ct
    result[min] += 1

def calculateNonDisjointClassification (data, cts, id, field, result):
    min = cts[0]
    votes = set()
    for ct in cts:
        if data[ct][id][field] < data[min][id][field]:
            min = ct
            votes.clear()
            votes.add(ct)
        elif data[ct][id][field] == data[min][id][field]:
            votes.add(ct)
    for vote in votes:
        result[vote] += 1

def calculateDisjointClassificationEvolution (data, cts, id, prevField, postField, result):
    minPrev = cts[0]
    for ct in cts:
        if data[ct][id][prevField] <= data[minPrev][id][prevField]:
            minPrev = ct
    minPost = cts[0]
    for ct in cts:
        if data[ct][id][postField] <= data[minPost][id][postField]:
            minPost = ct
    result[minPrev][minPost] += 1

def calculateNonDisjointClassificationEvolution (data, cts, id, prevField, postField, result):
    minPrev = cts[0]
    votesPrev = set()
    for ct in cts:
        if data[ct][id][prevField] < data[minPrev][id][postField]:
            minPrev = ct
            votesPrev.clear()
            votesPrev.add(ct)
        elif data[ct][id][prevField] == data[minPrev][id][postField]:
            votesPrev.add(ct)
    minPost = cts[0]
    votesPost = set()
    for ct in cts:
        if data[ct][id][prevField] < data[minPost][id][postField]:
            minPost = ct
            votesPost.clear()
            votesPost.add(ct)
        elif data[ct][id][prevField] == data[minPost][id][postField]:
            votesPost.add(ct)
    for ctPrev in votesPrev:
        for ctPost in votesPost:
            result[ctPrev][ctPost] +=1


def calculateEvolution (data, ct, id, prevField, postField, result):

    if data[ct][id][prevField] < data[ct][id][postField]:
        result[BH.beforeHeader] += 1
    elif data[ct][id][prevField] == data[ct][id][postField]:
        result[BH.equalHeader] += 1
    else:
        result[BH.afterHeader] += 1


####### MAIN #######
if __name__ == "__main__":

    print sys.argv
    print len(sys.argv)
    if (len(sys.argv) <3 ):
        print ("uso: python generateSpreadsheetBase.py databaseName spreadSheetFilename [ommitedCTs]*")
        sys.exit()

    databaseFilename = sys.argv[1]
    spreadSheetFilename = sys.argv[2]
    book = Workbook(style_compression=2)
    print "processing data table ... "
    dataSheet = book.add_sheet("data")

    parametersDB = obtainExecutionParameters(databaseFilename, "updates")
    anyParam = []
    for par in parametersDB:
        anyParam.append(par[BH.CTTable])

    print 'retrieved parameters:'
    print anyParam
    for rem in sys.argv[3:]:
        anyParam.remove(rem)
    anyParam.sort()
    print 'working parameters:'
    print anyParam

    rowPos = 1
    percentage = 0
    data = {}
    count = 0
    for executionParams in anyParam:
        loadData(databaseFilename, "updates", data, executionParams)

    listIds = data[anyParam[0]].keys()

    ## Disjoint version
    classificationResultsPrevStatesDisj = {}
    for i in anyParam:
        classificationResultsPrevStatesDisj[i] = 0

    classificationResultsPostStatesDisj = {}
    for i in anyParam:
        classificationResultsPostStatesDisj[i] = 0

    classificationResultsPrevRatioDisj= {}
    for i in anyParam:
        classificationResultsPrevRatioDisj[i] = 0

    classificationResultsPostRatioDisj= {}
    for i in anyParam:
        classificationResultsPostRatioDisj[i] = 0

    classificationEvolCodificationDisj = {}
    for i in anyParam:
        classificationEvolCodificationDisj[i] = {}
        for j in anyParam:
            classificationEvolCodificationDisj[i][j]=0

    classificationEvolRatioDisj = {}
    for i in anyParam:
        classificationEvolRatioDisj[i] = {}
        for j in anyParam:
            classificationEvolRatioDisj[i][j] = 0

    ## Non disjoint version

    classificationResultsPrevStates = {}
    for i in anyParam:
        classificationResultsPrevStates[i] = 0

    classificationResultsPostStates = {}
    for i in anyParam:
        classificationResultsPostStates[i] = 0

    classificationResultsPrevRatio = {}
    for i in anyParam:
        classificationResultsPrevRatio[i] = 0

    classificationResultsPostRatio = {}
    for i in anyParam:
        classificationResultsPostRatio [i] = 0

    classificationEvolCodification = {}
    for i in anyParam:
        classificationEvolCodification[i] = {}
        for j in anyParam:
            classificationEvolCodification[i][j]=0

    classificationEvolRatio = {}
    for i in anyParam:
        classificationEvolRatio[i] = {}
        for j in anyParam:
            classificationEvolRatio[i][j] = 0


    ## status evolution
    evolutionStatus = {}
    for i in anyParam:
        evolutionStatus[i] = {BH.beforeHeader: 0, BH.equalHeader: 0, BH.afterHeader: 0}

    vote = ''
    test = 1
    for id in listIds:
        ## Disjoint calculations
        calculateDisjointClassification(data, anyParam, id, BH.prevCodSizeHeader, classificationResultsPrevStatesDisj)
        calculateDisjointClassification(data, anyParam, id, BH.postCodSizeHeader, classificationResultsPostStatesDisj)
        calculateDisjointClassification(data, anyParam, id, BH.compressionRatioPrevTable, classificationResultsPrevRatioDisj)
        calculateDisjointClassification(data, anyParam, id, BH.compressionRatioPostTable, classificationResultsPostRatioDisj)

        calculateDisjointClassificationEvolution(data, anyParam, id, BH.prevCodSizeHeader, BH.postCodSizeHeader, classificationEvolCodificationDisj)
        calculateDisjointClassificationEvolution(data, anyParam, id, BH.compressionRatioPrevTable, BH.compressionRatioPostTable, classificationEvolRatioDisj)
        ## Non Disjoint calculations

        calculateNonDisjointClassification(data, anyParam, id, BH.prevCodSizeHeader, classificationResultsPrevStates)
        calculateNonDisjointClassification(data, anyParam, id, BH.postCodSizeHeader, classificationResultsPostStates)
        calculateNonDisjointClassification(data, anyParam, id, BH.compressionRatioPrevTable, classificationResultsPrevRatio)
        calculateNonDisjointClassification(data, anyParam, id, BH.compressionRatioPostTable, classificationResultsPostRatio)

        calculateNonDisjointClassificationEvolution(data, anyParam, id, BH.prevCodSizeHeader, BH.postCodSizeHeader,
                                                 classificationEvolCodification)
        calculateNonDisjointClassificationEvolution(data, anyParam, id, BH.compressionRatioPrevTable,
                                                 BH.compressionRatioPostTable, classificationEvolRatio)

        for ct in anyParam:
            calculateEvolution(data,ct,id,BH.compressionRatioPrevTable, BH.compressionRatioPostTable,evolutionStatus[ct])

    # compResults = {}
    # for i in (BH.prevCT1Header, BH.prevBothHeader, BH.prevCT2Header):
    #     compResults[i] = {}
    #     for j in (BH.postCT1Header, BH.postBothHeader, BH.postCT2Header):
    #         compResults[i][j] = 0
    #
    # compResultsRatio = {}
    # for i in (BH.prevCT1Header, BH.prevBothHeader, BH.prevCT2Header):
    #     compResultsRatio[i] = {}
    #     for j in (BH.postCT1Header, BH.postBothHeader, BH.postCT2Header):
    #         compResultsRatio[i][j] = 0
    #
    # compResultsUnifiedCT1 = {}
    # for i in (BH.prevCT1Header, BH.prevCT2Header):
    #     compResultsUnifiedCT1[i] = {}
    #     for j in (BH.postCT1Header, BH.postCT2Header):
    #         compResultsUnifiedCT1[i][j] = 0
    #
    # compResultsUnifiedCT2 = {}
    # for i in (BH.prevCT1Header, BH.prevCT2Header):
    #     compResultsUnifiedCT2[i] = {}
    #     for j in (BH.postCT1Header, BH.postCT2Header):
    #         compResultsUnifiedCT2[i][j] = 0
    #
    # compResultsEvolCT1 = {BH.beforeHeader: 0, BH.equalHeader: 0, BH.afterHeader: 0}
    #
    # compResultsEvolCT2 = {BH.beforeHeader: 0, BH.equalHeader: 0, BH.afterHeader: 0}
    #
    # compResultsCross = {}
    # for i in (BH.prevCT1Header, BH.prevBothHeader, BH.prevCT2Header):
    #     compResultsCross[i] = {}
    #     for j in (BH.postCT1Header, BH.postBothHeader, BH.postCT2Header):
    #         compResultsCross[i][j] = 0
    #
    # compResultsCrossUnifiedCT1 = {}
    # for i in (BH.prevCT1Header, BH.prevCT2Header):
    #     compResultsCrossUnifiedCT1[i] = {}
    #     for j in (BH.postCT1Header, BH.postCT2Header):
    #         compResultsCrossUnifiedCT1[i][j] = 0
    #
    # compResultsCrossUnifiedCT2 = {}
    # for i in (BH.prevCT1Header, BH.prevCT2Header):
    #     compResultsCrossUnifiedCT2[i] = {}
    #     for j in (BH.postCT1Header, BH.postCT2Header):
    #         compResultsCrossUnifiedCT2[i][j] = 0
    #

    #
    # for id in listIds:
    #     if dataCT1[id][BH.prevCodSizeHeader] < dataCT2[id][BH.prevCodSizeHeader]:
    #         if dataCT1[id][BH.postCodSizeHeader] < dataCT2[id][BH.postCodSizeHeader]:
    #             compResults[BH.prevCT1Header][BH.postCT1Header] += 1
    #         elif dataCT1[id][BH.postCodSizeHeader] == dataCT2[id][BH.postCodSizeHeader]:
    #             compResults[BH.prevCT1Header][BH.postBothHeader] += 1
    #         else:
    #             compResults[BH.prevCT1Header][BH.postCT2Header] += 1
    #     elif dataCT1[id][BH.prevCodSizeHeader] == dataCT2[id][BH.prevCodSizeHeader]:
    #         if dataCT1[id][BH.postCodSizeHeader] < dataCT2[id][BH.postCodSizeHeader]:
    #             compResults[BH.prevBothHeader][BH.postCT1Header] += 1
    #         elif dataCT1[id][BH.postCodSizeHeader] == dataCT2[id][BH.postCodSizeHeader]:
    #             compResults[BH.prevBothHeader][BH.postBothHeader] += 1
    #         else:
    #             compResults[BH.prevBothHeader][BH.postCT2Header] += 1
    #     else:
    #         if dataCT1[id][BH.postCodSizeHeader] < dataCT2[id][BH.postCodSizeHeader]:
    #             compResults[BH.prevCT2Header][BH.postCT1Header] += 1
    #         elif dataCT1[id][BH.postCodSizeHeader] == dataCT2[id][BH.postCodSizeHeader]:
    #             compResults[BH.prevCT2Header][BH.postBothHeader] += 1
    #         else:
    #             compResults[BH.prevCT2Header][BH.postCT2Header] += 1
    #
    #     if dataCT1[id][BH.compressionRatioPrevTable] < dataCT2[id][BH.compressionRatioPrevTable]:
    #         if dataCT1[id][BH.compressionRatioPostTable] < dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsRatio[BH.prevCT1Header][BH.postCT1Header] += 1
    #         elif dataCT1[id][BH.compressionRatioPostTable] == dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsRatio[BH.prevCT1Header][BH.postBothHeader] += 1
    #         else:
    #             compResultsRatio[BH.prevCT1Header][BH.postCT2Header] += 1
    #     elif dataCT1[id][BH.compressionRatioPrevTable] == dataCT2[id][BH.compressionRatioPrevTable]:
    #         if dataCT1[id][BH.compressionRatioPostTable] < dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsRatio[BH.prevBothHeader][BH.postCT1Header] += 1
    #         elif dataCT1[id][BH.compressionRatioPostTable] == dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsRatio[BH.prevBothHeader][BH.postBothHeader] += 1
    #         else:
    #             compResultsRatio[BH.prevBothHeader][BH.postCT2Header] += 1
    #     else:
    #         if dataCT1[id][BH.compressionRatioPostTable] < dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsRatio[BH.prevCT2Header][BH.postCT1Header] += 1
    #         elif dataCT1[id][BH.compressionRatioPostTable] == dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsRatio[BH.prevCT2Header][BH.postBothHeader] += 1
    #         else:
    #             compResultsRatio[BH.prevCT2Header][BH.postCT2Header] += 1
    #
    #     if dataCT1[id][BH.compressionRatioPrevTable] < dataCT1[id][BH.compressionRatioPostTable]:
    #         compResultsEvolCT1[BH.beforeHeader] += 1
    #     elif dataCT1[id][BH.compressionRatioPrevTable] == dataCT1[id][BH.compressionRatioPostTable]:
    #         compResultsEvolCT1[BH.equalHeader] += 1
    #     else:
    #         compResultsEvolCT1[BH.afterHeader] += 1
    #
    #     if dataCT2[id][BH.compressionRatioPrevTable] < dataCT2[id][BH.compressionRatioPostTable]:
    #         compResultsEvolCT2[BH.beforeHeader] += 1
    #     elif dataCT2[id][BH.compressionRatioPrevTable] == dataCT2[id][BH.compressionRatioPostTable]:
    #         compResultsEvolCT2[BH.equalHeader] += 1
    #     else:
    #         compResultsEvolCT2[BH.afterHeader] += 1
    #
    #     # cross comparison
    #     if dataCT1[id][BH.compressionRatioPrevTable] < dataCT1[id][BH.compressionRatioPostTable]:
    #         if dataCT2[id][BH.compressionRatioPrevTable] < dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsCross[BH.prevCT1Header][BH.postCT1Header] += 1
    #         elif dataCT2[id][BH.compressionRatioPrevTable] == dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsCross[BH.prevCT1Header][BH.postBothHeader] += 1
    #         else:
    #             compResultsCross[BH.prevCT1Header][BH.postCT2Header] += 1
    #     elif dataCT1[id][BH.compressionRatioPrevTable] == dataCT1[id][BH.compressionRatioPostTable]:
    #         if dataCT2[id][BH.compressionRatioPrevTable] < dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsCross[BH.prevBothHeader][BH.postCT1Header] += 1
    #         elif dataCT2[id][BH.compressionRatioPrevTable] == dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsCross[BH.prevBothHeader][BH.postBothHeader] += 1
    #         else:
    #             compResultsCross[BH.prevBothHeader][BH.postCT2Header] += 1
    #     else:
    #         if dataCT2[id][BH.compressionRatioPrevTable] < dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsCross[BH.prevCT2Header][BH.postCT1Header] += 1
    #         elif dataCT2[id][BH.compressionRatioPrevTable] == dataCT2[id][BH.compressionRatioPostTable]:
    #             compResultsCross[BH.prevCT2Header][BH.postBothHeader] += 1
    #         else:
    #             compResultsCross[BH.prevCT2Header][BH.postCT2Header] += 1
    #
    #     calculateComparisonUnified(dataCT1, dataCT2, id, BH.prevCodSizeHeader, BH.postCodSizeHeader, compResultsUnifiedCT1)
    #     calculateComparisonUnified(dataCT2, dataCT1, id, BH.prevCodSizeHeader, BH.postCodSizeHeader, compResultsUnifiedCT2)
    #
    #     calculateCrossComparisonUnified(dataCT1, dataCT2, id,
    #                                     BH.compressionRatioPrevTable, BH.compressionRatioPostTable,
    #                                     compResultsCrossUnifiedCT1)
    #     calculateCrossComparisonUnified(dataCT2, dataCT1, id,
    #                                 BH.compressionRatioPrevTable, BH.compressionRatioPostTable,
    #                                 compResultsCrossUnifiedCT2)
    #
    # headersComp = [(BH.prevCT1Header, BH.prevBothHeader, BH.prevCT2Header),
    #                (BH.postCT1Header, BH.postBothHeader, BH.postCT2Header)]
    # headersCross = [(BH.beforeHeader, BH.equalHeader, BH.afterHeader),
    #                 (BH.beforeHeader, BH.equalHeader, BH.afterHeader)]
    #
    # headersUnified = [(BH.prevCT1Header, BH.prevCT2Header),
    #                   (BH.postCT1Header, BH.postCT2Header)]
    #
    rowPos = writeDescription(dataSheet, anyParam, rowPos)

    rowPos = writeLine(dataSheet, 'Evolution of the ratios for each dataset', 0,3, rowPos)
    rowPos = writeDataEvol(dataSheet, anyParam, evolutionStatus, rowPos)

    rowPos = writeLine(dataSheet, 'Disjoint classification using codification length', 0, 3, rowPos)
    rowPos = writeRowDataCT(dataSheet, anyParam, classificationResultsPrevStatesDisj, rowPos, 'prevStatesClassifiedDisj')
    rowPos = writeRowDataCT(dataSheet, anyParam, classificationResultsPostStatesDisj, rowPos, 'postStatesClassifiedDisj')

    rowPos = writeLine(dataSheet, '', 0, 3, rowPos)
    rowPos = writeClassificationEvol(dataSheet, anyParam, classificationEvolCodificationDisj, rowPos)

    rowPos = writeLine(dataSheet, 'Disjoint classification using compression ratios', 0, 3, rowPos)
    rowPos = writeRowDataCT(dataSheet, anyParam, classificationResultsPrevRatioDisj, rowPos, 'prevRatioClassifiedDisj')
    rowPos = writeRowDataCT(dataSheet, anyParam, classificationResultsPostRatioDisj, rowPos, 'postRatioClassifiedDisj')

    rowPos = writeLine(dataSheet, '', 0, 3, rowPos)
    rowPos = writeClassificationEvol(dataSheet, anyParam, classificationEvolRatioDisj, rowPos)

    rowPos = writeLine(dataSheet, 'Non disjoint classification using codification length', 0, 3, rowPos)
    rowPos = writeRowDataCT(dataSheet, anyParam, classificationResultsPrevStates, rowPos, 'prevStatesClassified')
    rowPos = writeRowDataCT(dataSheet, anyParam, classificationResultsPostStates, rowPos, 'postStatesClassified')
    rowPos = writeLine(dataSheet, '', 0, 3, rowPos)
    rowPos = writeClassificationEvol(dataSheet, anyParam, classificationEvolCodification, rowPos)
    rowPos = writeLine(dataSheet, 'Non disjoint classification using compression ratios', 0, 3, rowPos)
    rowPos = writeRowDataCT(dataSheet, anyParam, classificationResultsPrevRatio, rowPos, 'prevRatioClassified')
    rowPos = writeRowDataCT(dataSheet, anyParam, classificationResultsPostRatio, rowPos, 'postRatioClassified')
    rowPos = writeLine(dataSheet, '', 0, 3, rowPos)
    rowPos = writeClassificationEvol(dataSheet, anyParam, classificationEvolRatio, rowPos)

    #
    # rowPos = write3x3MatrixData(dataSheet, compResults, rowPos, headersComp[0], headersComp[1])
    #
    # rowPos = writeLine(dataSheet, 'CT1 is '+anyParam[0], 0, 3, rowPos)
    # rowPos = write2x2MatrixData(dataSheet, compResultsUnifiedCT1, rowPos, headersUnified[0], headersUnified[1])
    #
    # rowPos = writeLine(dataSheet, 'CT1 is ' + anyParam[1], 0, 3, rowPos)
    # rowPos = write2x2MatrixData(dataSheet, compResultsUnifiedCT2, rowPos, headersUnified[0], headersUnified[1])
    #
    # rowPos = write3x3MatrixData(dataSheet, compResultsRatio, rowPos, headersComp[0], headersComp[1])
    #
    # rowPos = writeRowData(dataSheet, compResultsEvolCT1, rowPos, anyParam[0])
    #
    # rowPos = writeRowData(dataSheet, compResultsEvolCT2, rowPos, anyParam[1])
    #
    # rowPos = write3x3MatrixData(dataSheet, compResultsCross, rowPos, headersCross[0], headersCross[1])
    #
    # rowPos = writeLine(dataSheet, 'CT1 is ' + anyParam[0], 0, 3, rowPos)
    # rowPos = write2x2MatrixData(dataSheet, compResultsCrossUnifiedCT1, rowPos, headersUnified[0], headersUnified[1])
    #
    # rowPos = writeLine(dataSheet, 'CT1 is ' + anyParam[1], 0, 3, rowPos)
    # rowPos = write2x2MatrixData(dataSheet, compResultsCrossUnifiedCT2, rowPos, headersUnified[0], headersUnified[1])
    #
    book.save(spreadSheetFilename)
    #
    #
    # # dataSheet = book.add_sheet("data-FGraph-graph")
    # # writeDataGraph(dataSheet,data, BH.FCoverTable)
    # # book.save(spreadSheetFilename)
    # #
    # # dataSheet = book.add_sheet("data-ExecTime-graph")
    # # writeDataGraph(dataSheet,data,BH.execTimeTable)
    # # book.save(spreadSheetFilename)