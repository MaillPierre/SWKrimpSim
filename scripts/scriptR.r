setwd("d:/Datos/workingDir/krimp/iswc")

library(scales)

data2014=read.csv2("2014.csv", header = TRUE, dec = ".")
data201504=read.csv2("201504.csv", header = TRUE, dec = ".")
data201510=read.csv2("201510.csv", header = TRUE, dec = ".")
data201610=read.csv2("201610.csv", header = TRUE, dec = ".")

getAndPlotKClusterBreaks <- function (data, numClusters, freqValue, titleName) {
  fit <-kmeans(data, numClusters)
  tempFram <- data.frame(data, fit$cluster)
  minIntervals <- vector(mode="double",length=numClusters)
  
  for (i in 1:numClusters) {
    minIntervals[i] <- min(tempFram$data[tempFram$fit.cluster == i])
  }
  maxInterval <- max(tempFram$data)
  minInterval <- min(tempFram$data)
  breaksArray = c(sort(minIntervals),maxInterval)
    
  hist(data,breaks = breaksArray, freq = freqValue, main=titleName)
  return (breaksArray)
  # intervals <- data.frame(sort(breaksArray))
  # 
  # grid.arrange(tableGrob(sort(breaksArray)), rows=2)
  # 
  
}

plotClusterNumberAnalysis <- function (myData, name, variableName) {
  wss <- (nrow(myData)-1)*sum(apply(myData,2,var))
  for (i in 2:15) wss[i] <- sum(kmeans(myData, 
                                       centers=i)$withinss)
  plot(1:15, wss, type="b", xlab="Number of Clusters",
       ylab="Within groups sum of squares")
  title(sprintf("%s - %s", name, variableName))
}

# calculating the new columns 

data2014$dPost <- data2014$postCodSizeSCT - data2014$postCodSize
data201504$dPost <- data201504$postCodSizeSCT - data201504$postCodSize
data201510$dPost <- data201510$postCodSizeSCT - data201510$postCodSize
data201610$dPost <- data201610$postCodSizeSCT - data201610$postCodSize

data2014$dPrev <- data2014$prevCodSizeSCT - data2014$prevCodSize
data201504$dPrev <- data201504$prevCodSizeSCT - data201504$prevCodSize
data201510$dPrev <- data201510$prevCodSizeSCT - data201510$prevCodSize
data201610$dPrev <- data201610$prevCodSizeSCT - data201610$prevCodSize

data2014$deltaD <- data2014$dPost - data2014$dPrev
data201504$deltaD <- data201504$dPost - data201504$dPrev
data201510$deltaD <- data201510$dPost - data201510$dPrev
data201610$deltaD <- data201610$dPost - data201610$dPrev

data2014$deltaCT <- data2014$prevCodSize - data2014$postCodSize
data201504$deltaCT <- data201504$prevCodSize - data201504$postCodSize
data201510$deltaCT <- data201510$prevCodSize - data201510$postCodSize
data201610$deltaCT <- data201610$prevCodSize - data201610$postCodSize

data2014$deltaSCT <- -( data2014$deltaD - data2014$deltaCT ) 
data201504$deltaSCT <- -(data201504$deltaD - data201504$deltaCT)
data201510$deltaSCT <- -(data201510$deltaD - data201510$deltaCT)
data201610$deltaSCT <- -(data201610$deltaD - data201610$deltaCT)


# separation of values 

data2014meaningLess <- data2014[ which(data2014$prevRatio == 0 & data2014$postRatio ==0),]
data2014deletes <- data2014[ which(data2014$prevRatio != 0 & data2014$postRatio ==0),]
data2014additions <- data2014[ which(data2014$prevRatio == 0 & data2014$postRatio !=0),]
data2014modifications <- data2014[ which(data2014$prevRatio != 0 & data2014$postRatio !=0),]

nrow(data2014meaningLess)+nrow(data2014deletes)+nrow(data2014additions)+nrow(data2014modifications)

data201504meaningLess <- data201504[ which(data201504$prevRatio == 0 & data201504$postRatio ==0),]
data201504deletes <- data201504[ which(data201504$prevRatio != 0 & data201504$postRatio ==0),]
data201504additions <- data201504[ which(data201504$prevRatio == 0 & data201504$postRatio !=0),]
data201504modifications <- data201504[ which(data201504$prevRatio != 0 & data201504$postRatio !=0),]

nrow(data201504meaningLess)+nrow(data201504deletes)+nrow(data201504additions)+nrow(data201504modifications)

data201510meaningLess <- data201510[ which(data201510$prevRatio == 0 & data201510$postRatio ==0),]
data201510deletes <- data201510[ which(data201510$prevRatio != 0 & data201510$postRatio ==0),]
data201510additions <- data201510[ which(data201510$prevRatio == 0 & data201510$postRatio !=0),]
data201510modifications <- data201510[ which(data201510$prevRatio != 0 & data201510$postRatio !=0),]

nrow(data201510meaningLess)+nrow(data201510deletes)+nrow(data201510additions)+nrow(data201510modifications)

data201610meaningLess <- data201610[ which(data201610$prevRatio == 0 & data201610$postRatio ==0),]
data201610deletes <- data201610[ which(data201610$prevRatio != 0 & data201610$postRatio ==0),]
data201610additions <- data201610[ which(data201610$prevRatio == 0 & data201610$postRatio !=0),]
data201610modifications <- data201610[ which(data201610$prevRatio != 0 & data201610$postRatio !=0),]

nrow(data201610meaningLess)+nrow(data201610deletes)+nrow(data201610additions)+nrow(data201610modifications)

# data2014modifications$otherDeltaRatio <- 
#   (-data2014modifications$deltaRatio)/data2014modifications$prevRatio
# data201504modifications$otherDeltaRatio <- 
#   (-data201504modifications$deltaRatio)/data201504modifications$prevRatio
# data201510modifications$otherDeltaRatio <- 
#   (-data201510modifications$deltaRatio)/data201510modifications$prevRatio
# data201610modifications$otherDeltaRatio <- 
#   (-data201610modifications$deltaRatio)/data201610modifications$prevRatio


pdf("graphs.pdf")
dev.off()
# drawing
numBreaks=50

diffModsTitle <- "diff(bits) (mods)"
diffAddsTitle <- "diff(bits) (adds)"
diffAllTitle <- "diff(bits) (all)"
diffDelTitle <- "diff(bits) (del)"

titleString <-diffModsTitle

#deltas2014 <- data2014deletes$deltaD
#deltas201504 <- data201504deletes$deltaD
#deltas201510 <- data201510deletes$deltaD
#deltas201610 <- data201610deletes$deltaD

# deltas2014 <- data2014additions$deltaD
# deltas201504 <- data201504additions$deltaD
# deltas201510 <- data201510additions$deltaD
# deltas201610 <- data201610additions$deltaD

 deltas2014 <- data2014modifications$deltaD
 deltas201504 <- data201504modifications$deltaD
 deltasMod201510 <- data201510modifications$deltaD
 deltasMod201610 <- data201610modifications$deltaD

 deltas201510 <- data201510$deltaD
 deltas201610 <- data201610$deltaD
 
 nrow(data201510[which(data201510$deltaD == 0),])
 
 
  deltasAdd201510 <- data201510additions$deltaD
  deltasAdd201610 <- data201610additions$deltaD
  
  deltasDel201510 <- data201510deletes$deltaD
  deltasDel201610 <- data201610deletes$deltaD
 
 
# deltas2014 <- data2014$deltaD
# deltas201504 <- data201504$deltaD
# deltas201510 <- data201510$deltaD
# deltas201610 <- data201610$deltaD

#altogether = data.frame(deltas2014, 
#                        deltas201504, 
#                        deltas201510, 
#                        deltas201610)

altogether = data.frame(deltas201510, 
                        deltas201610)
library(plyr)
renamed=rename(altogether, c("deltas201510"="DBpedia 2015-10", "deltas201610"="DBpedia 2016-10"))
boxplot(renamed, outline = FALSE, ylab="Delta (q,q') (bits)", xlab="Versions")
title("Distribution of Updates - Including 0's")



altogetherAdd = data.frame(deltasAdd201510, deltasAdd201610)
renamedAdd=rename(altogetherAdd, c("deltasAdd201510"="DBpedia 2015-10", "deltasAdd201610"="DBpedia 2016-10"))
boxplot(renamedAdd, outline = FALSE, ylab="Delta (q,q') (bits)", xlab="Versions")
title("Distribution - Additions")

altogetherDel = data.frame(deltasDel201510, deltasDel201610)
renamedDel=rename(altogetherDel, c("deltasDel201510"="DBpedia 2015-10", "deltasDel201610"="DBpedia 2016-10"))
boxplot(renamed, outline = FALSE, ylab="Delta (q,q') (bits)", xlab="Versions")
title("Distribution - Deletions")

par(mfrow=c(2,2),oma=c(0,0,0,0))
?par


?seq
sortedData2015 <- data201510[order(data201510$updateID),]
sortedData2015$tempOrder <- seq(1:nrow(sortedData2015))
sortedData2015 <- sortedData2015[which(sortedData2015$deltaD != 0),]

sortedData2016 <- data201610[order(data201610$updateID),]
sortedData2016$tempOrder <- seq(1:nrow(sortedData2016))
sortedData2016 <- sortedData2016[which(sortedData2016$deltaD != 0), ]

?boxplot

layout(matrix(c(1,2,2,3,2,2,3,2,2,3,4,4,3,4,4,5,4,4), 6, 3, byrow=TRUE))
layout.show(5)
plot.new()
par(cex.lab=1.5) # is for y-axis
par(cex.axis=1.5) # is for x-axis

boxplot(sortedData2015$deltaD, sortedData2016$deltaD, outline = FALSE, 
          ylab="Delta (q,q') (bits)", xlab="Versions", names=c("DBpedia 2015-10", "DBpedia 2016-10"))

title("Distribution of Updates",cex.lab=4)
?title
quantile(sortedData2015$deltaD)
upperOutlier2015 <- 32.3273 + 1.5 * (32.3273 + 93.9779)
lowerOutlier2015 <- -93.9779 - 1.5 * (32.3273 + 93.9779)

quantile(sortedData2016$deltaD)
quantile(sortedData2015$deltaD, probs=seq(0,1, 0.1))
upperOutlier2016 <- 23.797 + 1.5 * (23.797 + 32.737)
lowerOutlier2016 <- -32.737 - 1.5 * (23.797 + 32.737)
quantile(filteredData2016$deltaD, probs=seq(0,1,0.1))

filteredData2015 = sortedData2015[which(sortedData2015$deltaD > upperOutlier2015 | sortedData2015$deltaD < lowerOutlier2015), ]
filteredData2016Same2015 = sortedData2016[filteredData2015$updateID,]
filteredData2016 = sortedData2016[which(sortedData2016$deltaD > upperOutlier2016 | sortedData2016$deltaD < lowerOutlier2016), ]
filteredData2015Same2016 = sortedData2015[filteredData2016$updateID,]

maxValue = max(filteredData2015$deltaD)
minValue = min(filteredData2015$deltaD)

par(mfrow=c(2,1),mar=c(4,4,3,1), oma=c(0,0,2,0))
plot(x=filteredData2015$tempOrder, y=filteredData2015$deltaD, 
     pch=16,cex=0.4, ylab="Delta (q,q') (bits)", xlab="Deltas against DBpedia 2015-10 (six month updates)", ylim = c(minValue, maxValue))
title("Update Evaluation - 2015-10 Outliers")
plot(x=filteredData2016Same2015$tempOrder, y=filteredData2016Same2015$deltaD, 
     pch=16,cex=0.4, ylab="Delta (q,q') (bits)", xlab="Deltas against DBpedia 2016-10 (six month updates)", ylim = c(minValue, maxValue))

maxValueOutliers = max(filteredData2015$deltaD, filteredData2016$deltaD)
minValueOutliers = min(filteredData2015$deltaD, filteredData2016$deltaD)
par(mfrow=c(2,1),  mar=c(4,4,3,1), oma=c(0,0,2,0))
plot(x=filteredData2015$tempOrder, y=filteredData2015$deltaD, 
     pch=16,cex=0.4, ylab="Delta (q,q') (bits)", xlab="Deltas against DBpedia 2015-10 (six month updates)", ylim = c(minValue, maxValue))
title("Update Evaluation - Each Version's Outliers")
plot(x=filteredData2016$tempOrder, y=filteredData2016$deltaD, 
     pch=16,cex=0.4, ylab="Delta (q,q') (bits)", xlab="Deltas against DBpedia 2016-10 (six month updates)", ylim = c(minValue, maxValue))

maxValueOutliers = max(filteredData2016$deltaD)
minValueOutliers = min(filteredData2016$deltaD)

par(mfrow=c(2,1),  mar=c(4,4,3,1), oma=c(0,0,2,0))
plot(x=filteredData2015Same2016$tempOrder, y=filteredData2015Same2016$deltaD, 
     pch=16,cex=0.4, ylab="Delta (q,q') (bits)", xlab="Deltas against DBpedia 2015-10 (six month updates)", ylim = c(minValue, maxValue))
title("Update Evaluation - 2016-10 Outliers")
plot(x=filteredData2016$tempOrder, y=filteredData2016$deltaD, 
     pch=16,cex=0.4, ylab="Delta (q,q') (bits)", xlab="Deltas against DBpedia 2016-10 (six month updates)", ylim = c(minValue, maxValue))


auxHist2014 = hist(deltas2014,numBreaks, plot=F)
auxHist201504 = hist(deltas201504,numBreaks, plot=F)
auxHist201510 = hist(deltas201510,numBreaks, plot=F)
auxHist201610 = hist(deltas201610,numBreaks, plot=F)

yLimits = c(0,max(auxHist2014$counts, auxHist201504$counts, auxHist201510$counts, auxHist201610$counts))

# layout(matrix(c(1,2,3,4), 2, 2, byrow=TRUE))

maxValue = floor (max(altogether))
minValue = ceiling(min(altogether))
xLimits = c(minValue, maxValue)


# for hist with density 
par(mfrow=c(2,2),oma=c(0,0,2,0))
hist(deltas2014,numBreaks, xlim=xLimits, ylim=yLimits, freq = T)
hist(deltas201504,numBreaks, xlim=xLimits, ylim=yLimits, freq = T)
hist(deltas201510,numBreaks, xlim=xLimits, ylim= yLimits, freq = T)
hist(deltas201610,numBreaks, xlim=xLimits, ylim=yLimits, freq = T)
mtext(titleString , outer = TRUE, cex = 1.5)

auxHist2014 = hist(deltas2014,numBreaks, plot=F, freq = F)
auxHist201504 = hist(deltas201504,numBreaks, plot=F, freq = F)
auxHist201510 = hist(deltas201510,numBreaks, plot=F, freq = F)
auxHist201610 = hist(deltas201610,numBreaks, plot=F, freq = F)

yLimits = c(0,max(auxHist2014$density, auxHist201504$density, auxHist201510$density, auxHist201610$density))

# layout(matrix(c(1,2,3,4), 2, 2, byrow=TRUE))
maxValue = floor (max(altogether))
minValue = ceiling(min(altogether))
xLimits = c(minValue, maxValue)

par(mfrow=c(2,2),oma=c(0,0,2,0))
hist(deltas2014,numBreaks, xlim=xLimits, ylim=yLimits, freq = F)
hist(deltas201504,numBreaks, xlim=xLimits, ylim=yLimits, freq = F)
hist(deltas201510,numBreaks, xlim=xLimits, ylim= yLimits, freq = F)
hist(deltas201610,numBreaks, xlim=xLimits, ylim=yLimits, freq = F)
mtext(titleString, outer = TRUE, cex = 1.5)

#density graphs
par(mfrow=c(2,2),oma=c(0,0,2,0))
d2014<-density(deltas2014)
d201504<-density(deltas201504)
d201510<-density(deltas201510)
d201610<-density(deltas201610)
minValue = min(d2014$y, d201504$y, d201510$y, d201610$y)
maxValue = max(d2014$y, d201504$y, d201510$y, d201610$y)
yLimits = c(minValue, maxValue)
minValue = min(d2014$x, d201504$x, d201510$x, d201610$x)
maxValue = max(d2014$x, d201504$x, d201510$x, d201610$x)
xLimits = c(minValue, maxValue)

plot(d2014, xlim=xLimits, ylim=yLimits )
plot(d201504, xlim=xLimits, ylim=yLimits )
plot(d201510, xlim=xLimits, ylim=yLimits )
plot(d201610, xlim=xLimits, ylim=yLimits )
mtext(titleString, outer = TRUE, cex = 1.5)

# k clustering k = 10

deltas2014Scaled <- scale(deltas2014)
deltas201504Scaled <- scale(deltas201504)
deltas201510Scaled <- scale(deltas201510)
deltas201610Scaled <- scale(deltas201610)



par(mfrow=c(2,2),oma=c(0,0,2,0))
plotClusterNumberAnalysis(deltas2014Scaled, "deltas2014", titleString)
plotClusterNumberAnalysis(deltas201504Scaled, "deltas201504" , titleString)
plotClusterNumberAnalysis(deltas201510Scaled, "deltas201510", titleString)
plotClusterNumberAnalysis(deltas201610Scaled, "deltas201610", titleString)

mtext(titleString , outer = TRUE, cex = 1.5)

par(mfrow=c(2,2),oma=c(0,0,2,0))
kClusters2014 <- getAndPlotKClusterBreaks(deltas2014, 15, T, "deltas2014")
kClusters201504 <- getAndPlotKClusterBreaks(deltas201504, 15, T, "deltas201504")
kClusters201510 <- getAndPlotKClusterBreaks(deltas201510, 15, T, "deltas201510")
kClusters201610 <- getAndPlotKClusterBreaks(deltas201610, 15, T, "deltas201610")
mtext(titleString , outer = TRUE, cex = 1.5)


kClusters <- data.frame(kClusters2014, kClusters201504, kClusters201510, kClusters201610)

grid.arrange(textGrob(titleString), tableGrob(kClusters), heights=c(1,5), nrow=2)


# tables 
# library(grid)
# library(gridExtra)
# library(ggplot2)
# plot.new()
# par(mfrow=c(4,3),oma=c(0,0,2,0))
# 
# plotTableCross(data2014, "data2014")
# plotTableCross(data2014, "data2014")
# plotTableCross <- function(data, graphTitle) {
#   
#   rowsNames = c('deltaCT<0', 'deltaCT=0', 'deltaCT>0')
#   colsNames = c('deltaSCT<0', 'deltaSCT=0', 'deltaSCT>0')
#   
#   positive = matrix(NA, 3,3)
#   rownames(positive) <- rowsNames
#   colnames(positive) <- colsNames
#   positive[1,1] = nrow(data[ which( data$deltaD>0 & data$deltaCT <0 & data$deltaSCT<0), ])
#   positive[1,2] = nrow(data[ which( data$deltaD>0 & data$deltaCT <0 & data$deltaSCT==0), ])
#   positive[1,3] = nrow(data[ which( data$deltaD>0 & data$deltaCT <0 & data$deltaSCT>0), ])
#   
#   positive[2,1] = nrow(data[ which( data$deltaD >0 & data$deltaCT == 0 & data$deltaSCT<0), ])
#   positive[2,2] = nrow(data[ which( data$deltaD >0 & data$deltaCT == 0 & data$deltaSCT==0), ])
#   positive[2,3] = nrow(data[ which( data$deltaD >0 & data$deltaCT == 0 & data$deltaSCT>0), ])
#   
#   positive[3,1] = nrow(data[ which( data$deltaD >0 & data$deltaCT >0 & data$deltaSCT<0), ])
#   positive[3,2] = nrow(data[ which( data$deltaD >0 & data$deltaCT >0 & data$deltaSCT==0), ])
#   positive[3,3] = nrow(data[ which( data$deltaD >0 & data$deltaCT >0 & data$deltaSCT>0), ])
#   
#   neutral = matrix(NA, 3,3)
#   rownames(neutral) <- rowsNames
#   colnames(neutral) <- colsNames
#   neutral[1,1] = nrow(data[ which( data$deltaD==0 & data$deltaCT <0 & data$deltaSCT<0), ])
#   neutral[1,2] = nrow(data[ which( data$deltaD==0 & data$deltaCT <0 & data$deltaSCT==0), ])
#   neutral[1,3] = nrow(data[ which( data$deltaD==0 & data$deltaCT <0 & data$deltaSCT>0), ])
#   
#   neutral[2,1] = nrow(data[ which( data$deltaD==0 & data$deltaCT == 0 & data$deltaSCT<0), ])
#   neutral[2,2] = nrow(data[ which( data$deltaD==0 & data$deltaCT == 0 & data$deltaSCT==0), ])
#   neutral[2,3] = nrow(data[ which( data$deltaD==0 & data$deltaCT == 0 & data$deltaSCT>0), ])
#   
#   neutral[3,1] = nrow(data[ which( data$deltaD==0 & data$deltaCT >0 & data$deltaSCT<0), ])
#   neutral[3,2] = nrow(data[ which( data$deltaD==0 & data$deltaCT >0 & data$deltaSCT==0), ])
#   neutral[3,3] = nrow(data[ which( data$deltaD==0 & data$deltaCT >0 & data$deltaSCT>0), ])
#   
#   negative = matrix(NA, 3,3)
#   rownames(negative) <- rowsNames
#   colnames(negative) <- colsNames
#   negative[1,1] = nrow(data[ which( data$deltaD<0 & data$deltaCT <0 & data$deltaSCT<0), ])
#   negative[1,2] = nrow(data[ which( data$deltaD<0 & data$deltaCT <0 & data$deltaSCT==0), ])
#   negative[1,3] = nrow(data[ which( data$deltaD<0 & data$deltaCT <0 & data$deltaSCT>0), ])
#   
#   negative[2,1] = nrow(data[ which( data$deltaD<0 & data$deltaCT == 0 & data$deltaSCT<0), ])
#   negative[2,2] = nrow(data[ which( data$deltaD<0 & data$deltaCT == 0 & data$deltaSCT==0), ])
#   negative[2,3] = nrow(data[ which( data$deltaD<0 & data$deltaCT == 0 & data$deltaSCT>0), ])
#   
#   negative[3,1] = nrow(data[ which( data$deltaD<0 & data$deltaCT >0 & data$deltaSCT<0), ])
#   negative[3,2] = nrow(data[ which( data$deltaD<0 & data$deltaCT >0 & data$deltaSCT==0), ])
#   negative[3,3] = nrow(data[ which( data$deltaD<0 & data$deltaCT >0 & data$deltaSCT>0), ])
#   
#   lay <- rbind(c(1,1,1),
#                 c(2,3,4))
#   grid.arrange(textGrob(graphTitle), tableGrob(positive), tableGrob(neutral), tableGrob(negative), layout_matrix=lay)
#   
# 
# }
# 
# 
# 
# # 
# # library(cluster)
# # library(fpc)
# # plotClusterInfo <- function (numClusters, data) {
# # # K-Means Cluster Analysis
# # fit <- kmeans(data, numClusters) # 5 cluster solution
# # # get cluster means 
# # aggregate(data,by=list(fit$cluster),FUN=mean)
# # 
# # # append cluster assignment
# # #localData <- data.frame(data, fit$cluster)
# # 
# # clusplot(data, fit$cluster, color=TRUE, shade=TRUE, 
# #          labels=2, lines=0)
# # 
# # # Centroid Plot against 1st 2 discriminant functions
# # #plotcluster(data, fit$cluster)
# # }
# # 
# # plotClusterInfo(15,deltas2014Scaled)
# # 
# # layout(matrix(c(1), 1, 1, byrow = TRUE))
# # 
# # hist(deltas2014,col='skyblue',border=F, breaks=100, main="Histograms", xlab="Compression deltas", xlim=xLimits, ylim=yLimits)
# # hist(deltas201510,add=T, col=scales::alpha('red', .5), border=F,breaks=100, xlim=xLimits, ylim=yLimits)
# # hist(deltas201610,add=T, col=scales::alpha('yellow', .5), border=F,breaks=100, xlim=xLimits, ylim=yLimits)
# # legend("topright", c("2014","2015-10","2016-10"), col=c("skyblue","red","yellow"), fill=c("skyblue","red","yellow"))

# using k-clustering
goodCluster2015 <- data201510[ which (data201510$deltaD > 1052),]

examples <- sample.int(nrow(goodCluster2015), 5)
goodCluster2015[examples,]

# using outliers
quantile(data201510$deltaD)
#fences Q3 + 1.5 * IQ
distance = 1.4871 + 1.5*(1.4871+7.3503)
distanceLower
distance = 69.1440
goodOutliers <- data201510[ which (data201510$deltaD > distance), ]
goodExamples <- goodOutliers[ sample.int(nrow(goodOutliers), 5), ]
badOutliers <- data201510[ which (data201510$deltaD < -166.0089),]
badExamples <- badOutliers [sample.int(nrow(badOutliers), 5), ]
badExamples
goodExamples$updateID
badExamples$updateID

quantile (data201510$prevCodSizeSCT)
smallUpdates <- data201510[which(data201510$prevCodSizeSCT <200),]
quantile(smallUpdates$deltaD, probs=seq(0,1,0.05))
smallGoodOutliers <- smallUpdates[ which(smallUpdates$deltaD > 22),]
smallBadOutliers <- smallUpdates [ which(smallUpdates$deltaD < -14),]
smallGoodExamples <- smallGoodOutliers[sample.int(nrow(smallGoodOutliers), 5), ]
smallBadExamples <- smallBadOutliers[sample.int(nrow(smallBadOutliers), 5), ]
smallGoodExamples$updateID
smallBadExamples$updateID

smallBadExamples2016 <- data201610[smallBadExamples$updateID,
                                   ]
smallBadExamples2016 
smallBadExamples
smallGoodExamples2016 <- data201610[smallGoodExamples$updateID,]

veryGoodExamples <- smallUpdates[ which (smallUpdates$prevCodSizeSCT < smallUpdates$postCodSizeSCT & smallUpdates$postCodSize < smallUpdates$prevCodSize),]
veryBadExamples <- smallUpdates[ which(smallUpdates$prevCodSizeSCT> smallUpdates$postCodSizeSCT & smallUpdates$postCodSize > smallUpdates$prevCodSize),]

veryGoodOutliers <- veryGoodExamples[ which(veryGoodExamples$deltaD > 22),]
veryBadOutliers <- veryBadExamples[ which (veryBadExamples$deltaD< -10),]

values <- data201510[ which (data201510$prevCodSizeSCT != 0), ]
quantile(values$prevCodSize/values$prevCodSizeSCT, probs=seq(0,1,0.1))
values <- smallUpdates[ which(smallUpdates$prevCodSizeSCT != 0),]
smallPrevCompressedUpdates <- values[which(values$prevCodSize/values$prevCodSizeSCT < 0.30),]
veryGoodExamplesCompressed <- smallPrevCompressedUpdates[ 
  which (smallPrevCompressedUpdates$prevCodSizeSCT < smallPrevCompressedUpdates$postCodSizeSCT 
         & smallPrevCompressedUpdates$postCodSize < smallPrevCompressedUpdates$prevCodSize),]
veryBadExamplesCompressed <- smallPrevCompressedUpdates[ 
      which(smallPrevCompressedUpdates$prevCodSizeSCT> smallPrevCompressedUpdates$postCodSizeSCT 
              & smallPrevCompressedUpdates$postCodSize > smallPrevCompressedUpdates$prevCodSize),]
vgoc <- veryGoodExamplesCompressed[ which(veryGoodExamplesCompressed$deltaD > 22 & 
                                            veryGoodExamplesCompressed$X.prevTransactions != 
                                            veryGoodExamplesCompressed$X.postTransactions),]
vboc <- veryBadExamplesCompressed[ which (veryBadExamplesCompressed$deltaD< -10 & 
                                            veryBadExamplesCompressed$X.prevTransactions != 
                                            veryBadExamplesCompressed$X.postTransactions),]

vgocSample <- vgoc[sample.int(nrow(vgoc),2),]
vbocSample <- vboc[sample.int(nrow(vboc),5),]
write.table(rbind(vgoc, vboc), file="compressedExamples.txt")
