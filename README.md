# OverOps Functions

Public OverOps User Defined Functions

Full manifest of all libraries can be found here - https://git.io/fxDIW

## Our Collection consists of two types of functions:
### Anomaly Functions:
An Anomaly UDF is a function that would be used to determine whether the volume of specific events is considered anomalous by comparing it to a target threshold within a view/timeframe. Anomaly function will run periodically (the rate is adjustable - i.e. each 1/5/15... minutes) and search for anomalies.
Read more about Anomaly functions at: https://doc.overops.com/docs/managing-and-creating-alerts#section-anomaly-functions.
### Channel Functions:
The Function-alerting channel in “Alert Settings” screen enables to select a function to activate when alert is triggered.
Read more about Channel functions at: https://doc.overops.com/docs/managing-and-creating-alerts#section-channel-functions.


## List of UDF libraries in this repository:

### Anomaly Functions
#### 1. Relative Threshold -
Compare the event volume within the view against a target threshold and rate. The rate is defined as number of events / throughput. Throughput can set as the number of times the method containing the event was called, or the number of times the application thread calling into the event executed.
See code at: https://git.io/fx6sl
#### 2. Automatic Entry point Timers -
Automatically set timers on application entry points based on average runtime.
See code at: https://git.io/fxiy0
#### 3. Severity -
Mark events as New if they were introduced in an active deployment and are important. Mark events as Regressed if they have crossed a volume threshold against a previous timeframe.
See code at: https://git.io/fxiSl

### Channel Functions:
#### 1. Routing -
Classify incoming events according to the functional component within the code (Code Tier) from which they originated. The classification will be done by adding a label to events which belong to the same Code Tier (for example - "aws.lambda", "Java-lang", "Network Errors", etc..)
See code at: https://git.io/fx6s8
#### 2. Apply Label -
Applies a specific input label to events.
See code at: https://git.io/fx6sc

## UDF Structure

Every UDF must have two methods: `validateInput` and `execute`. Optionally, there may also be an `install` method.

```java
public class MyFunction {

  // required - return string if valid, throw exception if not
  public static String validateInput(String rawInput) {
    return getMyInput(rawInput).toString();
  }

  // required - this method is called when the UDF is executed
  public static void execute(String rawContextArgs, String rawInput) {

    // parse raw parameter input
    MyInput input = getMyInput(rawInput);

    // parse context
    ContextArgs args = (new Gson()).fromJson(rawContextArgs, ContextArgs.class);

    // get an API Client
    ApiClient apiClient = args.apiClient();

    //
    //  Make API calls
    //
    //  "meat and potatoes" of the UDF goes here
    //

  }

  // optional - this method is called when the UDF is applied to a view
  public static void install(String rawContextArgs, String rawInput) {

    //
    //  This code is run once
    //  For example, retroactively apply the function to historic data
    //

  }

  // helper for parsing input parameters
  private static MyInput getMyInput(String rawInput) {

    // params cannot be empty
    if (Strings.isNullOrEmpty(rawInput))
      throw new IllegalArgumentException("Input is empty");

    MyInput input;

    // parse params
    try {
      input = MyInput.of(rawInput);
    } catch (Exception e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }

    // validate input
    if (input.foo <= 0)
      throw new IllegalArgumentException("'foo' must be positive");

    return input;
  }

  // extend Input to easily parse parameters
  static class MyInput extends Input {

    // input parameters
    public int foo;

    // parse input
    private MyInput(String raw) {
      super(raw);
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();

      builder.append("MyUDF (");
      builder.append(foo);
      builder.append(")");

      return builder.toString();
    }

    static MyInput of(String raw) {
      return new MyInput(raw);
    }

  }

}
```

For more details on UDFs and how to write your own, see [User Defined Functions](https://github.com/takipi-field/udf). UDFs can be uploaded through the OverOps UI or with the [UDF Uploader](https://github.com/takipi/udf-uploader/).