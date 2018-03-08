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


    prev2015Post2015Header = "prev2015Post2015"
    prev2015PostBothHeader = "prev2015PostBoth"
    prev2015Post2016Header = "prev2015Post2016"

    prevBothPost2015Header = "prevBothPost2015"
    prevBothPostBothHeader = "prevBothPostBoth"
    prevBothPost2016Header = "prevBothPost2016"

    prev2016Post2015Header = "prev2016Post2015"
    prev2016PostBothHeader = "prev2016PostBoth"
    prev2016Post2016Header = "prev2016Post2016"

    beforeHeader = "beforeBetter"
    equalHeader = "equal"
    afterHeader = "afterBetter"

    prevBeforePostBeforeHeader = "prevB4PostB4"
    prevBeforePostBothHeader = "prevB4PostBoth"
    prevBeforePostAfterHeader = "prevB4PostAft"

    prevBothPostBeforeHeader = "prevBothPostB4"
    prevBothPostBothHeader = "prevBothPostBoth"
    prevBothPostAfterHeader = "prevBothPostAfter"

    prevAfterPostBeforeHeader = "prevAftPostB4"
    prevAfterPostBothHeader = "prevAftPostBoth"
    prevAfterPostAfterHeader = "prevAftPostAft"