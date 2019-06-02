# JQF: Coverage-guided structured fuzzing for Java

JQF is a feedback-directed fuzz testing platform for Java. JQF is built on top of [junit-quickcheck](https://github.com/pholser/junit-quickcheck), which itself lets you write [Quickcheck](http://www.cse.chalmers.se/~rjmh/QuickCheck/manual.html)-like **generators** and properties in a [Junit](http://junit.org)-style test class. JQF enables better input generation using **coverage-guided** fuzzing algorithms.

JQF has been successful in [discovering a number of bugs in widely used open-source software](https://github.com/rohanpadhye/jqf/wiki/Bug-trophy-case) such as OpenJDK, Apache Maven and the Google Closure Compiler.

JQF is a modular framework, supporting the following pluggable fuzzing front-ends called *guidances*:
* Binary fuzzing with [AFL](http://lcamtuf.coredump.cx/afl) ([tutorial](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-AFL))
* Semantic fuzzing with **[Zest](https://cs.berkeley.edu/~rohanpadhye/files/zest-issta19.pdf) [ISSTA'19]** ([tutorial 1](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-Zest)) ([tutorial 2](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-a-Compiler))
* Complexity fuzzing with **[PerfFuzz](https://people.eecs.berkeley.edu/~rohanpadhye/files/perffuzz-issta18.pdf) [ISSTA'18]**

Read more about JQF in the [ISSTA'19 tool paper](https://people.eecs.berkeley.edu/~rohanpadhye/files/jqf-issta19.pdf).

## Overview

### What is *structured fuzzing*?

Binary fuzzing tools like [AFL](http://lcamtuf.coredump.cx/afl) and [libFuzzer](https://llvm.org/docs/LibFuzzer.html) treat the input as a sequence of bytes. If the test program expects highly structured inputs, such as XML documents or JavaScript programs, then mutating byte-arrays often results in syntactically invalid inputs; the core of the test program remains untested.

**Structured fuzzing** tools leverage domain-specific knowledge of the input format to produce inputs that are *syntactically valid* by construction. Here is a nice article on [structure-aware fuzzing of C++ programs using libFuzzer](https://github.com/google/fuzzer-test-suite/blob/master/tutorial/structure-aware-fuzzing.md).

### What is *generator-based* fuzzing (QuickCheck)?

Structured fuzzing tools need a way to understand the input structure. Some other tools use declarative specifications of the input format such as [context-free grammars](https://embed.cs.utah.edu/csmith/) or [protocol buffers](https://github.com/google/libprotobuf-mutator). **JQF** uses QuickCheck's imperative approach for specifying the space of inputs: arbitrary ***generator*** programs whose job is to generate a single random input. 

A `Generator<T>` provides a method for producing random instances of type `T`. For example, a generator for type `Calendar` returns randomly-generated `Calendar` objects. One can easily write generators for more complex types, such as [XML documents](https://github.com/rohanpadhye/jqf/blob/master/examples/src/main/java/edu/berkeley/cs/jqf/examples/xml/XmlDocumentGenerator.java), [JavaScript programs](https://github.com/rohanpadhye/jqf/blob/master/examples/src/main/java/edu/berkeley/cs/jqf/examples/js/JavaScriptCodeGenerator.java), [JVM class files](https://github.com/rohanpadhye/jqf/blob/master/examples/src/main/java/edu/berkeley/cs/jqf/examples/bcel/JavaClassGenerator.java), SQL queries, HTTP requests, and [many more](https://github.com/pholser/junit-quickcheck/tree/master/examples/src/test/java/com/pholser/junit/quickcheck/examples) -- this is **generator-based fuzzing**. However, simply sampling random inputs of type `T` is not usually very effective, since the generator does not know if the inputs that it produces are any good.


### What is *semantic fuzzing* (Zest)?

JQF supports the **[*Zest algorithm*](https://cs.berkeley.edu/~rohanpadhye/files/zest-issta19.pdf), which uses code-coverage and input-validity feedback to bias a QuickCheck-style generator** towards generating structured inputs that can reveal deep semantic bugs. JQF extracts code coverage using bytecode instrumentation, and input validity using JUnit's [`Assume`](https://junit.org/junit4/javadoc/4.12/org/junit/Assume.html) API. An input is valid if no assumptions are violated.


## Documentation

### Tutorials

* [Zest 101](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-Zest): A basic tutorial for fuzzing a standalone toy program using command-line scripts. Walks through the process of writing a test driver and structured input generator for `Calendar` objects.
* [Fuzzing a compiler with Zest](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-a-Compiler): A tutorial for fuzzing a non-trivial program -- the [Google Closure Compiler](https://github.com/google/closure-compiler) -- using a generator for JavaScript programs. This tutorial makes use of the [JQF Maven plugin](https://github.com/rohanpadhye/jqf/wiki/JQF-Maven-Plugin).
* [Fuzzing with AFL](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-AFL): A tutorial for fuzzing a Java program that parses binary data, such as PNG image files, using the AFL binary fuzzing engine.

### Additional Details

The [JQF wiki](https://github.com/rohanpadhye/jqf/wiki) contains lots more documentation including:
- [Using a custom fuzz guidance](https://github.com/rohanpadhye/jqf/wiki/The-Guidance-interface)
- [Performance Benchmarks](https://github.com/rohanpadhye/jqf/wiki/Performance-benchmarks)
- [Bug trophy case](https://github.com/rohanpadhye/jqf/wiki/Bug-trophy-case)

JQF also publishes its [API docs](https://rohanpadhye.github.io/jqf/apidocs).

## Contact the developers

We want your feedback! (haha, get it? get it?) 

If you've found a bug in JQF or are having trouble getting JQF to work, please open an issue on the [issue tracker](https://github.com/rohanpadhye/jqf/issues). You can also use this platform to post feature requests.

If it's some sort of fuzzing emergency you can always send an email to the main developer: [Rohan Padhye](https://people.eecs.berkeley.edu/~rohanpadhye).
