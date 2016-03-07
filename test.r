library(RJDBC)
library(class)
library(e1071)

# Load the database driver
jdbcDriver <- JDBC(driverClass = "org.apache.derby.jdbc.ClientDriver", classPath = "./derbyclient.jar")
# Connect to database
jdbcConnection <- dbConnect(jdbcDriver, "jdbc:derby://localhost:1527/DiplomaOddsDatabase", "kiskacsa08", "kiskacsa")
# Read data
data <- dbReadTable(jdbcConnection, "CLEANEDDATA")

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

# Replace NAs with row means
k <- which(is.na(data[,3:24]), arr.ind=TRUE)
data[,3:24][k] <- data[,77][k[,1]]
k <- which(is.na(data[,25:46]), arr.ind=TRUE)
data[,25:46][k] <- data[,78][k[,1]]
k <- which(is.na(data[,47:68]), arr.ind=TRUE)
data[,47:68][k] <- data[,79][k[,1]]

data_train <- data[!is.na(data$CLASS),]
data_test <- data[is.na(data$CLASS),]
match_ids <- data_test[,"MATCH_ID"]
# CLASS <- data_test[,1]
# data_test <- data_test[,3:ncol(data_test)]
# data_test <- data_test[,!apply(data_test, 2, function(x) any(is.na(x)))]
# data_test <- cbind(CLASS, data_test)
data_train <- data_train[complete.cases(data_train),]
data_train <- data_train[,!names(data_train) %in% c("MATCH_ID")]
data_test <- data_test[,!names(data_test) %in% c("MATCH_ID")]
# data_train <- data_train[,names(data_train) %in% names(data_test)]

set.seed(5)
model <- randomForest(CLASS ~ ., data=data_train, nTree=300, maxnodes=20)
# data_test[,1][is.na(data_test[,1])] <- 0
# data_train[,c("HPREV1", "HPREV2", "HPREV3", "HPREV4", "APREV1", "APREV2", "APREV3", "APREV4")] <- sapply(data_train[,c("HPREV1", "HPREV2", "HPREV3", "HPREV4", "APREV1", "APREV2", "APREV3", "APREV4")], as.factor)
# data_test[,c("HPREV1", "HPREV2", "HPREV3", "HPREV4", "APREV1", "APREV2", "APREV3", "APREV4")] <- sapply(data_test[,c("HPREV1", "HPREV2", "HPREV3", "HPREV4", "APREV1", "APREV2", "APREV3", "APREV4")], as.factor)
pred <- predict(model, data_test, type = "class")
print(pred)
