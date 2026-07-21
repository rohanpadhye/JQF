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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.UUID;

import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import org.jetbrains.jetCheck.Generator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for the jetCheck provider: it maps parameter types to jetCheck
 * generators and produces deterministic, byte-driven values from the guided stream.
 */
class JetCheckArgumentsGeneratorTest {

    private static ArgumentsGenerator generatorFor(String methodName, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        Method method = Target.class.getMethod(methodName, parameterTypes);
        return new JetCheckArgumentsGeneratorFactory().create(Target.class, method);
    }

    private static InputStream inputOf(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    private static byte[] filled(int length, int value) {
        byte[] bytes = new byte[length];
        for (int i = 0; i < length; i++) {
            bytes[i] = (byte) value;
        }
        return bytes;
    }

    @Test
    void generatesValuesOfTheDeclaredTypes() throws Exception {
        ArgumentsGenerator generator = generatorFor("acceptTwo", int.class, boolean.class);

        Object[] arguments = generator.generate(inputOf(varied()));

        assertEquals(2, arguments.length);
        assertInstanceOf(Integer.class, arguments[0]);
        assertInstanceOf(Boolean.class, arguments[1]);
    }

    @Test
    void isDeterministicForTheSameInput() throws Exception {
        ArgumentsGenerator generator = generatorFor("accept", int.class);
        byte[] bytes = varied();

        Object first = generator.generate(inputOf(bytes))[0];
        Object second = generator.generate(inputOf(bytes))[0];

        // Replaying the same bytes (as jqf:repro does) rebuilds the same value.
        assertEquals(first, second);
    }

    @Test
    void valueDependsOnTheInputBytes() throws Exception {
        ArgumentsGenerator generator = generatorFor("accept", int.class);

        // Byte-structured: all-zero and all-ones inputs drive different draws.
        Object fromZeros = generator.generate(inputOf(filled(64, 0x00)))[0];
        Object fromOnes = generator.generate(inputOf(filled(64, 0xFF)))[0];

        assertNotEquals(fromZeros, fromOnes);
    }

    @Test
    void publicApiSupportsCustomProviders() {
        // A custom ArgumentsGeneratorFactory reuses the built-in map for known types and supplies
        // its own jetCheck generator for the rest, then reuses the per-trial loop via the builder.
        Generator<?>[] generators = {
                JetCheckArgumentsGeneratorFactory.generatorFor(int.class),
                Generator.constant("custom"),
        };
        ArgumentsGenerator generator = JetCheckArgumentsGenerator.builder(generators).sizeHint(4).build();

        Object[] arguments = generator.generate(inputOf(varied()));

        assertEquals(2, arguments.length);
        assertInstanceOf(Integer.class, arguments[0]);
        assertEquals("custom", arguments[1]);
    }

    @Test
    void generatesTemporalAndUuidTypes() throws Exception {
        ArgumentsGenerator generator = generatorFor("acceptTemporal", UUID.class, LocalDate.class,
                LocalTime.class, LocalDateTime.class, OffsetTime.class, OffsetDateTime.class,
                Date.class, Time.class, Timestamp.class);

        // Nine multi-draw generators need more than the 64-byte sample the other tests use.
        byte[] bytes = new byte[512];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 7 + 1);
        }
        Object[] arguments = generator.generate(inputOf(bytes));

        assertEquals(9, arguments.length);
        assertInstanceOf(UUID.class, arguments[0]);
        assertInstanceOf(LocalDate.class, arguments[1]);
        assertInstanceOf(LocalTime.class, arguments[2]);
        assertInstanceOf(LocalDateTime.class, arguments[3]);
        assertInstanceOf(OffsetTime.class, arguments[4]);
        assertInstanceOf(OffsetDateTime.class, arguments[5]);
        assertInstanceOf(Date.class, arguments[6]);
        assertInstanceOf(Time.class, arguments[7]);
        assertInstanceOf(Timestamp.class, arguments[8]);
    }

    private static byte[] varied() {
        byte[] bytes = new byte[64];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (i * 7 + 1);
        }
        return bytes;
    }

    static class Target {
        public void accept(int value) {
            // Only its signature matters here.
        }

        public void acceptTwo(int value, boolean flag) {
            // Only its signature matters here.
        }

        public void acceptTemporal(UUID uuid, LocalDate localDate, LocalTime localTime,
                LocalDateTime localDateTime, OffsetTime offsetTime, OffsetDateTime offsetDateTime,
                Date sqlDate, Time sqlTime, Timestamp sqlTimestamp) {
            // Only its signature matters here.
        }
    }
}
