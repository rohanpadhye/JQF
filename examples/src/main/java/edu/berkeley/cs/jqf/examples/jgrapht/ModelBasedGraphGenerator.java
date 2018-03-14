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

import java.util.Random;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.jgrapht.Graph;
import org.jgrapht.VertexFactory;
import org.jgrapht.WeightedGraph;
import org.jgrapht.generate.CompleteGraphGenerator;
import org.jgrapht.generate.EmptyGraphGenerator;
import org.jgrapht.generate.GnmRandomGraphGenerator;
import org.jgrapht.generate.GnpRandomGraphGenerator;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.DirectedPseudograph;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.jgrapht.graph.DirectedWeightedPseudograph;
import org.jgrapht.graph.Multigraph;
import org.jgrapht.graph.Pseudograph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.graph.SimpleDirectedWeightedGraph;
import org.jgrapht.graph.SimpleGraph;
import org.jgrapht.graph.SimpleWeightedGraph;
import org.jgrapht.graph.WeightedMultigraph;
import org.jgrapht.graph.WeightedPseudograph;

/**
 * Quick-check generator for JGraphT graphs using the
 * <tt>@GraphModel</tt> annotation.
 *
 * @author Rohan Padhye
 */
public class ModelBasedGraphGenerator extends Generator<Graph> {

    private GraphModel model;
    private boolean directed = false;

    public ModelBasedGraphGenerator() {
        super(Graph.class);
    }

    public void configure(GraphModel model) {
        this.model = model;
    }

    public void setDirected(boolean directed) {
        this.directed = directed;
    }

    public boolean isDirected() {
        return directed;
    }

    @Override
    public Graph generate(SourceOfRandomness sourceOfRandomness, GenerationStatus generationStatus) {
        // Ensure that a model exists
        if (model == null) {
            throw new IllegalArgumentException("Graph generators MUST be configured with @GraphModel");
        }

        // Create graph instance
        Graph<Integer, DefaultEdge> graph = createGraph();

        // Generate nodes and edges using JGraphT generators
        getModel(model, sourceOfRandomness)
                .generateGraph(graph, createNodeFactory(), null);

        // If weighted, generate edge weights
        if (model.weighted()) {
            WeightedGraph<Integer, DefaultEdge> wgraph = (WeightedGraph<Integer, DefaultEdge>) graph;
            // Set edges
            for (DefaultEdge e : wgraph.edgeSet()) {
                wgraph.setEdgeWeight(e, sourceOfRandomness.nextFloat());
            }
        }

        // Return handle to this graph
        return graph;
    }



    private Graph<Integer, DefaultEdge> createGraph() {
        Class<? extends DefaultEdge> edgeClass =
                model.weighted() ? DefaultWeightedEdge.class : DefaultEdge.class;

        if (model.loops()) {
            if (model.multiGraph() == false) {
                throw new IllegalArgumentException("Self-loops are only supported " +
                        "with multi-graphs");
            }
            if (isDirected()) {
                if (model.weighted()) {
                    return new DirectedWeightedPseudograph<>(edgeClass);
                } else {
                    return new DirectedPseudograph<>(edgeClass);
                }
            } else {
                if (model.weighted()) {
                    return new WeightedPseudograph<>(edgeClass);
                } else {
                    return new Pseudograph<>(edgeClass);
                }
            }
        } else {
            if (model.multiGraph()) {
                if (isDirected()) {
                    if (model.weighted()) {
                        return new DirectedWeightedMultigraph<>(edgeClass);
                    } else {
                        return new DirectedMultigraph<>(edgeClass);
                    }
                } else {
                    if (model.weighted()) {
                        return new WeightedMultigraph<>(edgeClass);
                    } else {
                        return new Multigraph<>(edgeClass);
                    }
                }
            } else {
                if (isDirected()) {
                    if (model.weighted()) {
                        return new SimpleDirectedWeightedGraph<>(edgeClass);
                    } else {
                        return new SimpleDirectedGraph<>(edgeClass);
                    }
                } else {
                    if (model.weighted()) {
                        return new SimpleWeightedGraph<>(edgeClass);
                    } else {
                        return new SimpleGraph<>(edgeClass);
                    }
                }
            }
        }

    }

    private static<E extends DefaultEdge> GraphGenerator<Integer, E, Integer>
        getModel(GraphModel model, SourceOfRandomness randomSource) {


        Random random = randomSource.toJDKRandom();

        if (model.nodes() <= 0) {
            throw new IllegalArgumentException("nodes must be > 0");
        }

        if (model.edges() > 0) {
            if (model.multiGraph() == false) {
                return new FastGnmRandomGraphGenerator<>(model.nodes(), model.edges(),
                        random, model.loops());
            } else {
                return new GnmRandomGraphGenerator<>(model.nodes(), model.edges(),
                        random, model.loops(), model.multiGraph());
            }
        }

        if (model.multiGraph()) {
            throw new IllegalArgumentException("Multi-graphs must specify edges and use GNM model");
        }

        if (model.p() < 0.0 || model.p() > 1.0) {
            throw new IllegalArgumentException("p must be in [0, 1]");
        }

        if (model.p() == 0.0) {
            return new EmptyGraphGenerator<>(model.nodes());
        } else if (model.p() == 1.0) {
            return new CompleteGraphGenerator<>(model.nodes());
        } else if (model.loops() == false && model.p() == 0.5) {
            return new BitSetBasedSimpleGraphGenerator<>(model.nodes(), random);
        } else {
            return new GnpRandomGraphGenerator<>(model.nodes(), model.p(),
                    random, model.loops());
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


}
