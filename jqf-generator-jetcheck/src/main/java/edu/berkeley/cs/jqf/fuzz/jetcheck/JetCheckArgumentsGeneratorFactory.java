/*
 * Copyright (c) 2026 Vladimir Sitnikov and JQF Contributors
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs.jqf.fuzz.jetcheck;

import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZoneOffset;
import java.util.UUID;

import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGeneratorFactory;
import org.jetbrains.jetCheck.Generator;

/**
 * Builds <a href="https://github.com/JetBrains/jetCheck">jetCheck</a>-backed
 * {@link ArgumentsGenerator}s.
 *
 * <p>A third reference provider, after junit-quickcheck and Instancio. Unlike
 * Instancio, jetCheck reads its randomness as a stream of ints rather than from a
 * single seed, so Zest's byte-level mutations map onto local, structural changes in
 * the generated value -- a better fit for coverage-guided search.
 *
 * <p>jetCheck has no reflective object builder, so this provider maps a fixed set of
 * parameter types to jetCheck-built values: the eight primitives (and their wrappers),
 * {@link String}, {@link UUID}, the {@code java.time} date-time types ({@link LocalDate},
 * {@link LocalTime}, {@link LocalDateTime}, {@link OffsetTime}, {@link OffsetDateTime}) and
 * their {@code java.sql} counterparts ({@link Date}, {@link Time}, {@link Timestamp}). Any
 * other type fails fast with a message pointing to a richer provider.
 */
public final class JetCheckArgumentsGeneratorFactory implements ArgumentsGeneratorFactory {

    // Two ints make a full-width long, so the UUID spans its whole 128-bit space.
    private static final Generator<Long> LONGS = Generator.integers().flatMap(high ->
            Generator.integers().map(low -> ((long) high << 32) | (low & 0xFFFFFFFFL)));

    private static final Generator<UUID> UUIDS = LONGS.flatMap(
            mostSignificant -> LONGS.map(leastSignificant -> new UUID(mostSignificant, leastSignificant)));

    // Dates stay within year 1..9999 so every java.time and java.sql formatter is well defined.
    private static final Generator<LocalDate> LOCAL_DATES = Generator.integers(
            (int) LocalDate.of(1, 1, 1).toEpochDay(),
            (int) LocalDate.of(9999, 12, 31).toEpochDay()).map(LocalDate::ofEpochDay);

    private static final Generator<LocalTime> LOCAL_TIMES = Generator.integers(0, 86_399).flatMap(
            second -> Generator.integers(0, 999_999_999).map(
                    nano -> LocalTime.ofSecondOfDay(second).withNano(nano)));

    private static final Generator<LocalDateTime> LOCAL_DATE_TIMES = LOCAL_DATES.flatMap(
            date -> LOCAL_TIMES.map(time -> LocalDateTime.of(date, time)));

    // Whole-minute offsets across the [-18:00, +18:00] range ZoneOffset accepts.
    private static final Generator<ZoneOffset> OFFSETS = Generator.integers(-1080, 1080).map(
            minutes -> ZoneOffset.ofTotalSeconds(minutes * 60));

    private static final Generator<OffsetTime> OFFSET_TIMES = LOCAL_TIMES.flatMap(
            time -> OFFSETS.map(offset -> OffsetTime.of(time, offset)));

    private static final Generator<OffsetDateTime> OFFSET_DATE_TIMES = LOCAL_DATE_TIMES.flatMap(
            dateTime -> OFFSETS.map(offset -> OffsetDateTime.of(dateTime, offset)));

    // java.sql.Time has second resolution, so drop the sub-second part.
    private static final Generator<LocalTime> SECOND_TIMES =
            Generator.integers(0, 86_399).map(LocalTime::ofSecondOfDay);

    @Override
    public ArgumentsGenerator create(Class<?> testClass, Method testMethod) {
        Class<?>[] parameterTypes = testMethod.getParameterTypes();
        Generator<?>[] generators = new Generator<?>[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            generators[i] = generatorFor(parameterTypes[i]);
        }
        return JetCheckArgumentsGenerator.builder(generators).build();
    }

    /**
     * Maps a parameter type to a jetCheck generator for one of the supported types.
     *
     * <p>Public so a custom factory can reuse these built-in mappings for common types and
     * supply its own generators only for its own types.
     *
     * @param type the parameter type
     * @return a generator for that type
     * @throws IllegalArgumentException if the type is not supported
     */
    public static Generator<?> generatorFor(Class<?> type) {
        if (type == boolean.class || type == Boolean.class) {
            return Generator.booleans();
        }
        if (type == int.class || type == Integer.class) {
            return Generator.integers();
        }
        if (type == long.class || type == Long.class) {
            return Generator.integers().map(Integer::longValue);
        }
        if (type == short.class || type == Short.class) {
            return Generator.integers(Short.MIN_VALUE, Short.MAX_VALUE).map(Integer::shortValue);
        }
        if (type == byte.class || type == Byte.class) {
            return Generator.integers(Byte.MIN_VALUE, Byte.MAX_VALUE).map(Integer::byteValue);
        }
        if (type == char.class || type == Character.class) {
            return Generator.asciiPrintableChars();
        }
        if (type == double.class || type == Double.class) {
            return Generator.doubles();
        }
        if (type == float.class || type == Float.class) {
            return Generator.doubles().map(Double::floatValue);
        }
        if (type == String.class) {
            return Generator.stringsOf(Generator.asciiPrintableChars());
        }
        if (type == UUID.class) {
            return UUIDS;
        }
        if (type == LocalDate.class) {
            return LOCAL_DATES;
        }
        if (type == LocalTime.class) {
            return LOCAL_TIMES;
        }
        if (type == LocalDateTime.class) {
            return LOCAL_DATE_TIMES;
        }
        if (type == OffsetTime.class) {
            return OFFSET_TIMES;
        }
        if (type == OffsetDateTime.class) {
            return OFFSET_DATE_TIMES;
        }
        if (type == Date.class) {
            return LOCAL_DATES.map(Date::valueOf);
        }
        if (type == Time.class) {
            return SECOND_TIMES.map(Time::valueOf);
        }
        if (type == Timestamp.class) {
            return LOCAL_DATE_TIMES.map(Timestamp::valueOf);
        }
        throw new IllegalArgumentException("The jetCheck provider supports the primitives, their "
                + "wrappers, String, UUID, and the java.time / java.sql date-time types, not "
                + type.getName() + ". Use jqf-generator-quickcheck or jqf-generator-instancio for "
                + "richer types.");
    }
}
