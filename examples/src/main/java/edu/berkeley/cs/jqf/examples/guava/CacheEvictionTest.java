/*
 * Copyright (c) 2018, The Regents of the University of California
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
package edu.berkeley.cs.jqf.examples.guava;

import java.util.ArrayList;
import java.util.Set;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.pholser.junit.quickcheck.From;
import com.pholser.junit.quickcheck.generator.InRange;
import com.pholser.junit.quickcheck.generator.Size;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.runner.RunWith;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * @author Rohan Padhye
 */
@RunWith(JQF.class)
public class CacheEvictionTest {
    private CacheLoader identityLoader = CacheLoader.from((k) -> k);
    private ArrayList evictedElements = new ArrayList();
    private RemovalListener listener = (n) -> {
        assertEquals(RemovalCause.SIZE, n.getCause());
        evictedElements.add(n.getKey());
    };

    private static final int MAX_SIZE = 10;
    private static final int MAX_WEIGHT = 1000;

    @Fuzz
    public void testLru(int @Size(max = MAX_SIZE) [] initial, int @Size(max = MAX_SIZE) [] first, int @Size(max = MAX_SIZE) [] second) {
        LoadingCache cache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .maximumSize(MAX_SIZE)
                .removalListener(listener)
                .build(identityLoader);

        // Load initial elements
        for (int i : initial) {
            cache.getUnchecked(i);
        }

        // Load first round (may evict from `initial`)
        for (int i : first) {
            cache.getUnchecked(i);
        }

        // Ensure that everything in the first round is in cache
        for (int i : first) {
            assertNotNull(cache.getIfPresent(i));
        }

        // Load second round (may evict from `initial` and `first`)
        for (int i : second) {
            cache.getUnchecked(i);
        }

        // Ensure that everything in the second round is in cache
        for (int i : second) {
            assertNotNull(cache.getIfPresent(i));
        }

        // TODO: Assert that `evictedElements` contain the elements from initial/first/second in LRU order


    }

    @Fuzz
    public void testWeighting(@InRange(minInt=0, maxInt=1024) int @Size(max = MAX_SIZE) [] elements) {
        testWeightingInternal(elements);
    }

    @Fuzz
    public void testWeightingWithGenerator( @From(CacheRequestsGenerator.class) int @Size(max = MAX_SIZE) [] elements) {
        testWeightingInternal(elements);
    }


    private void testWeightingInternal(int[] elements) {
        LoadingCache cache = CacheBuilder.newBuilder()
                .concurrencyLevel(1)
                .maximumWeight(MAX_WEIGHT)
                .weigher((k, v) -> Math.abs((int) v))
                .removalListener(listener)
                .build(identityLoader);

        int sum = 0;
        int numEvicted = evictedElements.size();
        for (int e : elements) {
            // What's in the cache?
            Set existingKeys = cache.asMap().keySet();

            // Is this element in the cache?
            boolean cacheHit = existingKeys.contains(e);

            // Get new element and make sure its value is as expected
            assertEquals(e, cache.getUnchecked(e));

            // Did something get evicted?
            int newEvictions = evictedElements.size() - numEvicted;

            int w = Math.abs(e);

            if (cacheHit) {
                assertThat(newEvictions, is(0));
                // Cache should not contain elements larger than MAX
                assertThat(e, lessThanOrEqualTo(MAX_WEIGHT));
            } else {
                // If this element was too large, it doesn't go in at all
                if (w > MAX_WEIGHT) {
                    assertThat(newEvictions, greaterThan(0));
                    int lastEvictedKey = (int) evictedElements.get(evictedElements.size()-1);
                    assertEquals(e, lastEvictedKey);
                } else {
                    // Update weight
                    sum += w;
                    // If the weight is higher than max, then something got evicted; else not
                    if (sum > MAX_WEIGHT) {
                        assertThat(newEvictions, greaterThan(0));
                    } else {
                        // Eviction is not necessary, but possible here
                    }
                }

                // If something got evicted, update weight to remove it
                for (int i = numEvicted; i < numEvicted + newEvictions; i++) {
                    int key = (int) evictedElements.get(i);
                    sum -= Math.abs(key);

                }
            }

            // Remember how many elements have been evicted for next turn
            numEvicted = evictedElements.size();
        }
    }
}
