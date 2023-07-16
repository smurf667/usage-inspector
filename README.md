# Usage Inspector

The is a [Java Agent](https://www.developer.com/design/what-is-java-agent/) that can record
the number of method calls on classes.

I've created this purely out of curiosity, but may use it in a different project which
visualises code quality based on different metrics. Real-life usage data might be combined
to emphasise often-used classes and to de-emphasise rarely-used classes.

To build the agent, simply run

    mvn package

I've included a rudimentary test which can be executed via

    mvn verify

The agent can be configured on the command line or by a JSON file.
With defaults, the collected data will be output to `System.err` at the shutdown of the VM.

To add the agent and potentially arguments for the agent, use

    java -javaagent:.../usage-inspector-0.1.0-SNAPSHOT.jar(=(optionName=optionValue:?)*)?

The following configuration options exist

| Name           | Meaning                                                  | Default      | Example command line   | JSON                                   |
|:--------------:|----------------------------------------------------------|:------------:|------------------------|----------------------------------------|
| `config`       | JSON configuration file                                  | n/a          | `config=filename.json` | n/a                                    |
| `excludes`     | Regular expression patterns for exclusion                | `^$`         | `excludes=com.+,org.+` | `{ "excludes": [ "com.+", "org.+" ] }` |
| `includes`     | Regular expression patterns for exclusion                | `.+`         | `includes=de.+,ch.+`   | `{ "includes": [ "de.+", "ch.+" ] }`   |
| `details`      | Include method details                                   | `true`       | `details=false`        | `{ "details": false }`                 |
| `out`          | Report output filename                                   | `System.err` | `out=report.json`      | `{ "out": "report.json" }`             |
| `reportIssues` | Output instrumentation issues to `System.err` at VM exit | `true`       | `reportIssues=false`   | `{ "reportIssues": false }`            |
| `reporter`     | Name of [reporter](#reporters) to use; custom via Java service loader  | `identity`   | `reporter=custom`      | `{ "reporter": "custom" }`             |
| `meta`         | Generic key/value meta data for the reporter             | `{}`         | `meta=filename.json`   | `{ "meta": "filename.json" }`          |

Additional information:

- package names are separated by `/`, for instance `de/engehausen/inspector`.
- an example configuration file is located at [`src/test/resources/agent-config.json`](./src/test/resources/agent-config.json).
- a class is not instrumented when it is excluded or not included.

There is an example Spring application in [`demo-app`](./demo-app). It can be used to experiment with the agent:

    cd demo-app
    mvn package
    cd target
    java -javaagent:../../target/usage-inspector-0.1.0-SNAPSHOT.jar=out=report.json:details=true -jar demo-0.0.1-SNAPSHOT.jar

The report will be available in the `report.json` file afterwards.

## Reporters

The default reporter will output a report of the following format (example, with `details=true`):

```json
{
  "classes": {
    "de/engehausen/example/ApplicationDemo": {
      "totalCalls": 10,
      "methodCalls": {
        "once()Z": 1,
        "twice()I": 2,
        "factorial(I)I": 5,
        "performRecursion()V": 1,
        "performCalls()V": 1
      }
    }
  }
}
```

Note that the agent reports the classes with packages with a `/` separator.

As an "advanced" feature, it is possible to shape the standard report into something else.
For this the [`de.engehausen.inspector.data.Reporter<T>`](src/main/java/de/engehausen/inspector/data/Reporter.java) interface can be used.
A Java service loader can load custom reporters.
The following default reporters exist:

| Name          | Functionality                                                                                                                         |
|---------------|---------------------------------------------------------------------------------------------------------------------------------------|
| `identity`    | Reports as described above. This is the default.                                                                                      |
| `correlator`  | [Correlates](src/main/java/de/engehausen/inspector/reporters/FileCorrelator.java) the classes with source code. Same format as above. |
| `percentile`  | Outputs a **list** of source files with percentile weights (0..1).                                                                    |
| `quantized`   | Outputs a **list** of source files with quantized weights (0..1 in "quantized" steps).                                                |
| `threshold`   | Outputs a **list** of source files with weights mapped to either 0 or 1 depending on the limit (0..1) of each files' percentile.      |

Example: Using the `percentile` reporter and additional [input configuration](src/test/resources/agent-config-correlator.json) (example)
for `sourceRoots` and `extensions`, a result might look like this:

```json
[
  {
    "name": "src/test/java/de/engehausen/inspector/reporters/ThresholdTest.java",
    "weight": 0.3333
  },
  {
    "name": "src/test/java/de/engehausen/inspector/reporters/PercentileTest.java",
    "weight": 0.6667
  },
  {
    "name": "src/test/java/de/engehausen/inspector/reporters/FileCorrelatorTest.java",
    "weight": 1.0
  }
]
```
