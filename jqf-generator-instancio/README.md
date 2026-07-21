# jqf-generator-instancio

An [Instancio](https://www.instancio.org/) argument-generator provider for JQF.

It implements the engine SPI `edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGeneratorFactory` and is a second reference provider alongside `jqf-generator-quickcheck`. Its job is to show that the generator is a pluggable choice: the engine and both run paths (JUnit 4 and JUnit 5) drive it without depending on Instancio or junit-quickcheck.

## What it depends on

`jqf-core` and `instancio-core`, nothing else. There is no junit-quickcheck and no JUnit 4 on the compile or runtime classpath. Instancio 5.x is the last line that supports Java 8+; 6.x requires Java 17.

## How to use it

Put `jqf-generator-instancio` on the test classpath instead of `jqf-generator-quickcheck`, and the engine resolves it through `ServiceLoader`:

```xml
<dependency>
    <groupId>edu.berkeley.cs.jqf</groupId>
    <artifactId>jqf-generator-instancio</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@FuzzTest
void acceptsPopulatedObject(Order order) {
    // `order` is built by Instancio
}
```

To pick it per test while another provider is also on the classpath, name it explicitly:

```java
@FuzzTest(arguments = InstancioArgumentsGeneratorFactory.class)
void acceptsPopulatedObject(Order order) { ... }
```

For each parameter the provider builds an object with `Instancio.of(type).withSeed(seed).create()`, where `seed` is an eight-byte value drawn from the engine's guided byte stream. The mapping is deterministic, so `jqf:repro` rebuilds the same arguments from a saved input.

## Known trade-off: coarser guidance

Instancio takes a single `long` seed and produces a whole object graph from it. The fuzzer therefore steers generation only through that seed, not byte by byte: a one-bit change to the seed reshapes the entire object, and there is no structural correspondence between an input region and a specific field. By contrast, junit-quickcheck reads the byte stream incrementally, so Zest's byte-level mutations map onto local, structured changes in the generated value, which coverage feedback can exploit more precisely.

This is expected, not a bug. A seed-based library is a good fit for broad random exploration and for populating large object graphs with little setup; a byte-structured library (junit-quickcheck, or jetCheck, which maps onto an int/byte source) suits Zest's coverage-guided search better. Choose the provider that fits the target.
