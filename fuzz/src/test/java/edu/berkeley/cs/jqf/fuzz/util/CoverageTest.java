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
package edu.berkeley.cs.jqf.fuzz.util;

import edu.berkeley.cs.jqf.instrument.tracing.events.BranchEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.CallEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReadEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.ReturnEvent;
import edu.berkeley.cs.jqf.instrument.tracing.events.TraceEvent;
import janala.logger.inst.INVOKESTATIC;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author Rohan Padhye
 */
@RunWith(JUnit4.class)
public class CoverageTest {
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

    private static BranchEvent branchEvent(int iid, int arm) {
        return new BranchEvent(iid, null, 0, arm);
    }


    @Test
    public void coverageCountsDistinctBranchesAndCalls() {
        Coverage c = new Coverage();
        c.handleEvent(callEvent(1));
        c.handleEvent(callEvent(2));
        c.handleEvent(branchEvent(3, 2));
        c.handleEvent(returnEvent(1));
        c.handleEvent(returnEvent(3));
        c.handleEvent(readEvent(1));
        c.handleEvent(readEvent(4));
        c.handleEvent(branchEvent(3, 1));
        c.handleEvent(branchEvent(4, 0));
        c.handleEvent(branchEvent(3, 2));
        c.handleEvent(callEvent(1));
        Assert.assertEquals(5, c.getNonZeroCount());
    }

    @Test
    public void testCoverageUpdateBits1() {
        Coverage c1 = new Coverage();
        Coverage c2 = new Coverage();
        Coverage total = new Coverage();
        TraceEvent[] baseEvents = { callEvent(1), callEvent(2), branchEvent(3, 1) };
        TraceEvent[] newEvents = { callEvent(4) };

        for (TraceEvent e : baseEvents) {
            c1.handleEvent(e);
            c2.handleEvent(e);
        }

        for (TraceEvent e : newEvents) {
            c2.handleEvent(e);
        }

        total.updateBits(c1);
        boolean changed = total.updateBits(c2);

        Assert.assertTrue(changed);
        Assert.assertEquals(c2.getNonZeroCount(), total.getNonZeroCount());
    }

    @Test
    public void testCoverageUpdateBits2() {
        Coverage c1 = new Coverage();
        Coverage c2 = new Coverage();
        Coverage total = new Coverage();
        TraceEvent[] baseEvents = { callEvent(1), callEvent(1), callEvent(2), branchEvent(3, 1) };
        TraceEvent[] newEvents = { callEvent(1) };

        for (TraceEvent e : baseEvents) {
            c1.handleEvent(e);
            c2.handleEvent(e);
        }

        for (TraceEvent e : newEvents) {
            c2.handleEvent(e);
        }

        total.updateBits(c1);
        boolean changed = total.updateBits(c2);

        Assert.assertFalse(changed); // Because hob(2) and hob(3) are the same
        Assert.assertEquals(c2.getNonZeroCount(), total.getNonZeroCount());
    }


    @Test
    public void testCoverageUpdateBits3() {
        Coverage c1 = new Coverage();
        Coverage c2 = new Coverage();
        Coverage total = new Coverage();
        TraceEvent[] baseEvents = { callEvent(1), callEvent(1), callEvent(1), callEvent(2), branchEvent(3, 1) };
        TraceEvent[] newEvents = { };

        for (TraceEvent e : baseEvents) {
            c1.handleEvent(e);
            c2.handleEvent(e);
        }

        for (TraceEvent e : newEvents) {
            c2.handleEvent(e);
        }

        total.updateBits(c1);
        boolean changed = total.updateBits(c2);

        Assert.assertFalse(changed);
        Assert.assertEquals(c2.getNonZeroCount(), total.getNonZeroCount());
    }



    @Test
    public void testCoverageUpdateBits4() {
        Coverage c1 = new Coverage();
        Coverage c2 = new Coverage();
        Coverage total = new Coverage();
        TraceEvent[] baseEvents = { callEvent(1), callEvent(2), branchEvent(3, 1) };
        TraceEvent[] newEvents = { callEvent(1), callEvent(1) };

        for (TraceEvent e : baseEvents) {
            c1.handleEvent(e);
            c2.handleEvent(e);
        }

        for (TraceEvent e : newEvents) {
            c2.handleEvent(e);
        }

        total.updateBits(c2);
        boolean changed = total.updateBits(c1);

        Assert.assertTrue(changed); // Because hob(3) and hob(1) are different
        Assert.assertEquals(c2.getNonZeroCount(), total.getNonZeroCount());
    }
}
