package edu.berkeley.cs.jqf.examples.chocopy;

import com.pholser.junit.quickcheck.Pair;
import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.apache.commons.lang3.StringUtils;


import java.util.*;
import java.util.function.Function;
import chocopy.common.analysis.types.*;

import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

/**
 * ChocoPy Program Generator: Detailed Summary
 *
 * The ChocoPy program generator is designed to produce random, syntactically, and semantically valid
 * ChocoPy programs using a recursive, type-driven generation strategy. Here's a breakdown of the core
 * principles and methodology used in the generator:
 *
 * General Strategy:
 *
 * 1. Class Type Hierarchy Generation:
 *    - A random number of class types are generated.
 *    - Establishes parent-child relationships between classes, ensuring that every class ultimately descends
 *      from the base Object type.
 *
 * 2. Type Conformance:
 *    - Ensures that generated expressions and statements are type-conformant by using predefined and dynamically
 *      generated types.
 *    - Selects types that respect the established class hierarchy and type relationships.
 *
 * Program Generation:
 *
 * 1. Program Structure:
 *    - The top-level `generateProgram` function initializes class hierarchies and assembles classes, declarations,
 *      and statements into a complete program.
 *
 * 2. Class Definitions:
 *    - `generateClassDefOfType` creates class definitions by randomly generating attributes and method declarations.
 *
 * 3. Declarations:
 *    - Variable and function declarations are produced, with variable types and function signatures adhering to
 *      the current scope and type constraints.
 *    - Generates variable definitions, type annotations, and initial assignments.
 *
 * 4. Statements and Blocks:
 *    - Statements include simple and compound types like assignments, expressions, control flows (`if`, `while`,
 *      `for`), and function calls.
 *    - Blocks of statements and declarations are recursively generated, maintaining indentation and scope context.
 *
 * Expression Generation:
 *
 * 1. Expressions:
 *    - Consist of literals, variable references, calls, binary/unary operations, and more.
 *    - Generated expressions match the expected type and respect the nesting depth to prevent excessive recursion.
 *
 * 2. Expression Types:
 *    - Handles special types (int, bool, str), objects, lists, and other types (None and Empty).
 *    - Ensures that generated expressions conform to the expected type through type-driven choices.
 *
 * Auxiliary Functions:
 *
 * 1. Utility Functions:
 *    - Functions like `generateIdentifier`, `generateLiteralOfType`, and others support generating syntactically
 *      correct identifiers and literals.
 *    - `generateConformingType` generates types selected conform to the required constraints and hierarchies.
 *
 * 2. Random Bounds and Lists:
 *    - Uses custom bounded random functions to select sizes and contents within specified maximums.
 *    - Generates lists of items for program components like statements, attributes, etc., using defined constraints.
 *
 * Recursive Depth Management:
 *
 * 1. Depth Management:
 *    - Manages recursion and nesting depth to prevent infinite loops and overly complex constructs.
 *    - Controls branching choices to balance between simple and compound constructs based on current depth and randomness.
 *
 * Scoping and Context:
 *
 * 1. Scope Maintenance:
 *    - Tracks variable declarations, types, and function scopes to ensure valid references.
 *    - Manages global and local scopes, ensuring access to variables and functions in the correct context.
 *
 */

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
    private static Map<Type, Map<String, Type>> attrTypes; // Keeps track of attributes and their types
    private static Map<Type, Map<String, Type>> methodTypes; // Keeps track of methods and their types
    private static Map<Type, Type> parentClasses; // Keeps track of parent classes
    private static Map<Type, List<Type>> childClasses; // Keeps track of child classes
    private static Scope globalScope; // Keeps track of global scope
    private Scope currentScope; // Keeps track of current scope
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
        this.attrTypes = new HashMap<>();
        this.methodTypes = new HashMap<>();
        this.parentClasses = new HashMap<>();
        this.childClasses = new HashMap<>();
        this.childClasses.put(Type.OBJECT_TYPE, new ArrayList<>());
        this.globalScope = new Scope("global", null);
        this.currentScope = globalScope;
        this.identCounter = 0;
        this.funcIdentCounter = 0;
        return generateProgram(random);
    }

    public void generateClassTypeHierarchy(SourceOfRandomness random) {
        // Generate a random class type hierarchy. Start by generating a random number of classes,
        // and then randomly decide which ones are subclasses.
        int numClasses = nextIntBound(random, 0, maxBound, maxItems);
        if (numClasses == 0) {
            return;
        }
        List<String> classes = new ArrayList<>();
        for (int i = 0; i < numClasses; i++) {
            String classIdent = generateIdentifier(random);
            classes.add(classIdent);
            ValueType classType = new ClassValueType(classIdent);
            classTypes.add(classType);
        }
        // Assign the parent-child relationships. Loop over the classes (after the first one), and randomly
        // choose a parent from the previous elements.
        Type firstClassType = new ClassValueType(classes.get(0));
        childClasses.get(Type.OBJECT_TYPE).add(firstClassType);
        parentClasses.put(firstClassType, Type.OBJECT_TYPE);
        for (int i = 1; i < numClasses; i++) {
            if (random.nextBoolean()) {
                int parentIndex = random.nextInt(0, i);
                String parent = classes.get(parentIndex);
                String child = classes.get(i);
                Type parentType = new ClassValueType(parent);
                Type childType = new ClassValueType(child);
                parentClasses.put(childType, parentType);
                if (!childClasses.containsKey(parentType)) {
                    childClasses.put(parentType, new ArrayList<>());
                }
                childClasses.get(parentType).add(childType);
            } else {
                Type classType = new ClassValueType(classes.get(i));
                parentClasses.put(classType, Type.OBJECT_TYPE);
                childClasses.get(Type.OBJECT_TYPE).add(classType);
            }
        }
    }

    public Type generateConformingType(SourceOfRandomness random, Type type) {
        if (type.isSpecialType()) {
            return type;
        } else if (type.isListType()) {
            float randFloat = random.nextFloat();
            if (randFloat > 0.75) {
                return type;
            } else {
                return Type.EMPTY_TYPE;
            }
        } else if (type.isValueType()) {
            if (!childClasses.containsKey(type)) {
                return type;
            } else {
                List<Type> subTypes = childClasses.get(type);
                subTypes.add(type);
                return random.choose(subTypes);
            }
        } else {
            // Should not be function type
            assert(false);
        }
        return Type.EMPTY_TYPE;
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
        String classDeclarations = "";
        generateClassTypeHierarchy(random);
        for (Type classType : classTypes) {
            classDeclarations += generateClassDefOfType(random, classType);
        }

        String declarations = String.join("", generateItemsMultipleMethods(Arrays.asList(
                this::generateFuncDef,
                this::generateVarDef
        ), random, 0));
        String statements = generateBlock(random, 1);
        return classDeclarations + declarations + statements;
    }

    /** Generates a random ChocoPy declaration */
    private String generateClassDeclaration(SourceOfRandomness random, Type classType) {
        String result = StringUtils.repeat(INDENT_TOKEN, indentLevel);
        result += random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                r -> generateAttrDef(r, classType),
                r -> generateMethodDef(r, classType)
        )).apply(random);
        return result;
    }

    /** Generates a random ChocoPy function declaration */
    private String generateFuncDeclaration(SourceOfRandomness random) {
        String result = StringUtils.repeat(INDENT_TOKEN, indentLevel);
        int randDepth = nextIntBound(random, 0, maxBound, maxDepth);
        if (declarationDepth >= randDepth) {
            // Choose a random private method from this class, and then call it with `random`
            result += random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                    this::generateVarDef,
                    this::generateNonlocalDecl,
                    this::generateGlobalDecl
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
                    this::generateAssignStmt,
                    this::generatePassStmt,
                    this::generateExpressionStmt,
                    this::generateIfStmt,
                    this::generateForStmt,
                    this::generateWhileStmt
            )).apply(random);
        }
        return result + "\n";
    }

    private String generateVarOfType(SourceOfRandomness random, Type type, boolean onlyCurrentScope) {
        List<String> candidateVars = currentScope.getVarsOfType(type, onlyCurrentScope);
        assumeTrue(!candidateVars.isEmpty());
        return random.choose(candidateVars);
    }

    private String generateParenExprOfType(SourceOfRandomness random, Type type) {
        return "(" + generateExpressionOfType(random, type) + ")";
    }

    // TODO: implement
    private String generateMemberExprOfType(SourceOfRandomness random, Type type) {
        List<Pair<Type, String>> attrCandidates = new ArrayList<>();
        for (Type classType : attrTypes.keySet()) {
            Map<String, Type> attrs = attrTypes.get(classType);
            for (String attr : attrs.keySet()) {
                if (attrs.get(attr).equals(type)) {
                    attrCandidates.add(new Pair(classType, attr));
                }
            }
        }
        assumeTrue(!attrCandidates.isEmpty());
        Pair<Type, String> attrPair = random.choose(attrCandidates);
        Type classType = generateConformingType(random, attrPair.first);
        String objectExpr = generateExpressionOfType(random, classType);
        String attr = attrPair.second;
        return objectExpr + "." + attr;
    }

    // TODO: implement
    private String generateMethodCallExprOfType(SourceOfRandomness random, Type type) {
        // Need to find a method that returns the correct type
        List<Pair<String, FuncType>> candidateFuncs = globalScope.getMethodsWithReturnType(type);
        assumeTrue(!candidateFuncs.isEmpty());
        Pair<String, FuncType> func = random.choose(candidateFuncs);
        FuncType funcType = func.second;
        List<ValueType> paramTypes = funcType.parameters;
        List<String> args = new ArrayList<>();
        for (Type paramType : paramTypes) {
            args.add(generateExpressionOfType(random, generateConformingType(random, paramType)));
        }
        return func + "(" + String.join(", ", args) + ")";
    }

    private String generateIndexExprOfType(SourceOfRandomness random, Type type) {
//        System.out.println("Generating index expr of type: " + type);
        Type listType = new ListValueType(type);
//        System.out.println("Generating list expr of type: " + listType);
        String result = "";
        result = generateExpressionOfType(random, listType);
        String index = generateExpressionOfType(random, Type.INT_TYPE);
        result += "[" + index + "]";
        return result;
    }

    private String generateCallExprOfType(SourceOfRandomness random, Type type) {
        // Need to find a function that returns the correct type
        List<Pair<String, FuncType>> candidateFuncs = currentScope.getFuncsWithReturnType(type);
        assumeTrue(!candidateFuncs.isEmpty());
        Pair<String, FuncType> func = random.choose(candidateFuncs);
        FuncType funcType = func.second;
        List<ValueType> paramTypes = funcType.parameters;
        List<String> args = new ArrayList<>();
        for (Type paramType : paramTypes) {
            args.add(generateExpressionOfType(random, generateConformingType(random, paramType)));
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
                        int listDepth = random.nextInt(1, maxDepth);
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
        return "(" + lhs + " " + token + " " + rhs + ")";
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
        return "(" + token + " " + generateExpressionOfType(random, operandType) + ")";
    }

    private String generateListExprOfType(SourceOfRandomness random, Type type) {
        assert(type.isListType());
        String result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                r -> generateConstExprOfType(r, type),
                r -> generateVarOfType(r, type, false),
                r -> generateParenExprOfType(r, type),
                r -> generateMemberExprOfType(r, type),
                r -> generateMethodCallExprOfType(r, type),
                r -> generateIndexExprOfType(r, type),
                r -> generateCallExprOfType(r, type),
                r -> generateBinaryExprOfType(r, type)
        )).apply(random);
        return result;
    }


    /** Generates a random ChocoPy expression using recursive calls */
    private String generateExpressionOfType(SourceOfRandomness random, Type type) {
//        System.out.println("Generating expression of type: " + type);
//        System.out.println("Current expression depth: " + expressionDepth);
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
                        r -> generateVarOfType(r, type, false),
                        r -> generateParenExprOfType(r, type),
                        r -> generateMemberExprOfType(r, type),
                        r -> generateMethodCallExprOfType(r, type),
                        r -> generateIndexExprOfType(r, type),
                        r -> generateCallExprOfType(r, type),
                        r -> generateBinaryExprOfType(r, type),
                        r -> generateUnaryExprOfType(r, type)
                )).apply(random);
            } else if (type.isListType() || type == Type.STR_TYPE) {
                result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                        r -> generateConstExprOfType(r, type),
                        r -> generateVarOfType(r, type, false),
                        r -> generateParenExprOfType(r, type),
                        r -> generateMemberExprOfType(r, type),
                        r -> generateMethodCallExprOfType(r, type),
                        r -> generateIndexExprOfType(r, type),
                        r -> generateCallExprOfType(r, type),
                        r -> generateBinaryExprOfType(r, type)
                )).apply(random);
            } else if (type == Type.EMPTY_TYPE) {
                return "[]";
            } else if (type == Type.NONE_TYPE) {
                return "None";
            } else {
                result = random.choose(Arrays.<Function<SourceOfRandomness, String>>asList(
                        r -> generateConstExprOfType(r, type),
                        r -> generateVarOfType(r, type, false),
                        r -> generateParenExprOfType(r, type),
                        r -> generateMemberExprOfType(r, type),
                        r -> generateMethodCallExprOfType(r, type),
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
        } else if (type == Type.EMPTY_TYPE) {
            return "[]";
        } else if (type == Type.NONE_TYPE) {
            return "None";
        } else if (type.isListType()) {
            return "[" + generateConstExprOfType(random, type.elementType()) + "]";
        } else if (type.isValueType()) {
            return type.className() + "()";
        } else {
            assumeTrue(false);
            return "";
        }
    }

    // TODO: Add member expressions and method calls
    private String generateAssignStmt(SourceOfRandomness random) {
        String result = "";
        // Choose a random entry in varTypes. NOTE: varTypes will include variables from parent scopes
        // if they were declared nonlocal or global
        assumeTrue(!currentScope.varTypes.isEmpty());
        String var = random.choose(currentScope.varTypes.entrySet()).getKey();
        Type varType = currentScope.varTypes.get(var);
        result += var + " = ";

        // Randomly generate a conforming type
//        System.out.println("Generating expression of type: " + varType);
        Type conformingType = generateConformingType(random, varType);
//        System.out.println("Generating conforming expression of type: " + conformingType);

        return result + generateExpressionOfType(random, conformingType);
    }

    /** Generates a block of statements, excluding the statement */
    private String generateBlock(SourceOfRandomness random, int minimum) {
        return String.join("", generateItems(this::generateStatement, random, minimum));
    }

    private String generateClassDef(SourceOfRandomness random) {
        String result = "";
        String className = generateIdentifier(random);
        // Superclass could be one of the identifiers or object. Index should be from 0 to maxIdentifiers inclusive.
        int superClassIndex = nextIntBound(random, 0, classTypes.size(), maxIdentifiers);
        String superClassName = generateIdentifier(random);
        result += "class " + className + "(" + superClassName + "):\n";
        indentLevel++;
        result += generateClassDeclarationBlock(random, null);
        indentLevel--;
        return result + "\n";
    }

    private String generateClassDefOfType(SourceOfRandomness random, Type type) {
        String result = "";
        // Superclass could be one of the identifiers or object. Index should be from 0 to maxIdentifiers inclusive.
        Type superClassType = parentClasses.get(type);
        result += "class " + type.className() + "(" + superClassType.className() + "):\n";
        indentLevel++;
        result += generateClassDeclarationBlock(random, type);
        indentLevel--;
        return result + "\n";
    }

    /** Generates a block of VarDefs and FuncDefs*/
    private String generateClassDeclarationBlock(SourceOfRandomness random, Type classType) {
        String declarations = String.join("", generateItems(r -> generateClassDeclaration(r, classType), random, 1));
        return String.join("", declarations);
    }

    private String generateExpressionStmt(SourceOfRandomness random) {
        Type randomType = random.choose(allTypes);
        return generateExpressionOfType(random, randomType);
    }

    private String generateForStmt(SourceOfRandomness random) {
        statementDepth++;
        ValueType listType = generateListType(random);
        String var = generateVarOfType(random, listType.elementType(), true);
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
        currentScope = new Scope(funcIdent, currentScope);
        int numParams = nextIntBound(random, 0, maxItems, maxItems);
        StringBuilder result = new StringBuilder("def " + generateFuncIdentifier(random) + "(");
        List<ValueType> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();
        for (int i = 0; i < numParams; i++) {
            Pair<String, ValueType> param = generateTypedVar(random);
            paramTypes.add(param.second);
            paramNames.add(param.first + ":" + param.second);
            currentScope.varTypes.put(param.first, param.second);
        }
        result.append(String.join(",", paramNames));
        result.append(")");

        ValueType returnType = Type.NONE_TYPE;

        if (random.nextBoolean()) {
            returnType = generateType(random);
            result.append("->").append(returnType);
        }
        FuncType funcType = new FuncType(paramTypes, returnType);
        result.append(":\n");
        indentLevel++;
        result.append(String.join("", generateItems(this::generateFuncDeclaration, random, 0)));
        result.append(generateBlock(random, 1));
        if (returnType != Type.NONE_TYPE) {
            result.append(StringUtils.repeat(INDENT_TOKEN, indentLevel)).append(generateReturnStmtOfType(random, generateConformingType(random, returnType)));
        }
        currentScope.funcTypes.put(funcIdent, funcType);
        indentLevel--;
        declarationDepth--;
        currentScope = currentScope.getParent();
        return result + "\n";
    }

    private String generateMethodDef(SourceOfRandomness random, Type classType) {
        declarationDepth++;

        String funcIdent = generateFuncIdentifier(random);
        currentScope = new Scope(funcIdent, currentScope);
        int numParams = nextIntBound(random, 0, maxItems, maxItems);
        String firstParam = generateIdentifier(random);
        ValueType firstParamType = new ClassValueType("\"" + classType.className() + "\"");
        StringBuilder result = new StringBuilder("def " + generateFuncIdentifier(random) + "(");
        List<ValueType> paramTypes = new ArrayList<>();
        List<String> paramNames = new ArrayList<>();
        paramTypes.add(firstParamType);
        paramNames.add(firstParam + ":" + firstParamType);
        for (int i = 1; i < numParams; i++) {
            Pair<String, ValueType> param = generateTypedVar(random);
            paramTypes.add(param.second);
            paramNames.add(param.first + ":" + param.second);
            currentScope.varTypes.put(param.first, param.second);
        }
        result.append(String.join(",", paramNames));
        result.append(")");

        ValueType returnType = Type.NONE_TYPE;

        if (random.nextBoolean()) {
            returnType = generateType(random);
            result.append("->").append(returnType);
        }
        FuncType funcType = new FuncType(paramTypes, returnType);
        result.append(":\n");
        indentLevel++;
        result.append(String.join("", generateItems(this::generateFuncDeclaration, random, 0)));
        result.append(generateBlock(random, 1));
        if (returnType != Type.NONE_TYPE) {
            result.append(StringUtils.repeat(INDENT_TOKEN, indentLevel)).append(generateReturnStmtOfType(random, generateConformingType(random, returnType)));
        }
        if (!methodTypes.containsKey(classType)) {
            methodTypes.put(classType, new HashMap<>());
        }
        methodTypes.get(classType).put(funcIdent, funcType);
        currentScope.funcTypes.put(classType + "." + funcIdent, funcType);
        indentLevel--;
        declarationDepth--;
        currentScope = currentScope.getParent();
        return result + "\n";
    }

    private String generateGlobalDecl(SourceOfRandomness random) {
        assumeTrue(!currentScope.name.equals("global"));
        assumeTrue(!globalScope.varTypes.isEmpty());
        String var = random.choose(globalScope.varTypes.keySet());
        assumeTrue(!currentScope.varTypes.containsKey(var));
        Type varType = globalScope.varTypes.get(var);
        currentScope.varTypes.put(var, varType);
        return "global " + var + "\n";
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

    private String generateNonlocalDecl(SourceOfRandomness random) {
        List<Pair<String, Type>> candidateVars = currentScope.getNonlocalVars(globalScope);
        assumeTrue(!candidateVars.isEmpty());
        Pair<String, Type> var = random.choose(candidateVars);
        currentScope.varTypes.put(var.first, var.second);
        return "nonlocal " + var.first + "\n";
    }


    private String generatePassStmt(SourceOfRandomness random) {
        return "pass";
    }

    private String generateReturnStmtOfType(SourceOfRandomness random, Type type) {
        return "return " + generateExpressionOfType(random, type);
    }

    /** Randomly choose from types and random list depth using maxDepth parameter */
    private ValueType generateType(SourceOfRandomness random) {
        List<Type> candidateTypes = new ArrayList<>(allTypes);
        candidateTypes.addAll(classTypes);
        ValueType baseType = (ValueType) random.choose(candidateTypes);
        float randFloat = random.nextFloat();
        if (randFloat > 0.75) {
            return baseType;
        } else {
            int listDepth = random.nextInt(0, maxDepth);
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
        return new Pair(ident, type);
    }

    private String generateVarDef(SourceOfRandomness random) {
        Pair<String, ValueType> typedVar = generateTypedVar(random);
        String ident = typedVar.first;
        ValueType varType = typedVar.second;
        currentScope.varTypes.put(ident, varType);

        return ident + ":" + varType + " = " + generateLiteralOfType(random, varType) + "\n";
    }

    private String generateAttrDef(SourceOfRandomness random, Type classType) {
        Pair<String, ValueType> typedVar = generateTypedVar(random);
        String ident = typedVar.first;
        ValueType varType = typedVar.second;

        if (!attrTypes.containsKey(classType)) {
            attrTypes.put(classType, new HashMap<>());
        }
        attrTypes.get(classType).put(ident, varType);

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
