# jqf-generator-jetcheck

A [jetCheck](https://github.com/JetBrains/jetCheck) argument-generator provider for JQF.

It implements the engine SPI `edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGeneratorFactory` and is a third reference provider, after `jqf-generator-quickcheck` and `jqf-generator-instancio`. Like the others, it proves the generator is a pluggable choice: the engine and both run paths (JUnit 4 and JUnit 5) drive it without depending on jetCheck.

## What it depends on

`jqf-core` and `jetCheck`, nothing else. There is no junit-quickcheck and no JUnit 4 on the compile or runtime classpath. jetCheck runs on Java 8+.

## How to use it

Put `jqf-generator-jetcheck` on the test classpath, and the engine resolves it through `ServiceLoader`:

```xml
<dependency>
    <groupId>edu.berkeley.cs.jqf</groupId>
    <artifactId>jqf-generator-jetcheck</artifactId>
    <scope>test</scope>
</dependency>
```

```java
@FuzzTest
void acceptsGeneratedValues(int count, String label) {
    // count and label are built by jetCheck
}
```

To pick it per test while another provider is also on the classpath, name it explicitly:

```java
@FuzzTest(arguments = JetCheckArgumentsGeneratorFactory.class)
void acceptsGeneratedValues(int count, String label) { ... }
```

jetCheck has no reflective object builder, so the provider maps a fixed set of parameter types to jetCheck generators: the eight primitives (and their wrappers), `String`, `UUID`, the `java.time` date-time types (`LocalDate`, `LocalTime`, `LocalDateTime`, `OffsetTime`, `OffsetDateTime`), and their `java.sql` counterparts (`Date`, `Time`, `Timestamp`). Any other type fails fast with a message pointing to a richer provider.

## Custom types

For a type the built-in factory does not cover, write your own `ArgumentsGeneratorFactory` and reuse this module's public pieces instead of re-implementing generation. `JetCheckArgumentsGeneratorFactory.generatorFor(Class)` returns the built-in generator for a supported type, and `JetCheckArgumentsGenerator.builder(...)` reuses the per-trial loop — the per-parameter draw and the null / `CannotSatisfyCondition` skip guard:

```java
public final class OrderArguments implements ArgumentsGeneratorFactory {
    private static final Map<Class<?>, Generator<?>> CUSTOM = Map.of(
            Order.class, orderGenerator(),
            Coercion.class, coercionGenerator());

    @Override
    public ArgumentsGenerator create(Class<?> testClass, Method method) {
        Generator<?>[] gens = Arrays.stream(method.getParameterTypes())
                .map(t -> CUSTOM.getOrDefault(t, JetCheckArgumentsGeneratorFactory.generatorFor(t)))
                .toArray(Generator<?>[]::new);
        return JetCheckArgumentsGenerator.builder(gens).sizeHint(24).build();
    }
}
```

Point a test at it with `@FuzzTest(arguments = OrderArguments.class)`. For finer control, `JetCheckGeneration.generate(generator, random, sizeHint)` drives a single generator against the guided stream, with the same skip guard.

## Why jetCheck: byte-structured guidance

jetCheck reads its randomness as a stream of ints, drawing only as many as each value needs. This provider routes every draw to the engine's guided byte stream, so Zest's byte-level mutations map onto local, structural changes in the generated value — flipping a byte changes one field or one list element, not the whole object. That is a better fit for coverage-guided search than a seed-based library such as Instancio, where a one-bit seed change reshapes everything at once.

The mapping is deterministic, so `jqf:repro` rebuilds the same arguments from a saved input.

## How it works

jetCheck 0.3.0 exposes `GenerationEnvironment.generative(IntSource, int)` and a public `IntSource`, which is the public entry point requested in [JetBrains/jetCheck#7](https://github.com/JetBrains/jetCheck/issues/7). The provider builds an environment from an `IntSource` that draws from JQF's `StreamBackedRandom`:

```java
IntSource source = distribution -> distribution.generateInt(random);
Object value = GenerationEnvironment.generative(source, sizeHint).generate(generator);
```

`IntDistribution.generateInt(java.util.Random)` is public and `StreamBackedRandom` is a `Random`, so each bounded draw consumes only the bytes it needs. No jetCheck internals are involved.

Before 0.3.0 there was no public single-value generation API, and an earlier version of this provider reached jetCheck's package-private internals; the 0.3.0 public hook removed that need.
