# JQF

JQF is a feedback-directed extension of [`junit-quickcheck`](https://github.com/pholser/junit-quickcheck), that enables coverage-guided fuzzing using tools like [AFL](lcamtuf.coredump.cx/afl).

## Building 

To build, you need Maven (`mvn`) installed and on your path, along with the standard JDK (`javac` etc). 

You also need to set the environment variable `AFL_DIR` to point to where AFL is installed if you want
to run AFL-guided fuzzing.


```
$ git clone https://github.com/rohanpadhye/jqf
$ cd jqf
$ export AFL_DIR=/path/to/afl
$ ./scripts/setup.sh 
```

## Usage

### Writing a Test

*TODO*

### Fuzzing 

JQF provides a handy script that serves as a target for AFL to run. The script in turn launches a JVM and runs your test method with randomly-generated input, using branch coverage as a feedback to AFL.

Since we want AFL to fuzz what is essentially a shell script, we need to use an environment variable to tell it to not validate that the target is a native binary.

```
$ export AFL_SKIP_BIN_CHECK=1
```

Then, run AFL as usual with the target program as `jqf-afl` whose command-line arguments are the Java class and method you want to fuzz, along with the *input file*.

```
$ /path/to/afl/afl-fuzz -i seeds -o results /path/to/jqf/jqf-afl [-cp CLASSPATH] TEST_CLASS TEST_METHOD @@
```

For example:
```
$ /path/to/afl/afl-fuzz -i seeds -o results /path/to/jqf/jqf-afl -cp /path/to/jqf/benchmarks edu.cs.berkeley.jqf.benchmarks.SortTest timSort @@
```

### Testing crashes (and other inputs)

If AFL finds a *crash* (due to a test failure such as an `AssertionError`), you can run the `jqf-repro` script with a specific input file. The script will in turn re-run the Java test, but without all the instrumentation overhead and it will dump the exception stack-trace on the standard error stream.

```
$ /path/to/jqf/jqf-repro [-cp CLASSPATH] TEST_CLASS TEST_METHOD INPUT_FILE
```

For example,
```
 /path/to/jqf/jqf-repro -cp /path/to/jqf/benchmarks edu.cs.berkeley.jqf.benchmarks.SortTest timSort results/crashes/id_000001
```

## Extending

You can also extend this framework for using fuzzing front-ends other than AFL, and feedback other than branch coverage. All you need to do is implement the `Guidance` interface and have your own script to launch the `JunitTestDriver`. It should be easy to extrapolate from existing implementations such as:

- `jqf-afl` using `AFLGuidance`
- `jqf-repro` using `ReproGuidance`
- `jqf-random` using `NoGuidance`




