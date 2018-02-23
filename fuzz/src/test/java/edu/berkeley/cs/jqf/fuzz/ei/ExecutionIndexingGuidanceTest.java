/*
 * Copyright (c) 2018, University of California, Berkeley
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
package edu.berkeley.cs.jqf.fuzz.ei;

import java.util.Random;

import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndexingGuidance.Input;
import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndex;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(JUnitQuickcheck.class)
public class ExecutionIndexingGuidanceTest {

    private static Random r;

    private ExecutionIndex e1 = new ExecutionIndex(new int[]{1});
    private ExecutionIndex e2 = new ExecutionIndex(new int[]{2});
    private ExecutionIndex e3 = new ExecutionIndex(new int[]{1,2});
    private ExecutionIndex e4 = new ExecutionIndex(new int[]{1,2,3,4});
    private ExecutionIndex e5 = new ExecutionIndex(new int[]{5,5,5});

    @Before
    public void seedRandom() {
        r = new Random(42);
    }

    @Test
    public void testGetOrFresh() {
        Input input = new Input();
        int k1a = input.getOrGenerateFresh(e1, r);
        int k1b = input.getOrGenerateFresh(e1, r);
        assertEquals(k1a, k1b);

        int k2a = input.getOrGenerateFresh(e2, r);
        int k2b = input.getOrGenerateFresh(e2, r);
        assertEquals(k2a, k2b);

        int k1c = input.getOrGenerateFresh(e1, r);
        assertEquals(k1a, k1c);
    }

    @Test
    public void testClone() {
        Input input = new Input();
        int k1 = input.getOrGenerateFresh(e1, r);
        int k2 = input.getOrGenerateFresh(e2, r);
        int k3 = input.getOrGenerateFresh(e3, r);
        int k4 = input.getOrGenerateFresh(e4, r);
        int k5 = input.getOrGenerateFresh(e5, r);

        Input clone = new Input(input);
        assertEquals(k1, clone.getOrGenerateFresh(e1, r));
        assertEquals(k2, clone.getOrGenerateFresh(e2, r));
        assertEquals(k3, clone.getOrGenerateFresh(e3, r));
        assertEquals(k4, clone.getOrGenerateFresh(e4, r));
        assertEquals(k5, clone.getOrGenerateFresh(e5, r));

    }


    @Test
    public void testGc() {
        Input input = new Input();
        int k1 = input.getOrGenerateFresh(e1, r);
        int k2 = input.getOrGenerateFresh(e2, r);
        int k3 = input.getOrGenerateFresh(e3, r);
        int k4 = input.getOrGenerateFresh(e4, r);
        int k5 = input.getOrGenerateFresh(e5, r);

        Input clone = new Input(input);
        assertEquals(k1, clone.getOrGenerateFresh(e1, r));
        assertEquals(k5, clone.getOrGenerateFresh(e5, r));
        assertEquals(k2, clone.getOrGenerateFresh(e2, r));

        clone.gc();

        // Note: The notEquals should succeed because we have
        // verified this with fixed random seed (see: prepareSeed)
        assertNotEquals(k3, clone.getOrGenerateFresh(e3, r));
        assertNotEquals(k4, clone.getOrGenerateFresh(e4, r));

    }
}
