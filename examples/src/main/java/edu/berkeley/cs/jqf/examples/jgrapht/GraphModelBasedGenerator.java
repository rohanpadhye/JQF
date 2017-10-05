/*
 * Copyright (c) 2017, University of California, Berkeley
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

import java.util.Random;

import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.jgrapht.Graph;
import org.jgrapht.VertexFactory;
import org.jgrapht.generate.CompleteGraphGenerator;
import org.jgrapht.generate.EmptyGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;

/**
 *
 * Utility class for generating graphs based on specified models.
 *
 * @author Rohan Padhye
 */
public class GraphModelBasedGenerator {

    private GraphModelBasedGenerator() {
        // Static only
    }

    private static<E extends DefaultEdge> GraphGenerator<Integer, E, Integer>
        getModel(GraphModel model, SourceOfRandomness randomSource,
                 boolean loops, boolean multipleEdges) {

        if (model == null) {
            throw new IllegalArgumentException("Graph generators MUST be configured with @GraphModel");
        }

        Random random = randomSource.toJDKRandom();

        if (model.nodes() <= 0) {
            throw new IllegalArgumentException("nodes must be > 0");
        }

        if (model.edges() > 0) {
            return new FastGnmRandomGraphGenerator<>(model.nodes(), model.edges(), random, loops);
        }

        if (multipleEdges) {
            throw new IllegalArgumentException("Multi-graphs must specify edges and use GNM model");
        }

        if (model.p() < 0.0 || model.p() > 1.0) {
            throw new IllegalArgumentException("p must be in [0, 1]");
        }

        if (model.p() == 0.0) {
            return new EmptyGraphGenerator<>(model.nodes());
        } else if (model.p() == 1.0) {
            return new CompleteGraphGenerator<>(model.nodes());
        } else if (loops == false && model.p() == 0.5) {
            return new BitSetBasedSimpleGraphGenerator<>(model.nodes(), random);
        } else {
            return new GnpRandomGraphGenerator<>(model.nodes(), model.p(), random, loops);
        }
    }

    private static VertexFactory<Integer> createNodeFactory() {
        return new VertexFactory<Integer>() {
            int nodeId = 1;

            @Override
            public Integer createVertex() {
                return this.nodeId++;
            }
        };
    }

    public static<E extends DefaultEdge> void generateGraph(Graph<Integer, E> graph,
                                                            GraphModel size, SourceOfRandomness randomSource,
                                                            boolean loops, boolean multipleEdges) {
        GraphModelBasedGenerator.<E>getModel(size, randomSource, loops, multipleEdges)
                .generateGraph(graph, createNodeFactory(), null);
    }
}
