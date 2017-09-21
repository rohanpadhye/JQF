# JQF: junit-quickcheck-fuzz

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

Then, run AFL as usual with the target program as `jqf-afl` whose command-line arguments are the Java class and method you want to fuzz, along with the *input file*. The classpath for your Java application may be provided using the `CLASSPATH` environment variable, which is assumed to be `.` by default.

```
$ /path/to/afl/afl-fuzz -i seeds -o results /path/to/jqf/jqf-afl TEST_CLASS TEST_METHOD @@
```

For example, to run one of the provided benchmarks:

```
$ export CLASSPATH=/path/to/jqf/fuzz/target/test-classes 
$ /path/to/afl/afl-fuzz -i seeds -o results /path/to/jqf/jqf-afl benchmarks.SortTest dualPivotQuicksort @@
```

### Testing crashes (and other inputs)

If AFL finds a *crash* (due to a test failure such as an `AssertionError`), you can run the `jqf-repro` script with a specific input file. The script will in turn re-run the Java test, and will dump the exception stack-trace on the standard error stream. The `CLASSPATH` environment variable is honored as above.

```
$ /path/to/jqf/jqf-repro TEST_CLASS TEST_METHOD INPUT_FILE
```

For example,
```
 /path/to/jqf/jqf-repro benchmarks.SortTest dualPivotQuicksort results/crashes/id_000001
```

## Extending

You can also extend this framework for use with fuzzing front-ends other than AFL, and feedback other than branch coverage. All you need to do is implement the `Guidance` interface and have your own script to launch the `JunitTestDriver`. It should be easy to extrapolate from existing implementations such as:

- `jqf-afl` using `AFLDriver` and `AFLGuidance`
- `jqf-repro` using `ReproDriver` and `ReproGuidance`
- `jqf-random` using `RandomDriver` and `NoGuidance`




