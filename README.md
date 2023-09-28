# JQF + Zest: Semantic Fuzzing for Java
[![Build](https://github.com/rohanpadhye/JQF/actions/workflows/ci.yml/badge.svg)](https://github.com/rohanpadhye/JQF/actions/workflows/ci.yml)

[ISSTA'19 paper]: https://rohan.padhye.org/files/zest-issta19.pdf
[ISSTA'18 paper]: https://rohan.padhye.org/files/perffuzz-issta18.pdf
[ISSTA'19 tool paper]: https://rohan.padhye.org/files/jqf-issta19.pdf
[ICSE'20 paper]: https://rohan.padhye.org/files/rlcheck-icse20.pdf
[ASE'20 paper]: https://rohan.padhye.org/files/bigfuzz-ase20.pdf
[ICSE'21 paper]: https://rohan.padhye.org/files/bonsai-icse21.pdf
[ISSTA'23 paper]: https://dx.doi.org/10.1145/3597926.3598107

JQF is a feedback-directed fuzz testing platform for Java (think: AFL/LibFuzzer but for JVM bytecode). JQF uses the abstraction of *property-based testing*, which makes it nice to write fuzz drivers as parameteric JUnit test methods. JQF is built on top of [junit-quickcheck](https://github.com/pholser/junit-quickcheck). JQF enables running junit-quickcheck style parameterized unit tests with the power of **coverage-guided** fuzzing algorithms such as **Zest**.

[Zest][ISSTA'19 paper] is an algorithm that biases coverage-guided fuzzing towards producing *semantically valid* inputs; that is, inputs that satisfy structural and semantic properties while maximizing code coverage. Zest's goal is to find deep semantic bugs that cannot be found by conventional fuzzing tools, which mostly stress error-handling logic only. By default, JQF runs Zest via the simple command: `mvn jqf:fuzz`.

JQF is a modular framework, supporting the following pluggable fuzzing front-ends called *guidances*:
* Binary fuzzing with [AFL](http://lcamtuf.coredump.cx/afl) ([tutorial](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-AFL))
* Semantic fuzzing with **[Zest](http://arxiv.org/abs/1812.00078)** [[ISSTA'19 paper]] ([tutorial 1](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-Zest)) ([tutorial 2](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-a-Compiler))
* Complexity fuzzing with **[PerfFuzz](https://github.com/carolemieux/perffuzz)** [[ISSTA'18 paper]]
* Reinforcement learning with **[RLCheck](https://github.com/sameerreddy13/rlcheck)** (based on a fork of JQF) [[ICSE'20 paper]]
* Mutation-analysis-guided fuzzing with **[Mu2](https://github.com/cmu-pasta/mu2)** [[ISSTA'23 paper]]

JQF has been successful in [discovering a number of bugs in widely used open-source software](#trophies) such as OpenJDK, Apache Maven and the Google Closure Compiler.

### Zest Research Paper

To reference Zest in your research, we request you to cite our [ISSTA'19 paper]:

> Rohan Padhye, Caroline Lemieux, Koushik Sen, Mike Papadakis, and Yves Le Traon. 2019. **Semantic Fuzzing with Zest**. In Proceedings of the 28th ACM SIGSOFT International Symposium on Software Testing and Analysis (ISSTA’19), July 15–19, 2019, Beijing, China. ACM, New York, NY, USA, 12 pages. https://doi.org/10.1145/3293882.3330576


#### JQF Tool Paper

If you are using the JQF framework to build new fuzzers, we request you to cite our [ISSTA'19 tool paper] as follows:

> Rohan Padhye, Caroline Lemieux, and Koushik Sen. 2019. **JQF: Coverage-Guided Property-Based Testing in Java**. In Proceedings of the 28th ACM SIGSOFT International Symposium on Software Testing and Analysis (ISSTA ’19), July 15–19, 2019, Beijing, China. ACM, New York, NY, USA, 4 pages. https://doi.org/10.1145/3293882.3339002


## Overview

### What is *structure-aware fuzzing*?

Binary fuzzing tools like [AFL](http://lcamtuf.coredump.cx/afl) and [libFuzzer](https://llvm.org/docs/LibFuzzer.html) treat the input as a sequence of bytes. If the test program expects highly structured inputs, such as XML documents or JavaScript programs, then mutating byte-arrays often results in syntactically invalid inputs; the core of the test program remains untested.

**Structure-aware fuzzing** tools leverage domain-specific knowledge of the input format to produce inputs that are *syntactically valid* by construction. There are some nice articles on structure-aware fuzzing of [C++](https://github.com/google/fuzzing/blob/master/docs/structure-aware-fuzzing.md) and [Rust](https://rust-fuzz.github.io/book/cargo-fuzz/structure-aware-fuzzing.html) programs using libFuzzer.

### What is *generator-based* fuzzing (QuickCheck)?

Structure-aware fuzzing tools need a way to understand the input structure. Some other tools use declarative specifications of the input format such as [context-free grammars](https://embed.cs.utah.edu/csmith/) or [protocol buffers](https://github.com/google/libprotobuf-mutator). **JQF** uses QuickCheck's imperative approach for specifying the space of inputs: arbitrary ***generator*** programs whose job is to generate a single random input. 

A `Generator<T>` provides a method for producing random instances of type `T`. For example, a generator for type `Calendar` returns randomly-generated `Calendar` objects. One can easily write generators for more complex types, such as 
[XML documents](examples/src/main/java/edu/berkeley/cs/jqf/examples/xml/XmlDocumentGenerator.java), 
[JavaScript programs](examples/src/main/java/edu/berkeley/cs/jqf/examples/js/JavaScriptCodeGenerator.java), 
[JVM class files](examples/src/main/java/edu/berkeley/cs/jqf/examples/bcel/JavaClassGenerator.java), SQL queries, HTTP requests, and [many more](https://github.com/pholser/junit-quickcheck/tree/master/examples/src/test/java/com/pholser/junit/quickcheck/examples) -- this is **generator-based fuzzing**. However, simply sampling random inputs of type `T` is not usually very effective, since the generator does not know if the inputs that it produces are any good.


### What is *semantic fuzzing* (Zest)?

JQF supports the **[*Zest algorithm*][ISSTA'19 paper], which uses code-coverage and input-validity feedback to bias a QuickCheck-style generator** towards generating structured inputs that can reveal deep semantic bugs. JQF extracts code coverage using bytecode instrumentation, and input validity using JUnit's [`Assume`](https://junit.org/junit4/javadoc/4.12/org/junit/Assume.html) API. An input is valid if no assumptions are violated.

## Example

Here is a JUnit-Quickcheck test for checking a property of the [PatriciaTrie](https://commons.apache.org/proper/commons-collections/apidocs/org/apache/commons/collections4/trie/PatriciaTrie.html) class from [Apache Commons Collections](https://commons.apache.org/proper/commons-collections/). The property tests that if a `PatriciaTrie` is initialized with an input JDK `Map`, and if the input map already contains a key, then that key should also exist in the newly constructed `PatriciaTrie`.

```java
@RunWith(JQF.class)
public class PatriciaTrieTest {

    @Fuzz  /* The args to this method will be generated automatically by JQF */
    public void testMap2Trie(Map<String, Integer> map, String key) {
        // Key should exist in map
        assumeTrue(map.containsKey(key));   // the test is invalid if this predicate is not true

        // Create new trie with input `map`
        Trie trie = new PatriciaTrie(map);

        // The key should exist in the trie as well
        assertTrue(trie.containsKey(key));  // fails when map = {"x": 1, "x\0": 2} and key = "x"
    }
}
```

Running `mvn jqf:fuzz` causes JQF to invoke the `testMap2Trie()` method repeatedly with automatically generated values for `map` and `key`. After about 5 seconds on average (~5,000 inputs), JQF will report an assertion violation. It finds [a bug in the implementation of `PatriciaTrie`](https://issues.apache.org/jira/browse/COLLECTIONS-714) that is unresolved as of v4.4. Random sampling of `map` and `key` values is unlikely to find the failing test case, which is a very special corner case (see the comments next to the assertion in the code above). JQF finds this violation easily using a coverage-guided called [**Zest**][ISSTA'19 paper]. To run this example as a standalone Maven project, check out the [jqf-zest-example repository](https://github.com/rohanpadhye/jqf-zest-example).

In the above example, the generators for `Map` and `String` were synthesized automatically by JUnitQuickCheck. It is also possible to specify generators for structured inputs manually. See the [tutorials](#tutorials) below.


## Documentation

* The [JQF Maven Plugin](https://github.com/rohanpadhye/JQF/wiki/JQF-Maven-Plugin) documentation shows how to run `mvn jqf:fuzz` and `mvn jqf:repro`.
* [Writing a JQF Test](https://github.com/rohanpadhye/JQF/wiki/Writing-a-JQF-test) demonstrates the creation of a JUnit-based parameterized test method for JQF.
* [The Guidance interface](https://github.com/rohanpadhye/jqf/wiki/The-Guidance-interface) docs show how JQF works internally, which is useful for researchers wishing to build custom guidance algorithms on top of JQF.
* [API docs](https://rohanpadhye.github.io/JQF/apidocs) are published at every major release, which is again useful for researchers wishing to extend JQF.

### Tutorials

* [Zest 101](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-Zest): A basic tutorial for fuzzing a standalone toy program using command-line scripts. Walks through the process of writing a test driver and structured input generator for `Calendar` objects.
* [Fuzzing a compiler with Zest](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-a-Compiler): A tutorial for fuzzing a non-trivial program -- the [Google Closure Compiler](https://github.com/google/closure-compiler) -- using a generator for JavaScript programs. This tutorial makes use of the [JQF Maven plugin](https://github.com/rohanpadhye/jqf/wiki/JQF-Maven-Plugin).
* [Fuzzing with AFL](https://github.com/rohanpadhye/jqf/wiki/Fuzzing-with-AFL): A tutorial for fuzzing a Java program that parses binary data, such as PNG image files, using the AFL binary fuzzing engine.
* [Fuzzing with ZestCLI](https://gitlab.com/gitlab-org/security-products/demos/coverage-fuzzing/java-fuzzing-example): A tutorial of fuzzing a Java program with ZestCLI 

### Continuous Fuzzing

[GitLab](https://docs.gitlab.com/ee/user/application_security/coverage_fuzzing/) supports running JQF in CI/CD ([tutorial](https://gitlab.com/gitlab-org/security-products/demos/coverage-fuzzing/java-fuzzing-example)), though they have recently rolled out their own custom Java fuzzer for this purpose.

## Research and Tools based on JQF

* **[Zest](https://github.com/rohanpadhye/jqf-zest-example)** 🍝 [[ISSTA'19 paper]] - Semantic Fuzzing 
* **[BigFuzz](https://github.com/UCLA-SEAL/BigFuzz)** 🍝 [[ASE'20 paper]] - Spark Fuzzing
* **[MoFuzz](https://github.com/hub-se/MoFuzz)** [[ASE'20 paper](https://doi.org/10.1145/3324884.3416668)] - Model-driven software
* **[RLCheck](https://github.com/sameerreddy13/rlcheck)** 🍝 [[ICSE'20 paper]] - Reinforcement learning 
* **[Bonsai](https://github.com/vasumv/bonsai-fuzzing)** 🍝 [[ICSE'21 paper]] - Concise test generation
* **[Confetti](https://github.com/neu-se/CONFETTI)** [[ICSE'22 paper](https://doi.org/10.1145/3510003.3510628)] - Concolic / taint tracking with global hinting
* **[BeDivFuzz](https://github.com/hub-se/BeDivFuzz)**  [[ICSE'22 paper](https://doi.org/10.1145/3510003.3510182)]- Behaviorial diversity
* **[ODDFuzz](https://github.com/ODDFuzz/ODDFuzz)** [[IEEE S&P'23 paper](https://arxiv.org/pdf/2304.04233.pdf)]  - Deserialization vulnerabilities
* **[GCMiner](https://github.com/GCMiner/GCMiner)** [[ICSE'23 paper](https://arxiv.org/pdf/2303.07593.pdf)] - Gadget chain mining
* **[Intender](https://github.com/purseclab/intender)** [[USENIX Security'23 paper](https://www.usenix.org/system/files/sec23fall-prepub-285_kim-jiwon.pdf)] - Intent-based networking
* **[Mu2](https://github.com/cmu-pasta/mu2)** 🍝 [[ISSTA'23 paper]] - Mutation testing as guidance
* **[TOAST](http://dx.doi.org/10.1007/s11390-021-1693-1)** [[JCST'22 paper](https://link.springer.com/article/10.1007/s11390-021-1693-1)] - Testing dynamic software updates
* **[Poracle](https://github.com/PLaSE-UNIST/poracle-tool)** [[TOSEM'23 paper](http://www.jooyongyi.com/papers/TOSEM23.pdf)] - Patch testing using differential fuzzing
* **[SPIDER](https://arxiv.org/abs/2209.04026)** 🍝 [[arxiv preprint](https://arxiv.org/abs/2209.04026)] - Stateful performance issues in SDN
* **[FuzzDiff](https://github.com/akashpatil7/FuzzDiff)** [[Dissertation](https://www.scss.tcd.ie/publications/theses/diss/2022/TCD-SCSS-DISSERTATION-2022-134.pdf)] - Dynamic program equivalence checking

🍝 = Involves at least one of the original JQF authors.

## Contact the developers

If you've found a bug in JQF or are having trouble getting JQF to work, please open an issue on the [issue tracker](https://github.com/rohanpadhye/jqf/issues). You can also use this platform to post feature requests.

If it's some sort of fuzzing emergency you can always send an email to the main developer: [Rohan Padhye](https://rohan.padhye.org).

## Trophies

If you find bugs with JQF and you comfortable with sharing, We would be happy to add them to this list. 
Please send a PR for README.md with a link to the bug/cve you found.

- [google/closure-compiler#2842](https://github.com/google/closure-compiler/issues/2842): IllegalStateException in VarCheck: Unexpected variable
- [google/closure-compiler#2843](https://github.com/google/closure-compiler/issues/2843): NullPointerException when using Arrow Functions in dead code 
- [google/closure-compiler#3173](https://github.com/google/closure-compiler/issues/3173): Algorithmic complexity / performance issue on fuzzed input
- [google/closure-compiler#3220](https://github.com/google/closure-compiler/issues/3220): ExpressionDecomposer throws IllegalStateException: Object method calls can not be decomposed
- [JDK-8190332](https://bugs.openjdk.java.net/browse/JDK-8190332): PngReader throws NegativeArraySizeException when width is too large
- [JDK-8190511](https://bugs.openjdk.java.net/browse/JDK-8190511): PngReader throws OutOfMemoryError for very small malformed PNGs
- [JDK-8190512](https://bugs.openjdk.java.net/browse/JDK-8190512): PngReader throws undocumented IllegalArgumentException: "Empty Region" instead of IOException for malformed images with negative dimensions
- [JDK-8190997](https://bugs.openjdk.java.net/browse/JDK-8190997): PngReader throws NullPointerException when PLTE section is missing
- [JDK-8191023](https://bugs.openjdk.java.net/browse/JDK-8191023): PngReader throws NegativeArraySizeException in parse_tEXt_chunk when keyword length exceeeds chunk size
- [JDK-8191076](https://bugs.openjdk.java.net/browse/JDK-8191076): PngReader throws  NegativeArraySizeException in parse_zTXt_chunk when keyword length exceeds chunk size
- [JDK-8191109](https://bugs.openjdk.java.net/browse/JDK-8191109): PngReader throws NegativeArraySizeException in parse_iCCP_chunk when keyword length exceeds chunk size
- [JDK-8191174](https://bugs.openjdk.java.net/browse/JDK-8191174): PngReader throws undocumented llegalArgumentException with message "Pixel stride times width must be <= scanline stride"
- [JDK-8191073](https://bugs.openjdk.java.net/browse/JDK-8191073): JpegImageReader throws IndexOutOfBoundsException when reading malformed header
- [JDK-8193444](https://bugs.openjdk.java.net/browse/JDK-8193444): SimpleDateFormat throws ArrayIndexOutOfBoundsException when format contains long sequences of unicode characters
- [JDK-8193877](https://bugs.openjdk.java.net/browse/JDK-8193877): DateTimeFormatterBuilder throws ClassCastException when using padding
- [mozilla/rhino#405](https://github.com/mozilla/rhino/issues/405): FAILED ASSERTION due to malformed destructuring syntax
- [mozilla/rhino#406](https://github.com/mozilla/rhino/issues/406): ClassCastException when compiling malformed destructuring expression
- [mozilla/rhino#407](https://github.com/mozilla/rhino/issues/407): java.lang.VerifyError in bytecode produced by CodeGen
- [mozilla/rhino#409](https://github.com/mozilla/rhino/issues/409): ArrayIndexOutOfBoundsException when parsing '<!-'
- [mozilla/rhino#410](https://github.com/mozilla/rhino/issues/410): NullPointerException in BodyCodeGen
- [COLLECTIONS-714](https://issues.apache.org/jira/browse/COLLECTIONS-714): PatriciaTrie ignores trailing null characters in keys
- [COMPRESS-424](https://issues.apache.org/jira/browse/COMPRESS-424): BZip2CompressorInputStream throws ArrayIndexOutOfBoundsException(s) when decompressing malformed input
- [LANG-1385](https://issues.apache.org/jira/browse/LANG-1385): StringIndexOutOfBoundsException in NumberUtils.createNumber
- [**CVE-2018-11771**](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-11771): Infinite Loop in Commons-Compress ZipArchiveInputStream ([found by Tobias Ospelt](https://www.floyd.ch/?p=1090))
- [MNG-6375](https://issues.apache.org/jira/browse/MNG-6375) / [plexus-utils#34](https://github.com/codehaus-plexus/plexus-utils/issues/34): NullPointerException when pom.xml has incomplete XML tag
- [MNG-6374](https://issues.apache.org/jira/browse/MNG-6374) / [plexus-utils#35](https://github.com/codehaus-plexus/plexus-utils/issues/35): ModelBuilder hangs with malformed pom.xml
- [MNG-6577](https://issues.apache.org/jira/browse/MNG-6577) / [plexus-utils#57](https://github.com/codehaus-plexus/plexus-utils/issues/57): Uncaught IllegalArgumentException when parsing unicode entity ref
- [Bug 62655](https://bz.apache.org/bugzilla/show_bug.cgi?id=62655): Augment task: IllegalStateException when "id" attribute is missing 
- [BCEL-303](https://issues.apache.org/jira/browse/BCEL-303): AssertionViolatedException in Pass 3A Verification of invoke instructions
- [BCEL-307](https://issues.apache.org/jira/browse/BCEL-307): ClassFormatException thrown in Pass 3A verification
- [BCEL-308](https://issues.apache.org/jira/browse/BCEL-308): NullPointerException in Verifier Pass 3A
- [BCEL-309](https://issues.apache.org/jira/browse/BCEL-309): NegativeArraySizeException when Code attribute length is negative
- [BCEL-310](https://issues.apache.org/jira/browse/BCEL-310): ArrayIndexOutOfBounds in Verifier Pass 3A
- [BCEL-311](https://issues.apache.org/jira/browse/BCEL-311): ClassCastException in Verifier Pass 2
- [BCEL-312](https://issues.apache.org/jira/browse/BCEL-312): AssertionViolation: INTERNAL ERROR Please adapt StringRepresentation to deal with ConstantPackage in Verifier Pass 2
- [BCEL-313](https://issues.apache.org/jira/browse/BCEL-313): ClassFormatException: Invalid signature: Ljava/lang/String)V in Verifier Pass 3A
- [**CVE-2018-8036**](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-8036): Infinite Loop leading to OOM in PDFBox's AFMParser ([found by Tobias Ospelt](https://www.floyd.ch/?p=1090))
- [PDFBOX-4333](https://issues.apache.org/jira/browse/PDFBOX-4333): ClassCastException when loading PDF (found by Robin Schimpf)
- [PDFBOX-4338](https://issues.apache.org/jira/browse/PDFBOX-4338): ArrayIndexOutOfBoundsException in COSParser (found by Robin Schimpf)
- [PDFBOX-4339](https://issues.apache.org/jira/browse/PDFBOX-4339): NullPointerException in COSParser (found by Robin Schimpf)
- [**CVE-2018-8017**](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-8017): Infinite Loop in IptcAnpaParser 
- [**CVE-2018-12418**](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2018-12418): Infinite Loop in junrar ([found by Tobias Ospelt](https://www.floyd.ch/?p=1090))
- [**CVE-2019-17359**](https://cve.mitre.org/cgi-bin/cvename.cgi?name=CVE-2019-17359): Attempt to trigger a large allocation leads to OOM in Bouncycastle ASN.1 parser ([found by Tobias Ospelt](https://www.youtube.com/watch?v=RaBGEgQiE-4))
