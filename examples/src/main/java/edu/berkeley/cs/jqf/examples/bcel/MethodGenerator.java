package edu.berkeley.cs.jqf.examples.bcel;

import com.pholser.junit.quickcheck.generator.GenerationStatus;
import com.pholser.junit.quickcheck.generator.Generator;
import com.pholser.junit.quickcheck.random.SourceOfRandomness;
import org.apache.bcel.Const;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.*;
import org.junit.AssumptionViolatedException;

import java.util.function.Supplier;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MethodGenerator {
    private static final int MIN_ARGUMENTS = 0;
    private static final int MAX_ARGUMENTS = 10;
    private static final int MAX_INSTRUCTIONS = 100;
    private static final int MIN_INSTRUCTIONS = 0;
    private static final int MAX_SWITCH_KEYS = 10;
    private final Supplier<String> classNameSupplier;
    private final Supplier<String> identifierSupplier;
    private final TypeGenerator typeGenerator;
    private final SourceOfRandomness random;
    private final ClassGen clazz;

    MethodGenerator(SourceOfRandomness random, GenerationStatus status, ClassGen clazz) {
        this.random = random;
        this.clazz = clazz;
        Generator<String> classNameGenerator = new JavaClassNameGenerator();
        Generator<String> identifierGenerator = new JavaIdentifierGenerator();
        this.typeGenerator = new TypeGenerator(random, status);
        this.classNameSupplier = () -> classNameGenerator.generate(random, status);
        this.identifierSupplier = () -> identifierGenerator.generate(random, status);
    }

    public Method generate() {
        String methodName = identifierSupplier.get();
        Type returnType = random.nextBoolean() ? Type.VOID : typeGenerator.generate();
        int numberOfArguments = random.nextInt(MIN_ARGUMENTS, MAX_ARGUMENTS);
        Type[] argumentTypes = Stream.generate(typeGenerator::generate).limit(numberOfArguments).toArray(Type[]::new);
        String[] argumentNames = Stream.generate(identifierSupplier).limit(numberOfArguments).toArray(String[]::new);
        InstructionFactory factory = new InstructionFactory(clazz);
        InstructionList code = generateInstructions(factory, argumentTypes);
        short accessFlags = random.nextShort((short) 0, Short.MAX_VALUE);
        MethodGen method = new MethodGen(accessFlags,
                returnType,
                argumentTypes,
                argumentNames,
                methodName,
                clazz.getClassName(),
                code,
                clazz.getConstantPool());
        return method.getMethod();
    }

    private InstructionList generateInstructions(InstructionFactory factory, Type[] argumentTypes) {
        InstructionList instructionList = new InstructionList();
        int numberOfInstructions = random.nextInt(MIN_INSTRUCTIONS, MAX_INSTRUCTIONS);
        int maxLocals = random.nextInt(argumentTypes.length + 1, 2 * argumentTypes.length + 1);
        while (instructionList.size() < numberOfInstructions) {
            Instruction ins = generateInstruction(factory, maxLocals, instructionList);
            if (ins instanceof BranchInstruction) {
                instructionList.append((BranchInstruction) ins);
            } else {
                instructionList.append(ins);
            }
        }
        return instructionList;
    }

    private Instruction generateInstruction(InstructionFactory factory, int maxLocals,
                                            InstructionList instructionList) {
        short opcode = random.nextShort(Const.NOP, Const.BREAKPOINT);
        Instruction ins = InstructionConst.getInstruction(opcode);
        if (ins != null) {
            return ins; // Used predefined immutable object, if available
        }
        switch (opcode) {
            case Const.BIPUSH:
                return factory.createConstant(random.nextByte(Byte.MIN_VALUE, Byte.MAX_VALUE));
            case Const.SIPUSH:
                return factory.createConstant(random.nextShort(Short.MIN_VALUE, Short.MAX_VALUE));
            case Const.LDC:
            case Const.LDC_W:
                return factory.createConstant("?");
            case Const.LDC2_W:
                return factory.createConstant(random.nextDouble(Double.MIN_VALUE, Double.MAX_VALUE));
            case Const.ILOAD:
                return new ILOAD(random.nextInt(maxLocals));
            case Const.LLOAD:
                return new LLOAD(random.nextInt(maxLocals));
            case Const.FLOAD:
                return new FLOAD(random.nextInt(maxLocals));
            case Const.DLOAD:
                return new DLOAD(random.nextInt(maxLocals));
            case Const.ALOAD:
                return new ALOAD(random.nextInt(maxLocals));
            case Const.ILOAD_0:
            case Const.ILOAD_1:
            case Const.ILOAD_2:
            case Const.ILOAD_3:
                return new ILOAD(opcode - Const.ILOAD_0);
            case Const.LLOAD_0:
            case Const.LLOAD_1:
            case Const.LLOAD_2:
            case Const.LLOAD_3:
                return new LLOAD(opcode - Const.LLOAD_0);
            case Const.FLOAD_0:
            case Const.FLOAD_1:
            case Const.FLOAD_2:
            case Const.FLOAD_3:
                return new FLOAD(opcode - Const.FLOAD_0);
            case Const.DLOAD_0:
            case Const.DLOAD_1:
            case Const.DLOAD_2:
            case Const.DLOAD_3:
                return new DLOAD(opcode - Const.DLOAD_0);
            case Const.ALOAD_0:
            case Const.ALOAD_1:
            case Const.ALOAD_2:
            case Const.ALOAD_3:
                return new ALOAD(opcode - Const.ALOAD_0);
            case Const.ISTORE:
                return new ISTORE(random.nextInt(maxLocals));
            case Const.LSTORE:
                return new LSTORE(random.nextInt(maxLocals));
            case Const.FSTORE:
                return new FSTORE(random.nextInt(maxLocals));
            case Const.DSTORE:
                return new DSTORE(random.nextInt(maxLocals));
            case Const.ASTORE:
                return new ASTORE(random.nextInt(maxLocals));
            case Const.ISTORE_0:
            case Const.ISTORE_1:
            case Const.ISTORE_2:
            case Const.ISTORE_3:
                return new ISTORE(opcode - Const.ISTORE_0);
            case Const.LSTORE_0:
            case Const.LSTORE_1:
            case Const.LSTORE_2:
            case Const.LSTORE_3:
                return new LSTORE(opcode - Const.LSTORE_0);
            case Const.FSTORE_0:
            case Const.FSTORE_1:
            case Const.FSTORE_2:
            case Const.FSTORE_3:
                return new FSTORE(opcode - Const.FSTORE_0);
            case Const.DSTORE_0:
            case Const.DSTORE_1:
            case Const.DSTORE_2:
            case Const.DSTORE_3:
                return new DSTORE(opcode - Const.DSTORE_0);
            case Const.ASTORE_0:
            case Const.ASTORE_1:
            case Const.ASTORE_2:
            case Const.ASTORE_3:
                return new ASTORE(opcode - Const.ASTORE_0);
            case Const.IINC:
                return new IINC(random.nextInt(maxLocals), random.nextInt(-128, 128));
            case Const.GETSTATIC:
            case Const.PUTSTATIC:
            case Const.GETFIELD:
            case Const.PUTFIELD:
                return generateFieldAccess(factory, opcode);
            case Const.INVOKEVIRTUAL:
            case Const.INVOKESPECIAL:
            case Const.INVOKESTATIC:
            case Const.INVOKEINTERFACE:
            case Const.INVOKEDYNAMIC:
                return generateInvoke(factory, opcode);
            case Const.NEW:
                return factory.createNew(typeGenerator.generateObjectType());
            case Const.CHECKCAST:
                return factory.createCast(typeGenerator.generateReferenceType(), typeGenerator.generateReferenceType());
            case Const.INSTANCEOF:
                return factory.createInstanceOf(typeGenerator.generateReferenceType());
            case Const.NEWARRAY:
            case Const.ANEWARRAY:
            case Const.MULTIANEWARRAY:
                return factory.createNewArray(typeGenerator.generate(), random.nextShort((short) 1, (short) 20));
            case Const.IFEQ:
            case Const.IFNE:
            case Const.IFLT:
            case Const.IFGE:
            case Const.IFGT:
            case Const.IFLE:
            case Const.IF_ICMPEQ:
            case Const.IF_ICMPNE:
            case Const.IF_ICMPLT:
            case Const.IF_ICMPGE:
            case Const.IF_ICMPGT:
            case Const.IF_ICMPLE:
            case Const.IF_ACMPEQ:
            case Const.IF_ACMPNE:
            case Const.GOTO:
            case Const.JSR:
            case Const.IFNULL:
            case Const.IFNONNULL:
            case Const.GOTO_W:
            case Const.JSR_W:
                return InstructionFactory.createBranchInstruction(opcode, chooseTarget(instructionList));
            case Const.RET:
                return new RET(random.nextInt(maxLocals));
            case Const.TABLESWITCH:
            case Const.LOOKUPSWITCH:
                return generateSwitch(opcode, instructionList);
            case Const.WIDE:
            case Const.BREAKPOINT:
                return new BREAKPOINT();
            default:
                throw new AssumptionViolatedException("Invalid opcode");
        }
    }

    private InvokeInstruction generateInvoke(InstructionFactory factory, short opcode) {
        String className = classNameSupplier.get();
        String name = identifierSupplier.get();
        Type returnType = random.nextBoolean() ? Type.VOID : typeGenerator.generate();
        int numberOfArguments = random.nextInt(MIN_ARGUMENTS, MAX_ARGUMENTS);
        Type[] argumentTypes = Stream.generate(typeGenerator::generate).limit(numberOfArguments).toArray(Type[]::new);
        return factory.createInvoke(className, name, returnType, argumentTypes, opcode);
    }

    private InstructionHandle chooseTarget(InstructionList instructionList) {
        InstructionHandle[] handles = instructionList.getInstructionHandles();
        // If no instructions generated so far, emit a NOP to get some label
        if (handles.length == 0) {
            handles = new InstructionHandle[]{instructionList.append(new NOP())};
        }
        return random.choose(handles);
    }

    private Select generateSwitch(int opcode, InstructionList instructionList) {
        int[] matches = IntStream.generate(random::nextInt)
                .limit(random.nextInt(0, MAX_SWITCH_KEYS))
                .distinct()
                .sorted()
                .toArray();
        InstructionHandle[] targets = Stream.generate(() -> chooseTarget(instructionList))
                .limit(matches.length)
                .toArray(InstructionHandle[]::new);
        InstructionHandle defaultTarget = chooseTarget(instructionList);
        return opcode == Const.TABLESWITCH ? new TABLESWITCH(matches, targets, defaultTarget) :
                new LOOKUPSWITCH(matches, targets, defaultTarget);
    }

    private FieldInstruction generateFieldAccess(InstructionFactory factory, short opcode) {
        return factory.createFieldAccess(classNameSupplier.get(),
                identifierSupplier.get(),
                typeGenerator.generate(),
                opcode);
    }
}
