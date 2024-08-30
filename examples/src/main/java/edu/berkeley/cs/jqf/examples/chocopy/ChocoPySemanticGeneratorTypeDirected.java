package edu.berkeley.cs.jqf.examples.chocopy;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.apache.commons.lang3.StringUtils;


import java.util.*;
import java.util.function.Function;
import org.apache.commons.lang3.tuple.Pair;
import chocopy.common.astnodes.ClassType;
import chocopy.common.analysis.types.*;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/* Generates random strings that are syntactically valid ChocoPy */
public class ChocoPySemanticGeneratorTypeDirected extends Generator<String> {
    public ChocoPySemanticGeneratorTypeDirected() {
        super(String.class); // Register type of generated object

        // Read parameters from system properties
        maxIdentifiers = Integer.getInteger(identifierProp, 3);
        maxItems = Integer.getInteger(itemsProp, 3);
        maxDepth = Integer.getInteger(depthProp, 3);
        maxBound = 4;
        assert(maxIdentifiers > 0);
        assert(maxItems > 0);
        assert(maxDepth > 0);

        // Create set of fixed identifiers
        generateIdentifiers(maxBound);
    }

    private final String identifierProp = "maxIdentifiers";
    private final String itemsProp = "maxItems";
    private final String depthProp = "maxDepth";
    private GenerationStatus status; // saved state object when generating
    private static List<String> identifiers; // Stores generated IDs, to promote re-use
    private static List<String> funcIdentifiers; // Stores generated IDs, to promote re-use
    private static List<ValueType> allTypes; // Keeps track of all types
    private static List<ValueType> classTypes; // Keeps track of all class types
    private static Map<String, Type> varTypes; // Keeps track of variables and their types
    private static Map<String, FuncType> funcTypes; // Keeps track of functions and their return types
    private static int maxIdentifiers;
    private static int maxItems;
    private static int maxDepth;
    private static int maxBound;
    private int identCounter;
    private int funcIdentCounter;
    private int statementDepth; // Keeps track of how deep the AST is at any point
    private int declarationDepth; // Keeps track of how deep the AST is at any point
    private int expressionDepth; // Keeps track of how nested an expression is at any point
    private int indentLevel; // Keeps track of indentation level

    private static final String[] INT_BINARY_OP_TOKENS = {
            "+", "-", "*", "//", "%",
    };

    private static final String[] BOOL_BINARY_OP_TOKENS = {
            "and", "or", "==", "!=", "is",
            "<", "<=", ">", ">=", "==", "!=", "is"
    };

    private static final String[] BINARY_BOOL_TOKENS = {
            "and", "or"
    };

    private static final String[] INT_LITERALS = {
            "0", "1"
    };

    private static final String[] STRING_LITERALS = {
            "\"a\"", "\"\""
    };

    private static final String[] BOOL_LITERALS = {
            "True", "False"
    };

    private static final Type[] PRIMITIVE_TYPES = {
            Type.INT_TYPE,
            Type.STR_TYPE,
            Type.BOOL_TYPE,
    };

    private static final ValueType[] BASE_TYPES = {
            Type.INT_TYPE,
            Type.STR_TYPE,
            Type.BOOL_TYPE,
            Type.OBJECT_TYPE,

    };

    private static final String INDENT_TOKEN = "    "; // 4 spaces


    /** Main entry point. Called once per test case. Returns a random ChocoPy program. */
    @Override
    public String generate(SourceOfRandomness random, GenerationStatus status) {
        this.status = status; // we save this so that we can pass it on to other generators
        this.declarationDepth = 0;
        this.statementDepth = 0;
        this.expressionDepth = 0;
        this.indentLevel = 0;
        this.classTypes = new ArrayList<>();
        this.allTypes = Arrays.asList(BASE_TYPES);
        this.varTypes = new HashMap<>();
        this.funcTypes = new HashMap<>();
        this.identCounter = 0;
        this.funcIdentCounter = 0;
        return generateProgram(random);
    }

    /** Utility method for generating a random list of items (e.g. statements, arguments, attributes) */
    private static List<String> generateItems(Function<SourceOfRandomness, String> genMethod, SourceOfRandomness random, int minimum) {
        int len = nextIntBound(random, minimum, maxBound, maxItems);
        List<String> items = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            items.add(genMethod.apply(random));
        }
        return items;
    }

    /** Utility method for generating a random list of items from a list of functions to choose from */
    private static List<String> generateItemsMultipleMethods(List<Function<SourceOfRandomness, String>> genMethods, SourceOfRandomness random, int minimum) {
        int len = nextIntBound(random, minimum, maxBound, maxItems);
        List<String> items = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            items.add(random.choose(genMethods).apply(random));
        }
        return items;
    }

    private static int nextIntBound(SourceOfRandomness random, int minimum, int maximum, int maxParam) {
        int randInt = random.nextInt(minimum, maximum);
        assumeTrue(randInt <= maxParam);
        return randInt;
    }

    /** Generates a random ChocoPy program of classes, declarations, and statements */
    private String generateProgram(SourceOfRandomness random) {
        String declarations = String.join("", generateItemsMultipleMethods(Arrays.asList(
//                this::generateClassDef,
                this::generateFuncDef,
                this::generateVarDef
        ), random, 0));
        String statements = generateBlock(random, 0);
        return declarations + statements;
    }

    /** Generates a random ChocoPy declaration */
    private String generateDeclaration(SourceOfRandomness random) {
        String result = StringUtils.repeat(INDENT_TOKEN, indentLevel);
        int randDepth = nextIntBound(random, 0, maxBound, maxDepth);
        if (declarationDepth >= randDepth) {
            // Choose a random private method from this class, and then call it with `random`
            result += generateVarDef(random);
        } else {
            // If depth is low and we won the flip, then generate compound declarations
            // (that is, declarations that contain other declarations)
            result += random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                    this::generateVarDef
//                    this::generateFuncDef
            )).apply(random);
        }
        return result;
    }

    /** Generates a random ChocoPy function declaration */
    private String generateFuncDeclaration(SourceOfRandomness random) {
        String result = StringUtils.repeat(INDENT_TOKEN, indentLevel);
        int randDepth = nextIntBound(random, 0, maxBound, maxDepth);
        if (declarationDepth >= randDepth) {
            // Choose a random private method from this class, and then call it with `random`
            result += random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                    this::generateVarDef
//                    this::generateNonlocalDecl,
//                    this::generateGlobalDecl
            )).apply(random);
        } else {
            // If depth is low and we won the flip, then generate compound declarations
            // (that is, declarations that contain other declarations)
            result += random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                    this::generateVarDef,
                    this::generateFuncDef
            )).apply(random);
        }
        return result + "\n";
    }

    /** Generates a random ChocoPy statement */
    private String generateStatement(SourceOfRandomness random) {
        String result = StringUtils.repeat(INDENT_TOKEN, indentLevel);
        // If depth is too high, then generate only simple statements to prevent infinite recursion
        // If not, generate simple statements after the flip of a coin
        int randDepth = nextIntBound(random, 0, maxBound, maxDepth);
        if (statementDepth >= randDepth) {
            // Choose a random private method from this class, and then call it with `random`
            result += random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                    this::generateAssignStmt,
                    this::generatePassStmt,
                    this::generateExpressionStmt
            )).apply(random);
        } else {
            // If depth is low and we won the flip, then generate compound statements
            // (that is, statements that contain other statements)
            result += random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                    this::generateIfStmt,
                    this::generateForStmt,
                    this::generateWhileStmt
            )).apply(random);
        }
        return result + "\n";
    }

    private String generateVarOfType(SourceOfRandomness random, Type type) {
        List<String> candidateVars = new ArrayList<>();
        for (Map.Entry<String, Type> entry : varTypes.entrySet()) {
            if (entry.getValue().equals(type)) {
                candidateVars.add(entry.getKey());
            }
        }
        assumeTrue(!candidateVars.isEmpty());
        return random.choose(candidateVars);
    }

    private String generateParenExprOfType(SourceOfRandomness random, Type type) {
        return "(" + generateExpressionOfType(random, type) + ")";
    }

    // TODO: implement
    private String generateMemberExprOfType(SourceOfRandomness random, Type type) {
        return "";
    }

    // TODO: implement
    private String generateMethodCallExprOfType(SourceOfRandomness random, Type type) {
        return "";
    }

    private String generateIndexExprOfType(SourceOfRandomness random, Type type) {
//        System.out.println("Generating index expr of type: " + type);
        Type listType = new ListValueType(type);
//        System.out.println("Generating list expr of type: " + listType);
        String result = "";
        result = generateListExprOfType(random, listType);
        String index = generateExpressionOfType(random, Type.INT_TYPE);
        result += "[" + index + "]";
        return result;
    }

    private String generateCallExprOfType(SourceOfRandomness random, Type type) {
        // Need to find a function that returns the correct type
        List<String> candidateFuncs = new ArrayList<>();
        for (Map.Entry<String, FuncType> entry : funcTypes.entrySet()) {
            Type returnType = entry.getValue().returnType;
            if (returnType.equals(type)) {
                candidateFuncs.add(entry.getKey());
            }
        }
        assumeTrue(!candidateFuncs.isEmpty());
        int funcIndex = random.nextInt(0, candidateFuncs.size());
        String func = candidateFuncs.get(funcIndex);
        FuncType funcType = funcTypes.get(func);
        List<ValueType> paramTypes = funcTypes.get(func).parameters;
        List<String> args = new ArrayList<>();
        for (Type paramType : paramTypes) {
            args.add(generateExpressionOfType(random, paramType));
        }
        return func + "(" + String.join(", ", args) + ")";
    }

    private String generateBinaryExprOfType(SourceOfRandomness random, Type type) {
//        System.out.println("Generating binary expr of type: " + type);
        Type operandType = null;
        String token = "";
        if (type == Type.INT_TYPE) {
            token = random.choose(INT_BINARY_OP_TOKENS);
            operandType = Type.INT_TYPE;
        } else if (type == Type.BOOL_TYPE) {
            token = random.choose(BOOL_BINARY_OP_TOKENS);
            switch(token) {
                case "and":
                case "or":
                    operandType = Type.BOOL_TYPE;
                    break;
                case "-":
                case "<":
                case "<=":
                case ">":
                case ">=":
                case "*":
                case "//":
                case "%":
                    operandType = Type.INT_TYPE;
                    break;
                case "==":
                case "!=":
                    operandType = random.choose(PRIMITIVE_TYPES);
                    break;
                case "is":
                    List<Type> nonPrimitiveTypes = new ArrayList<>(classTypes);
                    nonPrimitiveTypes.add(Type.OBJECT_TYPE);
                    nonPrimitiveTypes.add(new ListValueType(Type.INT_TYPE));
                    operandType = random.choose(nonPrimitiveTypes);
                    if (operandType.isListType()) {
                        int listDepth = random.nextInt(0, maxDepth);
                        operandType = random.choose(allTypes);
                        for (int i = 0; i < listDepth; i++) {
                            operandType = new ListValueType(operandType);
                        }
                    }
                    break;
                default:
                    System.out.println("Invalid token: " + token);
                    System.out.println("Invalid type: " + type);
                    assert(false);
            }
        } else if (type == Type.STR_TYPE) {
            token = "+";
            operandType = Type.STR_TYPE;
        } else if (type.isListType()) {
            token = "+";
            operandType = type;
        } else {
            assert(false);
        }
        String lhs = generateExpressionOfType(random, operandType);
        String rhs = generateExpressionOfType(random, operandType);
        return lhs + " " + token + " " + rhs;
    }

    private String generateUnaryExprOfType(SourceOfRandomness random, Type type) {
        Type operandType = null;
        String token = "";
        if (type == Type.INT_TYPE) {
            token = "-";
            operandType = Type.INT_TYPE;
        } else if (type == Type.BOOL_TYPE) {
            token = "not";
            operandType = Type.BOOL_TYPE;
        } else {
            assert(false);
        }
        return token + " " + generateExpressionOfType(random, operandType);
    }

    private String generateListExprOfType(SourceOfRandomness random, Type type) {
        assert(type.isListType());
        String result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                r -> generateConstExprOfType(r, type),
                r -> generateVarOfType(r, type),
                r -> generateParenExprOfType(r, type),
//          r -> generateMemberExprOfType(r, type),
//          r -> generateMethodCallExprOfType(r, type),
                r -> generateIndexExprOfType(r, type),
                r -> generateCallExprOfType(r, type),
                r -> generateBinaryExprOfType(r, type)
        )).apply(random);
        return result;
    }


    /** Generates a random ChocoPy expression using recursive calls */
    private String generateExpressionOfType(SourceOfRandomness random, Type type) {
        String result;
        // Choose terminal if nesting depth is too high or based on a random flip of a coin
        int randDepth = nextIntBound(random, 0, maxBound, maxDepth);
        if (expressionDepth >= randDepth) {
            result = generateConstExprOfType(random, type);
        } else {
            expressionDepth++;
            // Otherwise, choose a non-terminal generating function
            if (type == Type.INT_TYPE || type == Type.BOOL_TYPE) {
                result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                        r -> generateConstExprOfType(r, type),
                        r -> generateVarOfType(r, type),
                        r -> generateParenExprOfType(r, type),
//                        r -> generateMemberExprOfType(r, type),
//                        r -> generateMethodCallExprOfType(r, type),
                        r -> generateIndexExprOfType(r, type),
                        r -> generateCallExprOfType(r, type),
                        r -> generateBinaryExprOfType(r, type),
                        r -> generateUnaryExprOfType(r, type)
                )).apply(random);
            } else if (type.isListType() || type == Type.STR_TYPE) {
                result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                        r -> generateConstExprOfType(r, type),
                        r -> generateVarOfType(r, type),
                        r -> generateParenExprOfType(r, type),
//                        r -> generateMemberExprOfType(r, type),
//                        r -> generateMethodCallExprOfType(r, type),
                        r -> generateIndexExprOfType(r, type),
                        r -> generateCallExprOfType(r, type),
                        r -> generateBinaryExprOfType(r, type)
                )).apply(random);
            } else {
                result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                        r -> generateConstExprOfType(r, type),
                        r -> generateVarOfType(r, type),
                        r -> generateParenExprOfType(r, type),
//                        r -> generateMemberExprOfType(r, type),
//                        r -> generateMethodCallExprOfType(r, type),
                        r -> generateIndexExprOfType(r, type),
                        r -> generateCallExprOfType(r, type)
                        )).apply(random);
            }
            expressionDepth--;
        }
        return result;
    }

    // TODO: Add list type
    private String generateConstExprOfType(SourceOfRandomness random, Type type) {
        if (type == Type.INT_TYPE) {
            return generateIntLiteral(random);
        } else if (type == Type.STR_TYPE) {
            return generateStringLiteral(random);
        } else if (type == Type.BOOL_TYPE) {
            return generateBoolLiteral(random);
        } else if (type.isListType()) {
            return "[" + generateConstExprOfType(random, type.elementType()) + "]";
        } else {
            assumeTrue(false);
            return "";
        }
    }

    // TODO: Add member expressions and method calls
    private String generateAssignStmt(SourceOfRandomness random) {
        String result = "";
        // Choose a random entry in varTypes
        assumeTrue(!varTypes.isEmpty());
        String var = random.choose(varTypes.entrySet()).getKey();
        Type varType = varTypes.get(var);
        result += var + " = ";

        return result + generateExpressionOfType(random, varType);
    }

    /** Generates a block of statements, excluding the statement */
    private String generateBlock(SourceOfRandomness random, int minimum) {
        return String.join("", generateItems(this::generateStatement, random, minimum));
    }

//    private String generateClassDef(SourceOfRandomness random) {
//        String result = "";
//        String className = generateIdentifier(random);
//        // Superclass could be one of the identifiers or object. Index should be from 0 to maxIdentifiers inclusive.
//        int superClassIndex = nextIntBound(random, 0, classTypes.size(), maxIdentifiers);
//        String superClassName = classTypes.get(superClassIndex);
//        result += "class " + className + "(" + superClassName + "):\n";
//        indentLevel++;
//        result += generateDeclarationBlock(random, 1);
//        indentLevel--;
//        return result + "\n";
//    }

    /** Generates a block of VarDefs and FuncDefs*/
    private String generateDeclarationBlock(SourceOfRandomness random, int minimum) {
        return String.join("", generateItems(this::generateDeclaration, random, minimum));
    }

    private String generateExpressionStmt(SourceOfRandomness random) {
        Type randomType = random.choose(allTypes);
        return generateExpressionOfType(random, randomType);
    }

    private String generateForStmt(SourceOfRandomness random) {
        statementDepth++;
        ValueType listType = generateListType(random);
        String var = generateVarOfType(random, listType.elementType());
        String s = "for " + var + " in " + generateExpressionOfType(random, listType) + ":\n";
        indentLevel++;
        s += generateBlock(random, 1);
        indentLevel--;
        statementDepth--;
        return s;
    }

    private String generateFuncDef(SourceOfRandomness random) {
        declarationDepth++;

        String funcIdent = generateFuncIdentifier(random);
        int numParams = nextIntBound(random, 0, maxItems, maxItems);
        StringBuilder result = new StringBuilder("def " + generateFuncIdentifier(random) + "(");
        List<ValueType> paramTypes = new ArrayList<>();
        for (int i = 0; i < numParams; i++) {
            Pair<String, ValueType> param = generateTypedVar(random);
            paramTypes.add(param.getRight());
            result.append(param.getLeft()).append(":").append(param.getRight());
        }

        ValueType returnType = Type.NONE_TYPE;

        if (random.nextBoolean()) {
            returnType = generateType(random);
            result.append("->").append(returnType);
        }
        FuncType funcType = new FuncType(paramTypes, returnType);
        result.append(")");


        funcTypes.put(funcIdent, funcType);

        result.append(":\n");
        indentLevel++;
        result.append(String.join("", generateItems(this::generateFuncDeclaration, random, 0)));
        result.append(generateBlock(random, 1));
        if (returnType != Type.NONE_TYPE) {
            result.append(generateReturnStmtOfType(random, returnType));
        }
        indentLevel--;
        declarationDepth--;
        return result + "\n";
    }

    private String generateGlobalDecl(SourceOfRandomness random) {
        return "global " + generateIdentifier(random) + "\n";
    }

    private String generateIdentifier(SourceOfRandomness random) {
        return "a" + identCounter++;
    }

    private String generateFuncIdentifier(SourceOfRandomness random) {
        return "b" + funcIdentCounter++;
    }

    /** Creates initial set of identifiers depending on parameter */
    private void generateIdentifiers(int numIdentifiers) {
        this.identifiers = new ArrayList<>();
        this.funcIdentifiers = new ArrayList<>();
        funcIdentifiers.add("len");
        funcIdentifiers.add("print");
        String ident;
        for (int i = 0; i < numIdentifiers; i++) {
            ident = "a" + i;
            identifiers.add(ident);
            funcIdentifiers.add(ident);
        }
    }

    private String generateIfExprOfType(SourceOfRandomness random, Type type) {
        return generateExpressionOfType(random, type) + " if " + generateExpressionOfType(random, Type.BOOL_TYPE) + " else " + generateExpressionOfType(random, type);
    }

    private String generateIfStmt(SourceOfRandomness random) {
        statementDepth++;
        String result = "if " + generateExpressionOfType(random, Type.BOOL_TYPE) + ":\n";
        indentLevel++;
        result += generateBlock(random, 1);
        indentLevel--;
        if (random.nextBoolean()) {
            result += StringUtils.repeat(INDENT_TOKEN, indentLevel);
            result += "elif " + generateExpressionOfType(random, Type.BOOL_TYPE) + ":\n";
            indentLevel++;
            result += generateBlock(random, 1);
            indentLevel--;
        }
        if (random.nextBoolean()) {
            result += StringUtils.repeat(INDENT_TOKEN, indentLevel);
            result += "else:\n";
            indentLevel++;
            result += generateBlock(random, 1);
            indentLevel--;
        }
        statementDepth--;
        return result;
    }

    // Generate fixed primitive literals
    private String generateLiteralOfType(SourceOfRandomness random, Type type) {

        switch (type.toString()) {
            case "int":
                return generateIntLiteral(random);
            case "str":
                return generateStringLiteral(random);
            case "bool":
                return generateBoolLiteral(random);
            default:
                return generateNoneLiteral(random);
        }
    }

    private String generateIntLiteral(SourceOfRandomness random) {
        return random.choose(INT_LITERALS);
    }

    private String generateStringLiteral(SourceOfRandomness random) {
        return random.choose(STRING_LITERALS);
    }

    private String generateBoolLiteral(SourceOfRandomness random) {
        return random.choose(BOOL_LITERALS);
    }

    private String generateNoneLiteral(SourceOfRandomness random) {
        return "None";
    }

//    private String generateMemberExpr(SourceOfRandomness random) {
//        return "(" + generateCExpression(random) + ")." + generateIdentifier(random);
//    }
//
//    private String generateMethodCallExpr(SourceOfRandomness random) {
//        return generateCExpression(random) + "." + generateCallExpr(random);
//    }
//
//    private String generateNonlocalDecl(SourceOfRandomness random) {
//        return "nonlocal " + generateIdentifier(random) + "\n";
//    }
//

    private String generatePassStmt(SourceOfRandomness random) {
        return "pass";
    }

    private String generateReturnStmtOfType(SourceOfRandomness random, Type type) {
        return "return " + generateExpressionOfType(random, type);
    }

    /** Randomly choose from types and random list depth using maxDepth parameter */
    private ValueType generateType(SourceOfRandomness random) {
        ValueType baseType = random.choose(allTypes);
        int listDepth = random.nextInt(0, maxDepth);
        if (listDepth == 0) {
            return baseType;
        } else {
            for (int i = 0; i < listDepth; i++) {
                baseType = new ListValueType(baseType);
            }
        }
        return baseType;
    }

    /** Randomly choose from types and random list depth using maxDepth parameter */
    private ValueType generateListType(SourceOfRandomness random) {
        ValueType baseType = random.choose(allTypes);
        int listDepth = random.nextInt(1, maxDepth);
        for (int i = 0; i < listDepth; i++) {
            baseType = new ListValueType(baseType);
        }
        return baseType;
    }

    private Pair<String, ValueType> generateTypedVar(SourceOfRandomness random) {
        ValueType type = generateType(random);
        String ident = generateIdentifier(random);
        varTypes.put(ident, type);
        return Pair.of(ident, type);
    }

    private String generateVarDef(SourceOfRandomness random) {
        Pair<String, ValueType> typedVar = generateTypedVar(random);
        String ident = typedVar.getLeft();
        ValueType varType = typedVar.getRight();

        return ident + ":" + varType + " = " + generateLiteralOfType(random, varType) + "\n";
    }

    private String generateWhileStmt(SourceOfRandomness random) {
        statementDepth++;
        indentLevel++;
        String result = "while " + generateExpressionOfType(random, Type.BOOL_TYPE) + ":\n" + generateBlock(random, 1);
        indentLevel--;
        statementDepth--;
        return result;
    }
}
