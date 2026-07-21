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
package edu.berkeley.cs.jqf.fuzz.instancio;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import edu.berkeley.cs.jqf.fuzz.spi.ArgumentsGenerator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for {@link InstancioArgumentsGenerator}: it builds fully populated
 * objects and maps the same input bytes onto the same arguments every time.
 */
class InstancioArgumentsGeneratorTest {

    private static ArgumentsGenerator generatorFor(String methodName) throws NoSuchMethodException {
        Method method = Target.class.getMethod(methodName, Widget.class);
        return new InstancioArgumentsGeneratorFactory().create(Target.class, method);
    }

    private static InputStream inputOf(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }

    @Test
    void populatesTheArgumentObject() throws Exception {
        ArgumentsGenerator generator = generatorFor("accept");

        Object[] arguments = generator.generate(inputOf(input()));

        assertEquals(1, arguments.length);
        Widget widget = assertInstanceOf(Widget.class, arguments[0]);
        // Instancio fills every field by default, so a generated object is fully populated.
        assertNotNull(widget.getName(), "Instancio should populate the String field");
        assertNotNull(widget.getNested(), "Instancio should populate the nested object");
    }

    @Test
    void isDeterministicForTheSameInput() throws Exception {
        ArgumentsGenerator generator = generatorFor("accept");
        byte[] bytes = input();

        Widget first = (Widget) generator.generate(inputOf(bytes))[0];
        Widget second = (Widget) generator.generate(inputOf(bytes))[0];

        // Replaying the same bytes (as jqf:repro does) must rebuild the same arguments.
        assertEquals(first.getName(), second.getName());
        assertEquals(first.getSize(), second.getSize());
        assertEquals(first.getNested().getLabel(), second.getNested().getLabel());
    }

    private static byte[] input() {
        return new byte[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16};
    }

    static class Target {
        public void accept(Widget widget) {
            // Only its signature matters here.
        }
    }

    public static class Widget {
        private String name;
        private int size;
        private Nested nested;

        public String getName() {
            return name;
        }

        public int getSize() {
            return size;
        }

        public Nested getNested() {
            return nested;
        }
    }

    public static class Nested {
        private String label;

        public String getLabel() {
            return label;
        }
    }
}
