# JQF: Generator-based Structured Fuzzing for Java 

JQF is built on top of [junit-quickcheck](https://github.com/pholser/junit-quickcheck), which itself lets you write [Quickcheck](http://www.cse.chalmers.se/~rjmh/QuickCheck/manual.html)-like **generators** and properties in a [Junit](http://junit.org)-style test class. JQF enables better input generation using **coverage-guided** fuzzing tools such as [AFL](http://lcamtuf.coredump.cx/afl). 

JQF has been successful in [discovering a number of bugs in widely used open-source software](https://github.com/rohanpadhye/jqf/wiki/Bug-trophy-case) such as OpenJDK, Apache Maven and the Google Closure Compiler.

## What is *structured fuzzing*?

Conventional fuzzing tools like [AFL](http://lcamtuf.coredump.cx/afl) and [libFuzzer](https://llvm.org/docs/LibFuzzer.html) treat the input as a sequence of bytes. If the test program expects highly structured inputs, such as XML documents or JavaScript programs, then mutating byte-arrays often results in syntactically invalid inputs; the core of the test program remains untested.

**Structured fuzzing** tools like **JQF** and others perform mutations in the space of *syntactically valid* inputs, by leveraging domain-specific knowledge of the input format. Here is a nice article on [structure-aware fuzzing of C++ programs using libFuzzer](https://github.com/google/fuzzer-test-suite/blob/master/tutorial/structure-aware-fuzzing.md).

### What is *generator-based* structured fuzzing?

Structured fuzzing tools need a way to understand the input format. Some tools use [grammars](https://embed.cs.utah.edu/csmith/) or [protocol buffers](https://github.com/google/libprotobuf-mutator) as a declarative specification of input format. **JQF** uses an imperative approach for specifying the space of inputs: arbitrary ***generator*** programs whose job is to generate a single random input. 

A `Generator<T>` provides an API for producing random instances of type `T`. For example, a generator for type `Calendar` returns randomly-generated `Calendar` objects. One can easily write generators for more complex types, such as [XML documents](https://github.com/rohanpadhye/jqf/blob/master/examples/src/main/java/edu/berkeley/cs/jqf/examples/xml/XmlDocumentGenerator.java), [JavaScript programs](https://github.com/rohanpadhye/jqf/blob/master/examples/src/main/java/edu/berkeley/cs/jqf/examples/js/JavaScriptCodeGenerator.java), [JVM class files](https://github.com/rohanpadhye/jqf/blob/master/examples/src/main/java/edu/berkeley/cs/jqf/examples/bcel/JavaClassGenerator.java), SQL queries, HTTP requests, and many more -- this is **generator-based structured fuzzing**. However, simply sampling random inputs of type `T` is not usually very effective, since the generator does not know if the inputs it is producing are any good.

**JQF uses code-coverage feedback to bias the pseudo-random source that backs your generator**, thereby encouraging the production of inputs that are both syntactically valid and find bugs deep in your test program. Once you have a generator for type `T`, you can fuzz any method that takes an instance of type `T` in its argument list. JQF automatically converts any QuickCheck-style random-input generator of type `T` into a feedback-directed fuzzer on the domain of `T`.

## What is *semantic fuzzing*?

JQF supports the [Zest algorithm](https://arxiv.org/abs/1812.00078), which specializes in generating inputs that are *semantically valid* (i.e., structured inputs that satisfy specific invariants). 

### Tutorials

* [Zest 101](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-Zest): A basic tutorial for fuzzing a standalone toy program using command-line scripts. Walks through the process of writing a test driver and structured input generator for `Calendar` objects.
* [Fuzzing a compiler with Zest](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-a-Compiler): A tutorial for fuzzing a non-trivial program -- the [Google Closure Compiler](https://github.com/google/closure-compiler) -- using a generator for JavaScript programs. This tutorial makes use of the [JQF Maven plugin](https://github.com/rohanpadhye/jqf/wiki/JQF-Maven-Plugin).

## Documentation

The [JQF wiki](https://github.com/rohanpadhye/jqf/wiki) contains lots more documentation including:
- [Writing a JQF test](https://github.com/rohanpadhye/jqf/wiki/Writing-a-JQF-test)
- [Fuzzing with AFL](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-AFL)
- [Using a custom fuzz guidance](https://github.com/rohanpadhye/jqf/wiki/The-Guidance-interface)
- [Performance Benchmarks](https://github.com/rohanpadhye/jqf/wiki/Performance-benchmarks)

JQF also publishes its [API docs](https://rohanpadhye.github.io/jqf/apidocs).

## Contact the developers

We want your feedback! (haha, get it? get it?) 

If you've found a bug in JQF or are having trouble getting JQF to work, please open an issue on the [issue tracker](https://github.com/rohanpadhye/jqf/issues). You can also use this platform to post feature requests.

If it's some sort of fuzzing emergency you can always send an email to the main developer: [Rohan Padhye](https://people.eecs.berkeley.edu/~rohanpadhye).
