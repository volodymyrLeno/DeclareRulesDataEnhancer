# Declare Rules Data Enhancer - Version 1.1

The command-line tool for discovering data-aware rules for Declare constraints in declarative process discovery. Currently the system supports csv and xes formats. 
(CSV only) For simplicity, we assume that the log has the attribute names in the first row and first three attributes are caseID, activityName and timestamp. Then they are followed by payload. The payload shouldn't include any time and date type data (the distance metric for the date and time type is not yet implemented). 

## Content of this distribution

The initial distribution (DeclareRulesDataEnhancer.rar) includes:
* DeclareRulesDataEnhancer.jar - Java console application
* logs/ - the examples of synthetic logs

## Usage

The tool requires the following input parameters:
* logFile - absolute path to the event log (STRING)
* constraintsFile - absolute path to the file that contains the Declare constraints (STRING)
* considerViolations - specify whether violations of constaint have to be considered while searching for the rules (BOOLEAN)
* k - number of clusters for K-Medoids algorithm (INTEGER)
* minNodeSize - minimum relative number of instances covered by a rule (DOUBLE [0,1])
* pruning - trun of/on pruning in RIPPER algorithm (BOOLEAN)

The example of how to run the program:

```
java -jar DeclareRulesDataEnhancer.jar runningExample.csv constraints.txt false 2 0.05 true
```

## Requirements
Java 10 or above
