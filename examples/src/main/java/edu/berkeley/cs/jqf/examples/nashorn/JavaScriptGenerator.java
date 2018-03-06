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
import com.pholser.junit.quickcheck.internal.GeometricDistribution;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import edu.berkeley.cs.jqf.examples.common.AsciiStringGenerator;
import jdk.nashorn.internal.ir.*;
import jdk.nashorn.internal.parser.TokenType;
import jdk.nashorn.internal.runtime.Undefined;
import org.junit.Assume;

import static java.lang.Math.ceil;
import static java.lang.Math.log;

/**
 * @author Rohan Padhye
 */
public class JavaScriptGenerator extends Generator<String> {
    public JavaScriptGenerator() {
        super(String.class);
    }

    private GenerationStatus status;
    private GeometricDistribution geometric = new GeometricDistribution();


    private static final int MAX_IDENTIFIERS = 100;
    private static final int MAX_EXPRESSION_DEPTH = 10;
    private static final int MAX_STATEMENT_DEPTH = 4;
    private static final float NEW_IDENTIFIER_PROB = 0.1f;
    private static final Set<String> identifiers = new HashSet<>();

    private LexicalContext lc = null;

    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        this.status = status;
        return generateStatement(random).toString(false);
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
    private Expression generateExpression(SourceOfRandomness random) {
        expressionDepth++;
        // Choose between terminal or non-terminal
        Expression result;
        if (expressionDepth >= MAX_EXPRESSION_DEPTH || random.nextFloat() < 0.8) {
            result = random.choose(Arrays.<Function<SourceOfRandomness, Expression>>asList(
                    this::generateLiteralNode,
                    this::generateIdentNode
            )).apply(random);
        } else {
            result = random.choose(Arrays.<Function<SourceOfRandomness, Expression>>asList(
                    this::generateBinaryNode,
                    this::generateCallNode
            )).apply(random);
        }
        expressionDepth--;
        return result;
    }

    private int statementDepth = 0;
    private Statement generateStatement(SourceOfRandomness random) {
        statementDepth++;
        Statement result;
        if (random.nextBoolean()) {
            result = generateExpressionStatement(random);
        } else {
            if (statementDepth >= MAX_STATEMENT_DEPTH || random.nextFloat() < 0.8) {
                result = random.choose(Arrays.<Function<SourceOfRandomness, Statement>>asList(
                        this::generateExpressionStatement,
                        this::generateBreakNode,
                        this::generateContinueNode,
                        this::generateEmptyNode
                )).apply(random);
            } else {
                result = random.choose(Arrays.<Function<SourceOfRandomness, Statement>>asList(
                        this::generateForNode,
                        this::generateBlockStatement
                )).apply(random);
            }
        }
        statementDepth--;
        return result;
    }

    private AccessNode generateAccessNode(SourceOfRandomness random) {
        return null;
    }

    private BinaryNode generateBinaryNode(SourceOfRandomness random) {
        TokenType[] tokens = {
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
        TokenType token = random.choose(tokens);
        Expression lhs = (token == TokenType.OR || token == TokenType.AND) ?
                generateJoinPredecessorExpression(random) :
                generateExpression(random);
        Expression rhs = generateExpression(random);

        // Write an assumption to ensure assertion in BinaryNode() does not fail
        Assume.assumeTrue(lhs instanceof JoinPredecessorExpression ||
                (token != TokenType.OR && token != TokenType.AND));
        return new BinaryNode(token.ordinal(),lhs, rhs);
    }

    private Block generateBlock(SourceOfRandomness random) {
        return new Block(0, 0, generateItems(this::generateStatement, random, 4));
    }

    private BlockStatement generateBlockStatement(SourceOfRandomness random) {
        return new BlockStatement(-1, generateBlock(random));
    }

    private BreakNode generateBreakNode(SourceOfRandomness random) {
        return new BreakNode(-1, 0, 0, null);
    }

    private CallNode generateCallNode(SourceOfRandomness random) {
        return new CallNode(-1, 0, 0, generateExpression(random),
                generateItems(this::generateExpression, random, 3), random.nextBoolean());
    }

    private CaseNode generateCaseNode(SourceOfRandomness random) {
        return new CaseNode(0, 0, generateExpression(random), generateBlock(random));
    }

    private CatchNode generateCatchNode(SourceOfRandomness random) {
        return new CatchNode(-1, 0, 0,
                generateIdentNode(random), generateExpression(random),
                generateBlock(random), false);
    }

    private ContinueNode generateContinueNode(SourceOfRandomness random) {
        return new ContinueNode(-1, 0, 0, null);
    }

    private EmptyNode generateEmptyNode(SourceOfRandomness random) {
        return new EmptyNode(-1, 0, 0);
    }

    private ExpressionStatement generateExpressionStatement(SourceOfRandomness random) {
        return new ExpressionStatement(-1, 0, 0, generateExpression(random));
    }

    private ForNode generateForNode(SourceOfRandomness random) {
        // TODO: Generate flags for for-in/for-each
        ForNode forNode = new ForNode(-1, 0, 0, generateBlock(random), 0);
        if (random.nextBoolean()) {
            forNode = forNode.setInit(lc, generateExpression(random));
        }
        if (random.nextBoolean()) {
            forNode = forNode.setTest(lc, generateJoinPredecessorExpression(random));
        }
        if (random.nextBoolean()) {
            forNode = forNode.setModify(lc, generateJoinPredecessorExpression(random));
        }
        return forNode;
    }

    private FunctionNode generateFunctionNode(SourceOfRandomness random) {
        return null;
    }

    private GetSplitState generateGetSplitState(SourceOfRandomness random) {
        return null;
    }

    private IdentNode generateIdentNode(SourceOfRandomness random) {
        // Either generate a new identifier or use an existing one
        String identifier;
        if (identifiers.isEmpty() || (identifiers.size() < MAX_IDENTIFIERS && random.nextFloat() < NEW_IDENTIFIER_PROB)) {
            identifier = random.nextChar('a', 'z') + "_" + identifiers.size();
            identifiers.add(identifier);
        } else {
            identifier = random.choose(identifiers);
        }

        return new IdentNode(0, 0, identifier);
    }

    private IfNode generateIfNode(SourceOfRandomness random) {
        return new IfNode(-1, 0, 0,
                generateExpression(random),
                generateBlock(random),
                random.nextBoolean() ? generateBlock(random) : null);
    }

    private IndexNode generateIndexNode(SourceOfRandomness random) {
        return null;
    }

    private JoinPredecessorExpression generateJoinPredecessorExpression(SourceOfRandomness random) {
        return new JoinPredecessorExpression(generateExpression(random));
    }

    private JumpToInlinedFinally generateJumpToInlinedFinally(SourceOfRandomness random) {
        return null;
    }

    private LabelNode generateLabelNode(SourceOfRandomness random) {
        return null;
    }

    private LiteralNode generateLiteralNode(SourceOfRandomness random) {
        return random.choose(Arrays.<Supplier<LiteralNode>>asList(
                () -> LiteralNode.newInstance(0, 0, random.nextInt(-10, 1000)),
                () -> LiteralNode.newInstance(0, 0, random.nextBoolean()),
                () -> LiteralNode.newInstance(0, 0, new AsciiStringGenerator().generate(random, status)),
                () -> LiteralNode.newInstance(0, 0, Undefined.getUndefined()),
                () -> LiteralNode.newInstance(0, 0) /* null */
        )).get();
    }

    private Node generateNode(SourceOfRandomness random) {
        return null;
    }

    private ObjectNode generateObjectNode(SourceOfRandomness random) {
        return null;
    }

    private PropertyNode generatePropertyNode(SourceOfRandomness random) {
        return null;
    }

    private ReturnNode generateReturnNode(SourceOfRandomness random) {
        return null;
    }

    private RuntimeNode generateRuntimeNode(SourceOfRandomness random) {
        return null;
    }

    private SetSplitState generateSetSplitState(SourceOfRandomness random) {
        return null;
    }

    private SplitNode generateSplitNode(SourceOfRandomness random) {
        return null;
    }

    private SplitReturn generateSplitReturn(SourceOfRandomness random) {
        return null;
    }

    private SwitchNode generateSwitchNode(SourceOfRandomness random) {
        return null;
    }

    private TernaryNode generateTernaryNode(SourceOfRandomness random) {
        return null;
    }

    private ThrowNode generateThrowNode(SourceOfRandomness random) {
        return null;
    }

    private TryNode generateTryNode(SourceOfRandomness random) {
        return null;
    }

    private UnaryNode generateUnaryNode(SourceOfRandomness random) {
        return null;
    }

    private VarNode generateVarNode(SourceOfRandomness random) {
        return null;
    }

    private WhileNode generateWhileNode(SourceOfRandomness random) {
        return null;
    }

    private WithNode generateWithNode(SourceOfRandomness random) {
        return null;
    }
}