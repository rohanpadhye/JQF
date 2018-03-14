/*
 * Copyright (c) 2017-2018 The Regents of the University of California
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

import java.util.Arrays;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReadEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import janala.logger.inst.INVOKESTATIC;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeTrue;

@RunWith(JUnitQuickcheck.class)
public class ExecutionIndexingTest {

    private static CallEvent callEvent(int iid) {
        return new CallEvent(iid, null, 0,
                new INVOKESTATIC(iid, 0, "Foo", "bar", "()V"));
    }

    private static ReturnEvent returnEvent(int iid) {
        return new ReturnEvent(iid, null, 0);
    }

    private static ReadEvent readEvent(int iid) {
        return new ReadEvent(iid, null, 0, 0, "random");
    }

    @Test
    public void testDepth0() {
        ExecutionIndexingState e = new ExecutionIndexingState();
        int[] ei = e.getExecutionIndex(readEvent(42)).ei;

        int[] expected = {42, 1};
        assertArrayEquals(expected, ei);
    }

    @Test
    public void testDepth1() {
        ExecutionIndexingState e = new ExecutionIndexingState();
        e.pushCall(callEvent(4));
        int[] ei = e.getExecutionIndex(readEvent(42)).ei;
        e.popReturn(returnEvent(-1));

        int[] expected = {4, 1, 42, 1};
        assertArrayEquals(expected, ei);
    }


    @Test
    public void testDepth1withRepeat() {
        ExecutionIndexingState e = new ExecutionIndexingState();
        e.pushCall(callEvent(4));
        e.getExecutionIndex(readEvent(42));
        e.getExecutionIndex(readEvent(41));
        e.getExecutionIndex(readEvent(42));
        int[] ei = e.getExecutionIndex(readEvent(42)).ei;
        e.popReturn(returnEvent(-1));

        int[] expected = {4, 1, 42, 3};
        assertArrayEquals(expected, ei);
    }


    @Test
    public void testDepth2withRepeat() {
        ExecutionIndexingState e = new ExecutionIndexingState();
        int[] ei;
        e.pushCall(callEvent(4));
        e.popReturn(returnEvent(-1));

        e.pushCall(callEvent(4));
        {
            e.pushCall(callEvent(5));
            e.getExecutionIndex(readEvent(42));
            e.popReturn(returnEvent(-1));
        }
        e.popReturn(returnEvent(-1));

        e.pushCall(callEvent(3));
        {
            e.pushCall(callEvent(5));
            e.popReturn(returnEvent(-1));
            e.pushCall(callEvent(5));
            e.popReturn(returnEvent(-1));
            e.pushCall(callEvent(5));
            e.popReturn(returnEvent(-1));
            e.pushCall(callEvent(5));
            e.popReturn(returnEvent(-1));
        }
        e.popReturn(returnEvent(-1));

        e.pushCall(callEvent(4));
        {
            e.pushCall(callEvent(5));
            e.popReturn(returnEvent(-1));
            e.pushCall(callEvent(5));
            e.getExecutionIndex(readEvent(41));
            e.getExecutionIndex(readEvent(42));
            ei = e.getExecutionIndex(readEvent(42)).ei;

            e.popReturn(returnEvent(-1));
        }
        e.popReturn(returnEvent(-1));


        int[] expected = {4, 3, 5, 2, 42, 2};
        assertArrayEquals(expected, ei);
    }

    @Property
    public void validExecutionIndex(@InRange(minInt=1, maxInt=32) int @Size(min=2, max=48)[] expected) {
        assumeTrue(expected.length % 2 == 0);
        ExecutionIndexingState e = new ExecutionIndexingState();
        int i;
        int[] ei = null;
        for (i = 0; i < expected.length-2; i+= 2) {
            int iid = expected[i];
            int times = expected[i+1];
            for (int j = 0; j < times-1; j++) {
                e.pushCall(callEvent(iid));
                e.popReturn(returnEvent(-1));
            }
            e.pushCall(callEvent(iid));
        }
        int iid = expected[i];
        int times = expected[i+1];
        for (int j = 0; j < times; j++) {
            ei = e.getExecutionIndex(readEvent(iid)).ei;
        }

        assertArrayEquals(expected, ei);
    }

    @Property
    public void comparesEqual(int @Size(min=1, max=20)[] v1) {
        assumeTrue(v1.length % 2 == 0);
        int[] v2 = Arrays.copyOf(v1, v1.length);
        ExecutionIndex e1 = new ExecutionIndex(v1);
        ExecutionIndex e2 = new ExecutionIndex(v2);
        assertEquals(0, e1.compareTo(e2));
    }

    @Property
    public void comparesLexicographically(@InRange(minInt=1, maxInt=32) int @Size(min=1, max=20)[] v1) {
        assumeTrue(v1.length % 2 == 0);
        int[] v2 = Arrays.copyOf(v1, v1.length);
        v2[v1.length/2]--; // Make v2 less than v1

        ExecutionIndex e1 = new ExecutionIndex(v1);
        ExecutionIndex e2 = new ExecutionIndex(v2);
        assertThat(e1.compareTo(e2), greaterThan(0));
        assertThat(e2.compareTo(e1), lessThan(0));
    }


    @Property
    public void comparesLength(@InRange(minInt=1, maxInt=32) int @Size(min=4, max=20)[] v1) {
        assumeTrue(v1.length % 2 == 0);
        int[] v2 = Arrays.copyOf(v1, v1.length-2); // v2 is smaller

        ExecutionIndex e1 = new ExecutionIndex(v1);
        ExecutionIndex e2 = new ExecutionIndex(v2);
        assertThat(e1.compareTo(e2), greaterThan(0));
        assertThat(e2.compareTo(e1), lessThan(0));
    }


    @Property
    public void hasOwnPrefixAndSuffix(@InRange(minInt=1, maxInt=32) int @Size(min=4, max=20)[] v) {
        assumeTrue(v.length % 2 == 0);
        ExecutionIndex e = new ExecutionIndex(v);

        ExecutionIndex.Suffix selfCommonSuffix = e.getCommonSuffix(e);
        ExecutionIndex.Prefix suffixMinusPrefix = e.getPrefixOfSuffix(selfCommonSuffix);

        assertEquals(v.length/2, selfCommonSuffix.size());
        assertEquals(0, suffixMinusPrefix.size());
        assertEquals(true, e.hasPrefix(suffixMinusPrefix));
    }

    @Property
    public void splitsAndJoinsPrefixSuffix(@InRange(minInt=1, maxInt=32) int @Size(min=20, max=20)[] v,
                                           @InRange(minInt=0, maxInt=10) int offset) {
        assumeTrue(v.length % 2 == 0);
        ExecutionIndex e = new ExecutionIndex(v);

        ExecutionIndex.Prefix prefix = new ExecutionIndex.Prefix(e, offset);
        ExecutionIndex.Suffix suffix = e.getSuffixOfPrefix(prefix);

        assertEquals(e, new ExecutionIndex(prefix, suffix));

    }
}
