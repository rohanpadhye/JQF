# JQF: junit-quickcheck-fuzz

JQF is a feedback-directed extension of [junit-quickcheck](https://github.com/pholser/junit-quickcheck), that enables coverage-guided fuzzing using tools like [AFL](lcamtuf.coredump.cx/afl).

## Building 

To build, you need Maven (`mvn`) and GNU Make (`make`) installed and on your path. 

You also need to set the environment variable `AFL_DIR` to point to where [AFL](http://lcamtuf.coredump.cx/afl) is installed if you want to run AFL-guided fuzzing.

This document uses `bash` syntax to demonstrate command-line usage.

```bash
git clone https://github.com/rohanpadhye/jqf
cd jqf
export AFL_DIR=/path/to/afl
./scripts/setup.sh 
```

## Usage

### Writing a Test

*TODO*

### Fuzzing with AFL

*This section assumes you already familiar with fuzzing C programs with AFL.* If not, take a look at the [AFL quick start guide](http://lcamtuf.coredump.cx/afl/QuickStartGuide.txt) to learn more about the command-line usage of the `afl-fuzz` program.

JQF provides a handy script that serves as a target for AFL to run. The script in turn launches a JVM and runs your test method with randomly-generated input, using branch coverage as a feedback to AFL.

Since we want AFL to fuzz what is essentially a shell script, we need to use an environment variable to tell it to not validate that the target is a native binary.

```bash
export AFL_SKIP_BIN_CHECK=1
```

Then, run AFL as usual with the target program as `jqf-afl` whose command-line arguments are the Java class and method you want to fuzz, along with the *input file*. The classpath for your Java application may be provided using the `CLASSPATH` environment variable, which is assumed to be `.` by default.

```bash
$AFL_DIR/afl-fuzz -i seeds -o results $JQF_DIR/bin/jqf-afl TEST_CLASS TEST_METHOD @@
```

For example, to run one of the provided benchmarks:

```bash
export CLASSPATH=$JQF_DIR/examples/target/test-classes
$AFL_DIR/afl-fuzz -i $JQF_DIR/examples/seeds/zeros -o results $JQF_DIR/bin/jqf-afl edu.berkeley.cs.jqf.examples.jdk.SortTest dualPivotQuicksort @@
```

### Testing crashes (and other inputs)

If AFL finds a *crash* (due to a test failure such as an `AssertionError`), you can run the `jqf-repro` script with a specific input file. The script will in turn re-run the Java test, and will dump the exception stack-trace on the standard error stream. The `CLASSPATH` environment variable is honored as above.

```bash
$JQF_DIR/bin/jqf-repro TEST_CLASS TEST_METHOD INPUT_FILE`
```

For example,
```bash
export CLASSPATH=$JQF_DIR/examples/target/test-classes
$JQF_DIR/bin/jqf-repro edu.berkeley.cs.jqf.examples.jdk.SortTest dualPivotQuicksort results/crashes/id_000001
```

## Extending

You can also extend this framework for use with fuzzing front-ends other than AFL, and feedback other than branch coverage. All you need to do is implement the [`Guidance`](https://github.com/rohanpadhye/jqf/blob/master/fuzz/src/main/java/edu/berkeley/cs/jqf/fuzz/guidance/Guidance.java) interface and have your own script to launch an appropriate driver. It should be easy to extrapolate from existing implementations such as:

- [`jqf-afl`](https://github.com/rohanpadhye/jqf/blob/master/bin/jqf-afl) using [`AFLGuidance`](https://github.com/rohanpadhye/jqf/blob/master/fuzz/src/main/java/edu/berkeley/cs/jqf/fuzz/guidance/AFLGuidance.java)
- [`jqf-repro`](https://github.com/rohanpadhye/jqf/blob/master/bin/jqf-random) using [`ReproGuidance`](https://github.com/rohanpadhye/jqf/blob/master/fuzz/src/main/java/edu/berkeley/cs/jqf/fuzz/guidance/ReproGuidance.java)
- [`jqf-random`](https://github.com/rohanpadhye/jqf/blob/master/bin/jqf-repro) using [`NoGuidance`](https://github.com/rohanpadhye/jqf/blob/master/fuzz/src/main/java/edu/berkeley/cs/jqf/fuzz/guidance/NoGuidance.java)




