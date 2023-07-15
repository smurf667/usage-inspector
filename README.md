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
| `reporter`     | Name of reporter to use; custom via Java service loader  | `identity`   | `reporter=custom`      | `{ "reporter": "custom" }`             |
| `meta`         | Generic key/value meta data for reporter                 | `{}`         | `meta=filename.json`   | `{ "meta": "filename.json" }`          |

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
