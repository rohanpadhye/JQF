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
package edu.berkeley.cs.jqf.examples.js;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.examples.common.AsciiStringGenerator;
import edu.berkeley.cs.jqf.fuzz.guidance.StreamBackedRandom;
import edu.berkeley.cs.jqf.fuzz.junit.quickcheck.FastSourceOfRandomness;

import static java.lang.Math.ceil;
import static java.lang.Math.log;
import static org.junit.Assume.assumeTrue;

/**
 * @author Rohan Padhye
 */
public class JavaScriptIterativeCodeGenerator extends Generator<String> {
    public JavaScriptIterativeCodeGenerator() {
        super(String.class);

        // Read parameters from system properties
        maxIdentifiers = Integer.getInteger(identifierProp, 0);
        maxItems = Integer.getInteger(itemsProp, 0);
        maxDepth = Integer.getInteger(depthProp, 0);
        assert(maxIdentifiers > 0);
        assert(maxItems > 0);
        assert(maxDepth > 0);

        // Create set of fixed identifiers
    }

    private GenerationStatus status;


    private final String identifierProp = "maxIdentifiers";
    private final String itemsProp = "maxItems";
    private final String depthProp = "maxDepth";
    private static int maxIdentifiers;
    private static int maxItems;
    private static int maxDepth;
    private static int maxBound;
    private static List<String> identifiers; // Stores generated IDs, to promote re-use
    private int statementDepth;
    private int expressionDepth;


    private static final String[] UNARY_TOKENS = {
            "!", "++", "--", "~",
            "delete", "new", "typeof"
    };

    private static final String[] BINARY_TOKENS = {
            "!=", "!==", "%", "%=", "&", "&&", "&=", "*", "*=", "+", "+=", ",",
            "-", "-=", "/", "/=", "<", "<<", ">>=", "<=", "=", "==", "===",
            ">", ">=", ">>", ">>=", ">>>", ">>>=", "^", "^=", "|", "|=", "||",
            "in", "instanceof"
    };

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        this.status = status;
        this.statementDepth = 0;
        this.expressionDepth = 0;
        this.maxBound = 4;
        generateIdentifiers(maxBound);

        SourceOfRandomness r = new FastSourceOfRandomness((StreamBackedRandom) random.toJDKRandom()) {
            public int nextInt(int minimum, int maximum) {
                int randInt = random.nextInt(minimum, maxBound);
                assumeTrue(randInt < maximum);
                return randInt;
            }
        };
        return generateStatement(r).toString();
    }

    private static <T> List<T> generateItems(Function<SourceOfRandomness, T> generator, SourceOfRandomness random, int minimum) {
        int len = random.nextInt(minimum, maxItems + 1);
        List<T> items = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            items.add(generator.apply(random));
        }
        return items;
    }

    private String generateExpression(SourceOfRandomness random) {
        expressionDepth++;
        // Choose between terminal or non-terminal
        String result;
        int randDepth = random.nextInt(0, maxDepth + 1);
        if (expressionDepth >= randDepth) {
            result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                    this::generateLiteralNode,
                    this::generateIdentNode
            )).apply(random);
        } else {
            result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                    this::generateBinaryNode,
                    this::generateUnaryNode,
                    this::generateTernaryNode,
                    this::generateCallNode,
                    this::generateFunctionNode,
                    this::generatePropertyNode,
                    this::generateIndexNode,
                    this::generateArrowFunctionNode
            )).apply(random);
        }
        expressionDepth--;
        return "(" + result + ")";
    }

    private String generateStatement(SourceOfRandomness random) {
        statementDepth++;
        String result;
        int randDepth = random.nextInt(0, maxDepth + 1);
        if (statementDepth >= randDepth) {
            result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                    this::generateExpressionStatement,
                    this::generateBreakNode,
                    this::generateContinueNode,
                    this::generateReturnNode,
                    this::generateThrowNode,
                    this::generateVarNode,
                    this::generateEmptyNode
            )).apply(random);
        } else {
            result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                    this::generateIfNode,
                    this::generateForNode,
                    this::generateWhileNode,
                    this::generateNamedFunctionNode,
                    this::generateSwitchNode,
                    this::generateTryNode,
                    this::generateBlockStatement
            )).apply(random);
        }
        statementDepth--;
        return result;
    }


    private String generateBinaryNode(SourceOfRandomness random) {
        String token = random.choose(BINARY_TOKENS);
        String lhs = generateExpression(random);
        String rhs = generateExpression(random);

        return lhs + " " + token + " " + rhs;
    }

    private String generateBlock(SourceOfRandomness random) {
        return "{ " + String.join(";", generateItems(this::generateStatement, random, 0)) + " }";
    }

    private String generateBlockStatement(SourceOfRandomness random) {
        return generateBlock(random);
    }

    private String generateBreakNode(SourceOfRandomness random) {
        return "break";
    }

    private String generateCallNode(SourceOfRandomness random) {
        String func = generateExpression(random);
        String args = String.join(",", generateItems(this::generateExpression, random, 0));

        String call = func + "(" + args + ")";
        if (random.nextBoolean()) {
            return call;
        } else {
            return "new " + call;
        }
    }

    private String generateCaseNode(SourceOfRandomness random) {
        return "case " + generateExpression(random) + ": " +  generateBlock(random);
    }

    private String generateCatchNode(SourceOfRandomness random) {
        return "catch (" + generateIdentNode(random) + ") " +
                generateBlock(random);
    }

    private String generateContinueNode(SourceOfRandomness random) {
        return "continue";
    }

    private String generateEmptyNode(SourceOfRandomness random) {
        return "";
    }

    private String generateExpressionStatement(SourceOfRandomness random) {
        return generateExpression(random);
    }

    private String generateForNode(SourceOfRandomness random) {
        String s = "for(";
        if (random.nextBoolean()) {
            s += generateExpression(random);
        }
        s += ";";
        if (random.nextBoolean()) {
            s += generateExpression(random);
        }
        s += ";";
        if (random.nextBoolean()) {
            s += generateExpression(random);
        }
        s += ")";
        s += generateBlock(random);
        return s;
    }

    private String generateFunctionNode(SourceOfRandomness random) {
        return "function(" + String.join(", ", generateItems(this::generateIdentNode, random, 0)) + ")" + generateBlock(random);
    }

    private String generateNamedFunctionNode(SourceOfRandomness random) {
        return "function " + generateIdentNode(random) + "(" + String.join(", ", generateItems(this::generateIdentNode, random, 0)) + ")" + generateBlock(random);
    }

    private String generateArrowFunctionNode(SourceOfRandomness random) {
        String params = "(" + String.join(", ", generateItems(this::generateIdentNode, random, 0)) + ")";
        if (random.nextBoolean()) {
            return params + " => " + generateBlock(random);
        } else {
            return params + " => " + generateExpression(random);
        }

    }

    /** Creates initial set of identifiers depending on parameter */
    private void generateIdentifiers(int numIdentifiers) {
        this.identifiers = new ArrayList<>();
        String ident;
        for (int i = 0; i < numIdentifiers; i++) {
            ident = "a" + i;
            identifiers.add(ident);
        }
    }

    private String generateIdentNode(SourceOfRandomness random) {

        int randIndex = random.nextInt(0, maxIdentifiers);
        String identifier = identifiers.get(randIndex);

        return identifier;
    }

    private String generateIfNode(SourceOfRandomness random) {
        return "if (" +
                generateExpression(random) + ") " +
                generateBlock(random) +
                (random.nextBoolean() ? generateBlock(random) : "");
    }

    private String generateIndexNode(SourceOfRandomness random) {
        return generateExpression(random) + "[" + generateExpression(random) + "]";
    }

    private String generateObjectProperty(SourceOfRandomness random) {
        return generateIdentNode(random) + ": " + generateExpression(random);
    }

    private String generateLiteralNode(SourceOfRandomness random) {
        int randDepth = random.nextInt(0, maxDepth + 1);
        if (expressionDepth < randDepth) {
            if (random.nextBoolean()) {
                // Array literal
                return "[" + String.join(", ", generateItems(this::generateExpression, random, 0)) + "]";
            } else {
                // Object literal
                return "{" + String.join(", ", generateItems(this::generateObjectProperty, random, 0)) + "}";

            }
        } else {
            return random.choose(Arrays.<Supplier<String>>asList(
                    () -> String.valueOf(random.nextInt(-10, 1000)),
                    () -> String.valueOf(random.nextBoolean()),
                    () -> '"' + new AsciiStringGenerator().generate(random, status) + '"',
                    () -> "undefined",
                    () -> "null",
                    () -> "this"
            )).get();
        }
    }

    private String generatePropertyNode(SourceOfRandomness random) {
        return generateExpression(random) + "." + generateIdentNode(random);
    }

    private String generateReturnNode(SourceOfRandomness random) {
        return random.nextBoolean() ? "return" : "return " + generateExpression(random);
    }

    private String generateSwitchNode(SourceOfRandomness random) {
        return "switch(" + generateExpression(random) + ") {"
                + String.join(" ", generateItems(this::generateCaseNode, random, 0)) + "}";
    }

    private String generateTernaryNode(SourceOfRandomness random) {
        return generateExpression(random) + " ? " + generateExpression(random) +
                " : " + generateExpression(random);
    }

    private String generateThrowNode(SourceOfRandomness random) {
        return "throw " + generateExpression(random);
    }

    private String generateTryNode(SourceOfRandomness random) {
        return "try " + generateBlock(random) + generateCatchNode(random);
    }

    private String generateUnaryNode(SourceOfRandomness random) {
        String token = random.choose(UNARY_TOKENS);
        return token + " " + generateExpression(random);
    }

    private String generateVarNode(SourceOfRandomness random) {
        return "var " + generateIdentNode(random);
    }

    private String generateWhileNode(SourceOfRandomness random) {
        return "while (" + generateExpression(random) + ")" + generateBlock(random);
    }
}
