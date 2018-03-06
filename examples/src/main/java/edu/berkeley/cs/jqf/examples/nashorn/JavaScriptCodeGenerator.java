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
package edu.berkeley.cs.jqf.examples.nashorn;

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
import jdk.nashorn.internal.parser.TokenType;

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
    private static final int MAX_STATEMENT_DEPTH = 4;
    private static final float NEW_IDENTIFIER_PROB = 0.1f;
    private static Set<String> identifiers;


    private static final TokenType[] UNARY_TOKENS = {
            TokenType.NOT,
            TokenType.INCPREFIX,
            TokenType.DECPREFIX,
            TokenType.BIT_NOT,
            TokenType.DELETE,
            TokenType.NEW,
            TokenType.TYPEOF
    };

    private static final TokenType[] BINARY_TOKENS = {
            TokenType.NE,
            TokenType.NE_STRICT,
            TokenType.MOD,
            TokenType.ASSIGN_MOD,
            TokenType.BIT_AND,
            TokenType.AND,
            TokenType.ASSIGN_BIT_AND,
            TokenType.MUL,
            TokenType.ASSIGN_MUL,
            TokenType.ADD,
            TokenType.ASSIGN_ADD,
            TokenType.COMMARIGHT,
            TokenType.SUB,
            TokenType.ASSIGN_SUB,
            TokenType.DIV,
            TokenType.ASSIGN_DIV,
            TokenType.COLON,
            TokenType.SEMICOLON,
            TokenType.LT,
            TokenType.SHL,
            TokenType.ASSIGN_SHL,
            TokenType.LE,
            TokenType.ASSIGN,
            TokenType.EQ,
            TokenType.EQ_STRICT,
            TokenType.BIND,
            TokenType.GT,
            TokenType.GE,
            TokenType.SAR,
            TokenType.ASSIGN_SAR,
            TokenType.SHR,
            TokenType.ASSIGN_SHR,
            TokenType.TERNARY,
            TokenType.BIT_XOR,
            TokenType.ASSIGN_BIT_XOR,
            TokenType.BIT_OR,
            TokenType.ASSIGN_BIT_OR,
            TokenType.OR,
            TokenType.IN,
            TokenType.INSTANCEOF
    };

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        this.status = status;
        this.identifiers = new HashSet<>();
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

    private int expressionDepth = 0;
    private String generateExpression(SourceOfRandomness random) {
        expressionDepth++;
        // Choose between terminal or non-terminal
        String result;
        if (expressionDepth >= MAX_EXPRESSION_DEPTH || random.nextFloat() < 0.6) {
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
                    this::generateIndexNode
            )).apply(random);
        }
        expressionDepth--;
        return "(" + result + ")";
    }

    private int statementDepth = 0;
    private String generateStatement(SourceOfRandomness random) {
        statementDepth++;
        String result;
        if (random.nextBoolean()) {
            result = generateExpressionStatement(random);
        } else {
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
        }
        statementDepth--;
        return result;
    }


    private String generateBinaryNode(SourceOfRandomness random) {
        TokenType token = random.choose(BINARY_TOKENS);
        String lhs = generateExpression(random);
        String rhs = generateExpression(random);

        return lhs + " " + token.getName() + " " + rhs;
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
        return "function(" + generateItems(this::generateIdentNode, random, 5) + ")" + generateBlock(random);
    }

    private String generateNamedFunctionNode(SourceOfRandomness random) {
        return "function " + generateIdentNode(random) + "(" + generateItems(this::generateIdentNode, random, 5) + ")" + generateBlock(random);
    }


    private String generateIdentNode(SourceOfRandomness random) {
        // Either generate a new identifier or use an existing one
        String identifier;
        if (identifiers.isEmpty() || (identifiers.size() < MAX_IDENTIFIERS && random.nextFloat() < NEW_IDENTIFIER_PROB)) {
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
                (random.nextBoolean() ? generateBlock(random) : "");
    }

    private String generateIndexNode(SourceOfRandomness random) {
        return generateExpression(random) + "[" + generateExpression(random) + "]";
    }

    private String generateLiteralNode(SourceOfRandomness random) {
        return random.choose(Arrays.<Supplier<String>>asList(
                () -> String.valueOf(random.nextInt(-10, 1000)),
                () -> String.valueOf(random.nextBoolean()),
                () -> new AsciiStringGenerator().generate(random, status),
                () -> "undefined",
                () -> "null"
        )).get();
    }

    private String generateObjectNode(SourceOfRandomness random) {
        return null;
    }

    private String generatePropertyNode(SourceOfRandomness random) {
        return generateExpression(random) + "." + generateIdentNode(random);
    }

    private String generateReturnNode(SourceOfRandomness random) {
        return random.nextBoolean() ? "return" : "return " + generateExpression(random);
    }

    private String generateSwitchNode(SourceOfRandomness random) {
        return "switch(" + generateExpression(random) + ") {"
                + generateItems(this::generateCaseNode, random, 2) + "}";
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
        TokenType token = random.choose(UNARY_TOKENS);
        return token.getName() + " " + generateExpression(random);
    }

    private String generateVarNode(SourceOfRandomness random) {
        return "var " + generateIdentNode(random);
    }

    private String generateWhileNode(SourceOfRandomness random) {
        return "while (" + generateExpression(random) + ")" + generateBlock(random);
    }

    private String generateWithNode(SourceOfRandomness random) {
        return null;
    }
}