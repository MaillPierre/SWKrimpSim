###############################################################################
# File: CSVHeaders.py
# Author: Carlos Bobed
# Date: March 2018
# Comments: headers of the different CSV files that are generated in the
#       data evolution quality experiments
# Modifications:
###############################################################################


#CT;updateID;prevCodSize;prevCodSizeSCT;postCodSize;postCodSizeSCT;#prevTransactions;#postTransactions;prevCodTime;postCodTime

class BasicHeaders:
    CTHeader = "CT"
    updateIDHeader = "updateID"
    prevCodSizeHeader = "prevCodSize"
    prevCodSizeSCTHeader = "prevCodSizeSCT"
    postCodSizeHeader = "postCodSize"
    postCodSizeSCTHeader = "postCodSizeSCT"
    prevTransactionsHeader = "#prevTransactions"
    postTransactionsHeader = "#postTransactions"
    prevCodTimeHeader = "prevCodTime"
    postCodTimeHeader = "postCodTime"

    CTTable= "CT"
    updateIDTable = "updateID"
    prevCodSizeTable = "prevCodSize"
    prevCodSizeSCTTable = "prevCodSizeSCT"
    postCodSizeTable = "postCodSize"
    postCodSizeSCTTable = "postCodSizeSCT"
    prevTransactionsTable = "prevTransactions"
    postTransactionsTable = "postTransactions"
    prevCodTimeTable = "prevCodTime"
    postCodTimeTable  = "postCodTime"

    # calculated fields in the table
    compressionRatioPrevTable = "compRatioPrev"
    compressionRatioPostTable = "compRatioPost"

    prevCT1Header = "prevCT1"
    prevBothHeader = "prevBoth"
    prevCT2Header = "prevCT2"

    postCT1Header = "postCT1"
    postBothHeader = "postBoth"
    postCT2Header = "postCT2"

    beforeHeader = "beforeBetter"
    equalHeader = "equal"
    afterHeader = "afterBetter"

    prevCTHeader = "prevCT"
    postCTHeader = "postCT"
