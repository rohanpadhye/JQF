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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.examples.common.AsciiStringGenerator;

import static java.lang.Math.ceil;
import static java.lang.Math.log;

/**
 * @author Rohan Padhye
 */
public class JavaScriptCodeGenerator extends Generator<String> {
    public JavaScriptCodeGenerator() {
        super(String.class);
    }

    private GenerationStatus status;

    private static final int MAX_IDENTIFIERS = 100;
    private static final int MAX_EXPRESSION_DEPTH = 10;
    private static final int MAX_STATEMENT_DEPTH = 6;
    private static Set<String> identifiers;
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
        this.identifiers = new HashSet<>();
        this.statementDepth = 0;
        this.expressionDepth = 0;
        return generateStatement(random).toString();
    }

    private static int sampleGeometric(SourceOfRandomness random, double mean) {
        double p = 1 / mean;
        double uniform = random.nextDouble();
        return (int) ceil(log(1 - uniform) / log(1 - p));
    }

    private static <T> List<T> generateItems(Function<SourceOfRandomness, T> generator, SourceOfRandomness random,
                                             double mean) {
        int len = sampleGeometric(random, mean);
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
        if (expressionDepth >= MAX_EXPRESSION_DEPTH || random.nextBoolean()) {
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
        if (statementDepth >= MAX_STATEMENT_DEPTH || random.nextBoolean()) {
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
        return "{ " + String.join(";", generateItems(this::generateStatement, random, 4)) + " }";
    }

    private String generateBlockStatement(SourceOfRandomness random) {
        return generateBlock(random);
    }

    private String generateBreakNode(SourceOfRandomness random) {
        return "break";
    }

    private String generateCallNode(SourceOfRandomness random) {
        String func = generateExpression(random);
        String args = String.join(",", generateItems(this::generateExpression, random, 3));

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
        return "function(" + String.join(", ", generateItems(this::generateIdentNode, random, 5)) + ")" + generateBlock(random);
    }

    private String generateNamedFunctionNode(SourceOfRandomness random) {
        return "function " + generateIdentNode(random) + "(" + String.join(", ", generateItems(this::generateIdentNode, random, 5)) + ")" + generateBlock(random);
    }

    private String generateArrowFunctionNode(SourceOfRandomness random) {
        String params = "(" + String.join(", ", generateItems(this::generateIdentNode, random, 3)) + ")";
        if (random.nextBoolean()) {
            return params + " => " + generateBlock(random);
        } else {
            return params + " => " + generateExpression(random);
        }

    }

    private String generateIdentNode(SourceOfRandomness random) {
        // Either generate a new identifier or use an existing one
        String identifier;
        if (identifiers.isEmpty() || (identifiers.size() < MAX_IDENTIFIERS && random.nextBoolean())) {
            identifier = random.nextChar('a', 'z') + "_" + identifiers.size();
            identifiers.add(identifier);
        } else {
            identifier = random.choose(identifiers);
        }

        return identifier;
    }

    private String generateIfNode(SourceOfRandomness random) {
        return "if (" +
                generateExpression(random) + ") " +
                generateBlock(random) +
                (random.nextBoolean() ? " else " + generateBlock(random) : "");
    }

    private String generateIndexNode(SourceOfRandomness random) {
        return generateExpression(random) + "[" + generateExpression(random) + "]";
    }

    private String generateObjectProperty(SourceOfRandomness random) {
        return generateIdentNode(random) + ": " + generateExpression(random);
    }

    private String generateLiteralNode(SourceOfRandomness random) {
        if (expressionDepth < MAX_EXPRESSION_DEPTH && random.nextBoolean()) {
            if (random.nextBoolean()) {
                // Array literal
                return "[" + String.join(", ", generateItems(this::generateExpression, random, 3)) + "]";
            } else {
                // Object literal
                return "{" + String.join(", ", generateItems(this::generateObjectProperty, random, 3)) + "}";

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
                + String.join(" ", generateItems(this::generateCaseNode, random, 2)) + "}";
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
