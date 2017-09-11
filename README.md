# JWIG
Worst-case Input Generation (WIG) for Java  -- for lack of a clever name.


## Building 

To build, you need `ant`, `gradle`, `mvn` installed and on your path, along with the standard JDK (`javac` etc). 


```
$ git clone https://github.com/rohanpadhye/jwig
$ cd jwig
$ ./scripts/setup.sh 
$ ant  # should build the jwig classes
```

## Running

### Instrumenting test input execution

```
$ ./scripts/datatraces.sh MAIN_CLASS_NAME [PROGRAM_ARGS...]
```

This will run the Java main class `MAIN_CLASS_NAME` with any provided command-line arguments, by instrumenting classes on-the-fly.

The instrumented classes will produce a trace of calls, returns, branches and heap memory accesses. There is one trace file 
created per executed thread called `<THREAD_NAME>.log`. For a single-threaded program, this usually means `main.log`.

### Analysis of traces

The analysis scripts use Python. Please do yourself a favor and install `pypy` if you haven't already done so. You're welcome.


```
$ ./scripts/redundancy_analysis.py [--input TRACE_FILE] [--serialize PKL_FILE]
```

This will run an analysis on the log file `TRACE_FILE` (by default it is `main.log` if not provided) inspired 
by [Travioli](https://github.com/rohanpadhye/travioli) and print information about redundancy at various Acyclic Execution
Contexts (AECs) on standard output in human-readable form. The same information can also be pickled onto a machine-readable file
if `--serialize` is provided.

```
$ ./scripts/diff_cycles.py [PKL_FILE_1] [PKL_FILE_2]
```

This will compare the redundancy information in two pickle files serialized by `redundancy_analysis.py` and output 
the difference between the redundancies on standard output. As of this writing, the differences are only human-readable 
and for informational purposes only. Future work includes using this information to guide fuzz testing or symbolic execution 
or reinforcement learning in order to generate inputs with high redundancy scores leading to worst-case behavior.

## Full example

Here's an example of testing [insertion sort](https://en.wikipedia.org/wiki/Insertion_sort) of 5 numbers in best-case, average-case and worst-case.

```
$ seq 1 5 > best.txt           # Already sorted
$ seq 1 5 | sort -R > avg.txt  # Randomly shuffled
$ seq 5 -1 1 > worst.txt          # Reverse sorted

$ ./scripts/datatraces.sh benchmarks.wise.InsertionSort < best.txt
$ ./scripts/redundancy_analysis.py --serialize best.pkl

$ ./scripts/datatraces.sh benchmarks.wise.InsertionSort < avg.txt
$ ./scripts/redundancy_analysis.py --serialize avg.pkl

$ ./scripts/datatraces.sh benchmarks.wise.InsertionSort < worst.txt
$ ./scripts/redundancy_analysis.py --serialize worst.pkl

$ ./scripts/diff_cycles.py best.pkl avg.pkl       # Compare best vs avg
$ ./scripts/diff_cycles.py avg.pkl  worst.pkl     # Compare avg  vs worst
$ ./scripts/diff_cycles.py best.pkl worst.pkl     # Compare best vs worst
```

## Fuzz a junit-quickcheck property

Here's an example of using junit-quickcheck with a guided random file for property testing:

```
./scripts/fuzz.sh benchmarks.BinaryTreeTest contains
```
