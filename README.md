# overops-functions
Public OverOps User Defined Functions

Full manifest of all libraries can be found here - https://git.io/fxDIW

## Our Collection consists of two types of functions:
### Anomaly Functions:
An Anomaly UDF is a function that would be used to determine whether the volume of specific events is considered anomalous by comparing it to a target threshold within a view/timeframe.
Read more about Anomaly functions at: https://doc.overops.com/docs/managing-and-creating-alerts#section-anomaly-functions.
### Channel Functions:
The Function-alerting channel in “Alert Settings” screen enables to select a function to activate when alert is triggered.
Read more about Channel functions at: https://doc.overops.com/docs/managing-and-creating-alerts#section-anomaly-functions.


## List of UDF libraries in this repository:

### Anomaly Functions
#### 1. Relative Threshold -
Compare the event volume within the view against a target threshold and rate. The rate is defined as number of events / throughput. Throughput can set as the number of times the method containing the event was called, or the number of times the application thread calling into the event executed.
See code at: https://git.io/fx6sl
#### 2. Automatic Entry point Timers -
Automatically set timers on application entry points based on average runtime.
See code at: https://git.io/
#### 3. Severity -
Mark events as New if they were introduced in an active deployment and are important. Mark events as Regressed if they have crossed a volume threshold against a previous timeframe.
See code at: https://git.io/fxiSl

### Channel Functions:
#### 1. Routing -
Classify incoming events according to the functional component within the code from which they originated.
See code at: https://git.io/fx6s8
#### 2. Apply Label -
Applies a specific input label to events.
See code at: https://git.io/fx6sc
