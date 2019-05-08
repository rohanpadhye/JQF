# Semantic Fuzzing with Zest: Artifact Evaluation

This document has the following sections:
- **Getting started** (~10 human-minutes): Make sure that the provided Docker container runs on your system.
- **Part one: Running fresh experiments** (~10 human-minutes + ~30 compute-minutes): Run a short version of the experiments to quickly get a fresh-baked subset of the evaluation results. *Optional*: Add ~45 compute-hours for a better quality approximation. The full evaluation takes 900 compute-hours.
- **Part two: Validating claims in the paper** (~30 human-minutes + ~30 compute-minutes): Analyze pre-baked results of the full experiments, which were generated on the authors' machine in 900 compute-hours. Run scripts to produce the figures used in the paper. You can also use these instructions on the results from part one to produce figures for your own fresh-baked experiments, which should approximate the figures in the paper. 
- **Part three: Reuse beyond paper** (~20 human-minutes): Run Zest on a small standalone program, which serves as a Hello-World example for re-use in custom test targets.


## Getting-started

### Requirements

* You will need **Docker** on your system. You can get Docker CE for Ubuntu here: https://docs.docker.com/install/linux/docker-ce/ubuntu. See links on the sidebar for installation on other platforms.

### Load image

To load the artifact on your system, you have two options. 

**Option 1**: Download the following file and load it: https://drive.google.com/file/d/1WlpWeEXtkVTgzubVBYjejmjsMIa9yTei/view

```
docker load -i zest-artifact.tar.gz
```

**Option 2**: Pull the image from the public repo. This might be useful in case we have to make updates requested by the AEC.
```
docker pull rohanpadhye/zest-artifact
```

### Run container

Run the following to start a container and get a shell in your terminal:

```
docker run --name zest -it rohanpadhye/zest-artifact
```

The remaining sections of this document assume that you are inside the container's shell, within the default directory `/zest-artifact`. You can exit the shell via CTRL+C or CTRL+D or typing `exit`. This will kill running processes, if any, but will preserve changed files. You can re-start an exited container with `docker start -i zest`. Finally, you can clean up with `docker rm zest`.

### Container filesystem

The default directory in the container, `/zest-artifact`, contains the following contents:
- `README.txt`: This file.
- `afl`: This is AFL v2.52b, cloned from https://github.com/mcarpenter/afl.
- `jqf`: This is the Java fuzzing platform, cloned from https://github.com/rohanpadhye/jqf. Zest is implemented as a plug-in "guidance" within JQF.
- `scripts`: Contains various scripts used for running experiments and generating figures from the paper.
- `pre-baked`: Contains results of the experiments that were run on the authors' machine, which took 900 compute-hours to generate. 
- `fresh-baked`: This will contain the results of the experiments that you run, after following Part One (next section).
- `reuse`: Contains a "Hello World"-like test driver, for demonstrating how Zest can be used  to fuzz a standalone project.

## Part One: Running fresh experiments

The main evaluation of this paper involves experiments with **3 fuzzing techniques** on **5 benchmark programs** (i.e., 15 configurations). The experiments can be launched via `scripts/run_all.sh`, whose usage is as follows:

```
./scripts/run_all.sh RESULTS_DIR TIME REPS
```

Where `RESULTS_DIR` is the name of the directory where results will be saved, `TIME` is the duration of each fuzzing experiment (e.g. `30s` or `10m` or `3h`), and `REPS` is the number of repetitions to perform. 

Since we have 3 techniques and 5 benchmarks, the total running time of this script is `15 x TIME x REPS`. For the experiments in the paper, we ran with TIME=`3h` and REPS=`20`, which takes **900 compute-hours** (~1.2 months). However, we can get approximate results by running for a shorter duration and with fewer repetitions. For example, the following command will take `15 x 1m x 2` = **30 compute-minutes** to run two repetitions of 1-minute fuzzing sessions across all techniques and benchmarks:

```
./scripts/run_all.sh fresh-baked 1m 2     # Takes 30 minutes to complete
```

The above command will save results in a directory named `fresh-baked`. Feel free to tweak options if you have more time on your hands. We've found that running experiments for `1h` and `3` reps (total **45 compute-hours**) can give reasonably interesting results that approximate the evaluation in the paper. The trade-off is that shorter durations will trim the X-axis of the plots in Figure 4, and fewer repetitions will decrease the statistical confidence (e.g. expect larger shaded regions in Fig. 4).

In either case, running the above script produces `15 x REPS` sub-directories in `fresh-baked`, with the naming convention `$BENCH-$TECHNIQUE-results-$ID`, where:
- `BENCH` is one of `ant`, `maven`, `closure`, `rhino`, or `bcel`. 
- `TECHNIQUE` is one of `zest`, `afl`, or `rnd` (the last one is pure random = "QuickCheck" in the paper)
- `ID` is a number between 1 and `REPS`, inclusive.

For example, the directory `fresh-baked/ant-zest-results-1` contains the results for the first run of fuzzing Apache Ant with Zest.

The pre-populated directory `pre-baked` is similar to `fresh-baked` but contains the results of our 900-hour long experiments (20 reps of 3 hours each). The next section describes how to generate the plots in Figure 4 and the bug data in Table 2 using `pre-baked`. However, you can run the exact same scripts with `fresh-baked` to get plots and bug data for the experiments that you just ran using the commands above.

## Part Two: Validating claims in paper

This section explains how to analyze the results in `pre-baked`, which have been provided with the artifact, to produce Figure 4 and Table 2 in the paper. You can follow the same steps with the results of your own `fresh-baked` experiments from part one, as well.

### Reproducing Figure 4

In the paper, Figure 4 plots code coverage in the semantic analysis classes (ref. Table 1) for each of the five benchmarks. Generating these plots from a results directory (e.g. `fresh-baked` or `pre-baked`) is in general a two-step process. The first step has already been performed for `pre-baked`, but you are welcome to re-run it if you have compute time available (approx. 24 hours).

1. Collect semantic code coverage from all saved inputs, using JaCoCo **(can be skipped for pre-baked)**: 
```
./scripts/compute_semantic_coverage.sh pre-baked 20  # Takes a long time (> 24 compute-hours)
```

The above command analyzes inputs in all sub-directories of `pre-baked` up to `*-results-20`. The arguments to the command are the `RESULTS_DIR` and the number of `REPS`. If you are running this on `fresh-baked` results, make sure to provide the same `REPS` as you did in part one. Note that this will complete much faster for a smaller number of `REPS` (approx. one compute-hour per repetition). When this command completes, each sub-directory in the results directory (e.g. `pre-baked/ant-zest-1-results`) will have a file called `semantic-coverage-percent.ssv`. **Note that these files are already provided for results inside `pre-baked`**, though you are welcome to re-run the above script if you have compute time available.

2. Generate plots for Fig. 4: (this step depends on the SSV files from the previous step being available)
```
./scripts/generate_figure_4.py pre-baked 20     # Takes about 5 compute-minutes
```

Like before, this command takes as arguments the `RESULT_DIR` and the number of `REPS`. The above command creates plots in a sub-directory called `figures` inside the  `pre-baked` results directory.  You can do the same with your `fresh-baked` results to see coverage plots for the experiments that you ran in part one; just remember to provide the correct number of `REPS`.

Once you run the above command, do `ls pre-baked/figures` to list the generated PDFs for the `pre-baked` results. You can copy the PDF files from the docker container to your host machine to open them in a PDF viewer. Assuming you started the container with `docker run --name zest ...`, you can run the following command on your host: 

```
docker cp zest:zest-artifact/pre-baked/figures/Figure_4a_maven.pdf .
```

### Reproducing Table 2


To analyze failing test cases in a results directory, and to compute their Mean Time-to-Find (MTF) and Repeatability, run:

```
./scripts/generate_table_2.sh pre-baked 20    # Takes about 15 compute-minutes
```

Like before, the arguments to the above script are the `RESULTS_DIR` and the number of `REPS`. You can run this script on your own `fresh-baked` results, as long as you provide the correct number of `REPS`. 

The above commmand generates a CSV called `Table_2.csv` inside `pre-baked/figures`, next to the plots. In Table 2, each bug is identified by a pair of (benchmark, exception). The CSV has one row for each triple of (benchmark, exception, tool), assuming that `tool` found the bug at least once. The last two columns list the Mean Time-to-Find (MTF) in seconds, and the repeatibility.

Note that the CSV contains some bugs that are not listed in Table 2. We manually classified as syntactic vs. semantic when reporting them to the developers (depending on which class the bug arises in w.r.t. Table 1). Table 2 only lists the semantic bugs. All listings from Table 2 should be in the CSV.


### Discussion of Bugs in Section 5.2

In Section 5.2, we claim that our experiments resulted in over 95,000 failing inputs. You can verify this by summing the results of the following commands:
```
ls pre-baked/*-results-*/failures/ | wc -l
ls pre-baked/*-results-*/crashes/  | wc -l
```

In Section 5.2, under "Case Studies", we describe one bug in Ant, one in Rhino, and three in Closure, all of which have been acknowledged. Here are their URLs:

- `ant` [B] (**fixed**): https://bz.apache.org/bugzilla/show_bug.cgi?id=62655
- `rhino` [J]: https://github.com/mozilla/rhino/issues/407
- `closure` [C] (**fixed**): https://github.com/google/closure-compiler/issues/2843
- `closure` [D]: https://github.com/google/closure-compiler/issues/2842
- `closure` [U]: https://github.com/google/closure-compiler/issues/3220

In Section 5.2, we claim that "3 bugs have been fixed". Apart from the two fixed bugs listed above, see also:
- `maven/IllegalArgumentException` (**fixed**): https://issues.apache.org/jira/browse/MNG-6577. This is a syntax parsing bug, and is hence not listed in Table 2.


## Part Three: Reuse beyond paper

In order to demonstrate how Zest can be used beyond replicating the experiments in the paper, we have provided a "Hello World"-like application that can be fuzzed using Zest to find a real bug in Apache Commons.

Go to the `reuse` directory:
```
cd reuse
```

This directory is a Maven project containing just two files: 
1. `pom.xml`: specifies the project's dependency on Apache Commons Collections, which is our test target, and on JQF, which is the Java fuzzing platform on which Zest is built.
2. `src/test/java/PatriciaTrieTest.java`: A simple test driver that checks a basic property of Trie data structures constructed from an input HashMap. The goal is to find an input Map that violates the assert, using the default generator for HashMaps (as shipped with junit-quickcheck). 

Build this test with:
```
mvn test-compile
```

To fuzz using Zest, run:
```
mvn jqf:fuzz -Dclass=PatriciaTrieTest -Dmethod=testMap2Trie -Dout=results
```

You will see a status screen with the progress of fuzzing. Most likely, you will see "Unique Failures: 1" after a few seconds or minutes of fuzzing. The test failure is written to the directory `target/results`, which has a similar format to the results directories produced in Part One above. You can stop fuzzing with CTRL+C.

If your fuzzing run found at least one failure, then you can replay the failing test case, say `id_000001`, like so:
```
mvn jqf:repro -Dclass=PatriciaTrieTest -Dmethod=testMap2Trie -Dinput=target/results/failures/id_000000
```

The above command should result in a stack-trace showing the Assertion Violation. To find the actual source of the bug, one should add appropriate print statements or perform step-through debugging, whose description is not in the scope of this document. The test described here helps find the bug https://issues.apache.org/jira/browse/COLLECTIONS-714.

Note that this bug cannot be easily found using conventional QuickCheck-like random testing. To verifiy this, run the `mvn jqf:fuzz` command as above but with the additional `-Dblind` option to indicate that all inputs should be randomly generated from scratch:

```
mvn jqf:fuzz -Dclass=PatriciaTrieTest -Dmethod=testMap2Trie -Dout=results -Dblind
```

The above command usually results in very low coverage and no test failures even after several minutes of fuzzing. Press CTRL+C to exit. This exercise shows the benefit of using the Zest algorithm and also the ease of using Zest via the JQF Maven Plugin.

For further reading, we've added tutorials to the JQF wiki for tips on writing generators and fuzzing with Zest:
- https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-Zest 
- https://github.com/rohanpadhye/jqf/wiki/Fuzzing-a-Compiler
