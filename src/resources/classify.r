library(RJDBC)
library(party)
library(gbm)
library(e1071)
library(class)
library(rpart)
library(randomForest)

# Load the database driver
jdbcDriver <- JDBC(driverClass = "org.apache.derby.jdbc.ClientDriver", classPath = "./derbyclient.jar")
# Connect to database
jdbcConnection <- dbConnect(jdbcDriver, "jdbc:derby://localhost:1527/DiplomaOddsDatabase", "diploma", "diploma")
# Read data
data <- dbReadTable(jdbcConnection, "CLEANEDDATA")
latestPred <- dbReadTable(jdbcConnection, "PREDICTIONS")
matches <- dbReadTable(jdbcConnection, "MATCHES")

# Read data
# data <- read.csv("./cleaned.csv")
# Convert the "CLASS" column to factor
data[,"CLASS"] <- factor(data[,"CLASS"])
data[,"HPREV1"] <- factor(data[,"HPREV1"])
data[,"HPREV2"] <- factor(data[,"HPREV2"])
data[,"HPREV3"] <- factor(data[,"HPREV3"])
data[,"HPREV4"] <- factor(data[,"HPREV4"])
data[,"APREV1"] <- factor(data[,"APREV1"])
data[,"APREV2"] <- factor(data[,"APREV2"])
data[,"APREV3"] <- factor(data[,"APREV3"])
data[,"APREV4"] <- factor(data[,"APREV4"])

# Remove the "MATCH_ID" column
# x <- data[, c(-2)]

# typeof(data[,"MATCH_ID"])

# Replace NAs with row means
k <- which(is.na(data[,3:24]), arr.ind=TRUE)
data[,3:24][k] <- data[,77][k[,1]]
k <- which(is.na(data[,25:46]), arr.ind=TRUE)
data[,25:46][k] <- data[,78][k[,1]]
k <- which(is.na(data[,47:68]), arr.ind=TRUE)
data[,47:68][k] <- data[,79][k[,1]]

# Remove rows with NAs
# x[,2:78] <- x[,2:78][complete.cases(x),]

if (sum(is.na(data$CLASS)) == 0) {
  stop('There are no matches in one week!')
}


matches_to_predict <- data[is.na(data$CLASS),]
matches_to_predict <- matches_to_predict[,"MATCH_ID"]
if (length(matches_to_predict) != 0) {
  predicted <- subset(latestPred, !(MATCH_ID %in% matches_to_predict))
} else {
  predicted <- latestPred
}
predicted <- predicted[,"MATCH_ID"]
predicted <- unique(predicted)
matches <- subset(matches, ID %in% predicted)
matches <- matches[order(matches$MATCH_DATE),]
#matches <- tail(matches, 10)
matches <- matches[,"ID"]
latestPred <- subset(latestPred, MATCH_ID %in% matches)

output <<- ""
data_types <- c("all", "avg", "odds", "prev", "10bet", "32red", "bet365", "betfred", "betrally", "betvictor", "boylesports", "comeon", "coral", "ladbrokes", "marathonbet", "skybet", "smartlive", "sportingbet", "spreadex", "stanjames", "titanbet", "unibet", "williamhill", "youbet", "betdaq", "betfair")
functions <- c("ctree", "gbm", "naiveBayes", "randomForest", "rpart", "svm", "knn")

select_data <- function(data, type = "all"){
  output <<- ""
  if (type == "all") {
    output <<- "Data: All"
    data
  }
  else if (type == "avg") {
    output <<- "Data: Average bookmakers"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_AVG", "DRAW_AVG", "AWAY_AVG")]
  }
  else if (type == "odds") {
    output <<- "Data: Only odds"
    data <- data[,1:68]
  }
  else if (type == "prev") {
    output <<- "Data: Only previous matches"
    data <- data[,c("MATCH_ID", "CLASS", "HPREV1", "HPREV2", "HPREV3", "HPREV4", "APREV1", "APREV2", "APREV3", "APREV4")]
  }
  else if (type == "10bet") {
    output <<- "Data: Only 10Bet"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_10BET", "DRAW_10BET", "AWAY_10BET")]
  }
  else if (type == "32red") {
    output <<- "Data: Only 32Red"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_32RED", "DRAW_32RED", "AWAY_32RED")]
  }
  else if (type == "bet365") {
    output <<- "Data: Only Bet365"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_BET365", "DRAW_BET365", "AWAY_BET365")]
  }
  else if (type == "betfred") {
    output <<- "Data: Only BetFred"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_BETFRED", "DRAW_BETFRED", "AWAY_BETFRED")]
  }
  else if (type == "betrally") {
    output <<- "Data: Only BetRally"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_BETRALLY", "DRAW_BETRALLY", "AWAY_BETRALLY")]
  }
  else if (type == "betvictor") {
    output <<- "Data: Only BetVictor"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_BETVICTOR", "DRAW_BETVICTOR", "AWAY_BETVICTOR")]
  }
  else if (type == "boylesports") {
    output <<- "Data: Only BoyleSports"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_BOYLESPORTS", "DRAW_BOYLESPORTS", "AWAY_BOYLESPORTS")]
  }
  else if (type == "comeon") {
    output <<- "Data: Only ComeOn"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_COMEON", "DRAW_COMEON", "AWAY_COMEON")]
  }
  else if (type == "coral") {
    output <<- "Data: Only Coral"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_CORAL", "DRAW_CORAL", "AWAY_CORAL")]
  }
  else if (type == "ladbrokes") {
    output <<- "Data: Only LadBrokes"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_LADBROKES", "DRAW_LADBROKES", "AWAY_LADBROKES")]
  }
  else if (type == "marathonbet") {
    output <<- "Data: Only MarathonBet"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_MARATHONBET", "DRAW_MARATHONBET", "AWAY_MARATHONBET")]
  }
  else if (type == "skybet") {
    output <<- "Data: Only SkyBet"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_SKYBET", "DRAW_SKYBET", "AWAY_SKYBET")]
  }
  else if (type == "smartlive") {
    output <<- "Data: Only SmartLive"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_SMARTLIVE", "DRAW_SMARTLIVE", "AWAY_SMARTLIVE")]
  }
  else if (type == "sportingbet") {
    output <<- "Data: Only SportingBet"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_SPORTINGBET", "DRAW_SPORTINGBET", "AWAY_SPORTINGBET")]
  }
  else if (type == "spreadex") {
    output <<- "Data: Only Spreadex"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_SPREADEX", "DRAW_SPREADEX", "AWAY_SPREADEX")]
  }
  else if (type == "stanjames") {
    output <<- "Data: Only StanJames"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_STANJAMES", "DRAW_STANJAMES", "AWAY_STANJAMES")]
  }
  else if (type == "titanbet") {
    output <<- "Data: Only TitanBet"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_TITANBET", "DRAW_TITANBET", "AWAY_TITANBET")]
  }
  else if (type == "unibet") {
    output <<- "Data: Only UniBet"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_UNIBET", "DRAW_UNIBET", "AWAY_UNIBET")]
  }
  else if (type == "williamhill") {
    output <<- "Data: Only WilliamHill"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_WILLIANHILL", "DRAW_WILLIANHILL", "AWAY_WILLIANHILL")]
  }
  else if (type == "youbet") {
    output <<- "Data: Only YouBet"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_YOUBET", "DRAW_YOUBET", "AWAY_YOUBET")]
  }
  else if (type == "betdaq") {
    output <<- "Data: Only BetDaq"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_BETDAQ", "DRAW_BETDAQ", "AWAY_BETDAQ")]
  }
  else if (type == "betfair") {
    output <<- "Data: Only BetFair"
    data <- data[,c("MATCH_ID", "CLASS", "HOME_BETFAIR", "DRAW_BETFAIR", "AWAY_BETFAIR")]
  }
}

classification <- function(data, data_type, distribution = NULL, n.trees = NULL, interaction.depth = NULL, shrinkage = NULL, FUN){
  selected_data <- select_data(data, data_type)
  
  data_train <- selected_data[!is.na(selected_data$CLASS),]
  data_test <- selected_data[is.na(selected_data$CLASS),]
  data_train <- data_train[complete.cases(data_train),]
  data_train <- data_train[,!names(data_train) %in% c("MATCH_ID")]
  match_ids <- as.integer(data_test[,"MATCH_ID"])
  data_test <- data_test[,!names(data_test) %in% c("MATCH_ID")]
  
  set.seed(5)
  
  # Make model
  #--------------------------------------------------------------------
  if (FUN == "ctree") {
    tree_control <- ctree_control(mincriterion = 0, minsplit = 10, minbucket = 10, maxdepth = 3)
    model <- ctree(CLASS ~ ., data_train, controls = tree_control)
  }
  else if (FUN == "gbm") {
    model <- gbm(CLASS ~ ., data = data_train, distribution = "multinomial", n.trees =  1000 , interaction.depth =  3 , shrinkage =  1e-04 )
  }
  else if (FUN == "naiveBayes") {
    model <- naiveBayes(CLASS ~ ., data = data_train)
  }
  else if (FUN == "randomForest") {
    CLASS <- data_test[,1]
    data_test <- data_test[,3:ncol(data_test)]
    data_test <- data_test[,!apply(data_test, 2, function(x) any(is.na(x)))]
    data_test <- cbind(CLASS, data_test)
    data_train <- data_train[,names(data_train) %in% names(data_test)]
    model <- randomForest(CLASS ~ ., data=data_train, nTree=300, maxnodes=20)
  }
  else if (FUN == "rpart") {
    tree_control <- rpart.control(minsplit = 10, maxdepth = 10, cp = 0.005)
    model <- rpart(CLASS ~ ., data_train, control = tree_control)
  }
  else if (FUN == "svm") {
    # tune <- tune.svm(CLASS~., data=data_train, gamma=10^(-6:-1), cost=10^(1:4))
    CLASS <- data_test[,1]
    data_test <- data_test[,3:ncol(data_test)]
    data_test <- data_test[,!apply(data_test, 2, function(x) any(is.na(x)))]
    data_test <- cbind(CLASS, data_test)
    data_train <- data_train[,names(data_train) %in% names(data_test)]
    model <- svm(CLASS ~ ., data = data_train, method = "C-classification", kernel = "radial", probability=T, gamma=0.01, cost=0.1)
    # model <- svm(CLASS ~ ., data = data_train, method = "C-classification", kernel = "radial", probability=T, gamma=tune$best.parameters$gamma, cost=tune$best.parameters$cost)
  }
  else if (FUN == "knn") {
    CLASS <- data_test[,1]
    data_test <- data_test[,3:ncol(data_test)]
    data_test <- data_test[,!apply(data_test, 2, function(x) any(is.na(x)))]
    data_test <- cbind(CLASS, data_test)
    data_train <- data_train[,names(data_train) %in% names(data_test)]
  }
  else {
    model <- FUN(CLASS ~ ., data = data_train, distribution = distribution, n.trees = n.trees, interaction.depth = interaction.depth, shrinkage = shrinkage)
  }
  #--------------------------------------------------------------------
  
  # Predict a result from the model
  #--------------------------------------------------------------------
  if (FUN == "knn") {
    # data_test[,1][is.na(data_test[,1])] <- 0
    if (data_type != "all") {
      pred <- knn(as.matrix(data_train[,-1]), as.matrix(data_test[,-1]), as.vector(data_train[,1]), k=10)
    }
    else {
      return(match_preds <- data.frame())
    }
  }
  else if (FUN == "ctree" || FUN == "gbm") {
    pred <- predict(model, data_test, type = "response", n.trees = 1000)
  }
  else {
    pred <- predict(model, data_test, type = "class")
  }
  
  if (FUN == "gbm") {
    model_results <- character()
    levels <- attr(pred, "dimnames")[[2]]
    for (i in 1:as.integer(length(pred)/3)) {
      prediction <- as.vector(pred[i,,])
      names(prediction) <- levels
      prediction <- names(prediction[match(max(prediction),prediction)])
      
      # Save the predicted result
      model_results <- c(model_results, as.character(prediction))
    }
    pred <- model_results
  }
  #--------------------------------------------------------------------
  pred <- as.factor(pred)
  datas <- character()
  functs <- character()
  for (i in 1:length(pred)) {
    datas <- c(datas, data_type)
    functs <- c(functs, FUN)
  }
  match_preds <- data.frame(match_ids, datas, functs, pred)
}

df <- data.frame()
for (data_type in data_types) {
  for (funct in functions) {
    print(data_type)
    print(funct)
    results <- classification(data = data, data_type = data_type, FUN = funct)
    print(results)
    df <- rbind(df, results)
  }
}
names(df) <- c("MATCH_ID", "DATA", "ALGORITHM", "PRED_RESULT")
# df[,"MATCH_ID"] <- as.character(df[,"MATCH_ID"])

df <- rbind(latestPred, df)

# Write out the table
dbWriteTable(jdbcConnection, "PREDICTIONS", df)

# Close the database connection
dbDisconnect(jdbcConnection)
rm(list=ls())
gc()