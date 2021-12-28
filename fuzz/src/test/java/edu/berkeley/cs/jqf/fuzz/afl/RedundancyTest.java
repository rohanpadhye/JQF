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
package edu.berkeley.cs.jqf.fuzz.afl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.pholser.junit.quickcheck.Property;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.Size;
import com.pholser.junit.quickcheck.runner.JUnitQuickcheck;
import org.eclipse.collections.impl.list.mutable.primitive.IntArrayList;
import org.junit.Assert;
import org.junit.runner.RunWith;

@RunWith(JUnitQuickcheck.class)
public class RedundancyTest {

    @Property
    public void testDiscretization(@InRange(minDouble=0.0, maxDouble=1.0) Double score) {
        // Discretize a redundancy score
        int discrete = PerfFuzzGuidance.discretizeScore(score);

        // Ensure that the discretization is within the byte range
        Assert.assertTrue(discrete >= 0 && discrete <= Integer.MAX_VALUE);

    }

    @Property
    public void testRedundancyScore(@Size(min=1, max = 5) List<@InRange(minInt=1, maxInt=10) Integer> counts) {
        // Let's make the sum of counts a perfect square
        int sumCounts = sum(counts);
        int squareSum = nextPerfectSquare(sumCounts);
        int deficit = squareSum - sumCounts;
        if (deficit > 0) {
            counts.add(deficit);
        }
        Assert.assertTrue(sum(counts) == squareSum && isPerfectSquare(squareSum));

        int root = (int)(Math.round(Math.sqrt(squareSum)));
        Assert.assertTrue(root*root == squareSum);


        IntArrayList redundantCounts = new IntArrayList(root);
        for (int i = 0; i < root; i++) {
            redundantCounts.add(root);
        }
        Assert.assertTrue(redundantCounts.sum() == squareSum);

        IntArrayList countsIntArrayList = new IntArrayList(counts.size());
        for(int i : counts){
            countsIntArrayList.add(i);
        }
        // Compute redundancy score for some memory accesses
        double score = PerfFuzzGuidance.computeRedundancyScore(countsIntArrayList);

        // Ensure that scores are in [0, 1)
        Assert.assertTrue(score >= 0 && score < 1);

        // Ensure that it is not more than the max possible
        double maxScore = PerfFuzzGuidance.computeRedundancyScore(redundantCounts);
        Assert.assertTrue(maxScore >= score);


    }

    private static boolean isPerfectSquare(int x) {
        int maybeRoot = (int) Math.round(Math.sqrt(x));
        return maybeRoot * maybeRoot == x;
    }

    private static int nextPerfectSquare(int x) {
        while (!isPerfectSquare(x)) {
            x++;
        }
        return x;
    }

    private static int sum(Collection<Integer> values) {
        int result = 0;
        for (int value : values) {
            result += value;
        }
        return result;
    }

}
