###############################################################################
# File: DataImporter.py
# Author: Carlos Bobed
# Date: March 2018
# Comments: script to import the data of the codified transactions into a
#       sqlite database
# Modifications:
###############################################################################

###############################################################################
## MOBICOM HEADERS
#Holes;#scenario;#objects;alpha;#refObject;TrueCover;FalseCover;ExecTime;Precision;Recall;estimationAlg;LOSObjects;occPer

from csv import DictReader
import math
import sqlite3 as lite

import sys
import ntpath
from utils.CSVHeaders import BasicHeaders

def createTable(databaseFilename, tableName):
    con = lite.connect(databaseFilename)
    with con:
        cur = con.cursor()


    # prevTransactionsTable = "#prevTransactions"
    # postTransactionsTable = "#postTransactions"
    # prevCodTimeTable = "prevCodTime"
    # postCodTimeTable  = "postCodTime"
    #
    # # calculated fields in the table
    # compressionRatioPrevTable = "compRatioPrev"
    # compressionRatioPostTable = "compRatioPost"

        SQLSentence = "CREATE TABLE IF NOT EXISTS "+tableName+" (" +\
            BasicHeaders.CTTable+ " TEXT, "+\
            BasicHeaders.updateIDTable+ " TEXT, "+\
            BasicHeaders.prevCodSizeTable+" REAL, " +\
            BasicHeaders.prevCodSizeSCTTable+ " REAL, "+\
            BasicHeaders.postCodSizeTable+" REAL, "+\
            BasicHeaders.postCodSizeSCTTable+" REAL, "+\
            BasicHeaders.prevTransactionsTable+ " INTEGER,"+\
            BasicHeaders.postTransactionsTable+" INTEGER, "+\
            BasicHeaders.prevCodTimeTable+" REAL, "+\
            BasicHeaders.postCodTimeTable+" REAL, "+\
            BasicHeaders.compressionRatioPrevTable+" REAL, "+\
            BasicHeaders.compressionRatioPostTable+ " REAL )"
        print SQLSentence
        cur.execute(SQLSentence)

###### MAIN #######

if __name__ == "__main__":
    print sys.argv
    if (len(sys.argv) != 4):
        print ("uso: python DataImporter.py CSVFilename DBFilename TableName")
        sys.exit()
    createTable(sys.argv[2], sys.argv[3])

    with open(sys.argv[1]) as dataFile:
        dataSheet = DictReader(dataFile, delimiter=';')
        con = lite.connect(sys.argv[2])
        with con:
            cur = con.cursor()
            for row in dataSheet:
                #print row
                # BasicHeaders.CTTable + " TEXT, " + \
                # BasicHeaders.updateIDTable + " TEXT, " + \
                # BasicHeaders.prevCodSizeTable + " REAL, " + \
                # BasicHeaders.prevCodSizeSCTTable + " REAL, " + \
                # BasicHeaders.postCodSizeTable + " REAL, " + \
                # BasicHeaders.postCodSizeSCTTable + " REAL, " + \
                # BasicHeaders.prevTransactionsTable + " INTEGER," + \
                # BasicHeaders.postTransactionsTable + " INTEGER, " + \
                # BasicHeaders.prevCodTimeTable + " REAL, " + \
                # BasicHeaders.postCodTimeTable + " REAL, " + \
                # BasicHeaders.compressionRatioPrevTable + " REAL, " + \
                # BasicHeaders.compressionRatioPostTable + " REAL )"
                SQLInsertSentence = "INSERT INTO "+sys.argv[3]+" VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
                # print row
                if (row[BasicHeaders.prevCodSizeSCTHeader] == '0.0'):
                    prevRatio = 0.0
                else:
                    prevRatio = float(row[BasicHeaders.prevCodSizeHeader])/float(row[BasicHeaders.prevCodSizeSCTHeader])
                if (row[BasicHeaders.postCodSizeSCTHeader] == '0.0'):
                    postRatio = 0.0
                else:
                    postRatio = float(row[BasicHeaders.postCodSizeHeader])/float(row[BasicHeaders.postCodSizeSCTHeader])
                arguments = (row[BasicHeaders.CTHeader],
                            row[BasicHeaders.updateIDHeader],
                            float(row[BasicHeaders.prevCodSizeHeader]),
                            float(row[BasicHeaders.prevCodSizeSCTHeader]),
                            float(row[BasicHeaders.postCodSizeHeader]),
                            float(row[BasicHeaders.postCodSizeSCTHeader]),
                            int(row[BasicHeaders.prevTransactionsHeader]),
                            int(row[BasicHeaders.postTransactionsHeader]),
                            float(row[BasicHeaders.prevCodTimeHeader]),
                            float(row[BasicHeaders.postCodTimeHeader]),
                            prevRatio,
                            postRatio)
                cur.execute(SQLInsertSentence, arguments)

        cur.execute("CREATE INDEX IF NOT EXISTS "+sys.argv[3]+"_idx_compact ON "+sys.argv[3]+"("+BasicHeaders.updateIDTable+")")
