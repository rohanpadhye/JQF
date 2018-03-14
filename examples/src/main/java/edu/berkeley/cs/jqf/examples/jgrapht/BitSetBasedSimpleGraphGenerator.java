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
package edu.berkeley.cs.jqf.examples.jgrapht;

import java.util.BitSet;
import java.util.Map;
import java.util.Random;

import org.jgrapht.DirectedGraph;
import org.jgrapht.Graph;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.GraphGenerator;

/**
 * @author Rohan Padhye
 */
public class BitSetBasedSimpleGraphGenerator<V, E> implements GraphGenerator<V, E, V> {

    protected final int n;
    private final Random rng;

    public BitSetBasedSimpleGraphGenerator(int nodes, Random rng) {
        if (nodes < 1) {
            throw new IllegalArgumentException("At least one node required");
        }
        this.n = nodes;
        this.rng = rng;
    }

    private int numEdges(boolean isDirected) {
        if (isDirected) {
            return n * (n - 1);
        } else {
            return (n * (n - 1))/2;
        }
    }

    @Override
    public void generateGraph(Graph<V, E> graph, VertexFactory<V> vertexFactory, Map<String, V> map) {
        // Calculate number of edges to generate
        boolean isDirected = graph instanceof DirectedGraph;
        int numEdges = numEdges(isDirected);

        // Figure out how many random bytes to generate for this purpose
        int numBytes = (numEdges + 7) / 8;  // Equal to ceil(numEdges/8.0)
        byte[] bytes = new byte[numBytes];

        // Generate random bytes
        rng.nextBytes(bytes);

        // Convert to bitset
        BitSet bitSet = BitSet.valueOf(bytes);

        // Generate nodes
        V[] vertices = (V[]) new Object[n];
        for(int i = 0; i < n; ++i) {
            V v = vertexFactory.createVertex();
            graph.addVertex(v);
            vertices[i] = v;
        }

        // Add edges as necessary
        int k = 0;
        for (int i = 0; i < n; i++) {
            V s = vertices[i];
            for (int j = 0; j < i; j++) {
                V t = vertices[j];
                // Get next boolean to decide s --> t
                if (bitSet.get(k++)) {
                    graph.addEdge(s, t);
                }

                if (isDirected) {
                    // Get next boolean to decide t --> t
                    if (bitSet.get(k++)) {
                        graph.addEdge(t, s);
                    }
                }
            }
        }
        // Sanity check
        assert(k == numEdges);




    }
}
