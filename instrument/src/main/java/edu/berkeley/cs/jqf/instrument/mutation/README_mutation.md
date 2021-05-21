# Mutation-Guided Fuzzing
Documentation for running and understanding the implementation of mutation-guided fuzzing provided here.

For project writeup, see [this document](https://saphirasnow.github.io/17-355/Bella_Laybourn_17355_Project.pdf).

For more questions, feel free to email [Bella Laybourn](mailto:ilaybour@andrew.cmu.edu).

## Running
### Mutation Guidance
Runs like Zest, just add flag `-Dengine=mutation` on `jqf:fuzz` terminal commands.

Example: `mvn jqf:fuzz -Dclass=package.class -Dmethod=method -Dengine=mutation`

### Mutate Goal
For reproducing results from mutation-guided fuzzing. Run using `jqf:mutate` with the `-Dclass` and `-Dmethod` flags.

Example: `mvn jqf:mutate -Dclass=package.class -Dmethod=method`

## Implementation
### ClassLoaders
#### MutationInstance
A unique classloader to apply a particular mutation to a particular place in a particular class. 
Because each is uniquely associated with a mutant, it acts as the representative object for the mutant as a whole (currently contains additionally whether the mutant has been caught, could be extended to contain other information as well).

Mutants are for this implementation uniquely identifiable by mutator (what kind of mutation), class (where the mutation is applied), and instance (index in the sequential listing of all opportunities to apply that mutator to that class).

#### CartographyClassLoader
Initial class loading should take place using this ClassLoader. It creates a list of MutationInstances representing all of the mutation opportunities (the "cartograph").

### Mutators
#### Mutator Format
Each mutator replaces one instruction with another, or with a series of instructions. 
They sometimes take the form `prefix_oldInstruction_TO_newInstruction(Opcodes.oldInstruction, returnType, [list, of, instruction, calls, ...])` and other times, as appropriate to the type of mutator, are just descriptors.

To make that more clear, three actual examples, broken down:

`I_ADD_TO_SUB(Opcodes.IADD, null, new InstructionCall(Opcodes.ISUB))`
refers to swapping <b>I</b>nteger <b>ADD</b>ition for <b>SUB</b>traction, which requires no relevant return type.

`VOID_REMOVE_STATIC(Opcodes.INVOKESTATIC, "V", new InstructionCall(Opcodes.NOP))`
refers to removing calls to void functions made with `invokestatic`, which requires that the return type of the called function be <b>V</b>oid.

`S_IRETURN_TO_0(Opcodes.IRETURN, "S", new InstructionCall(Opcodes.POP), new InstructionCall(Opcodes.ICONST_0), new InstructionCall(Opcodes.IRETURN))`
refers to making a function with return type <b>S</b>hort return the short `0` instead of what it would normally have returned. This requires that the return type of the returning function be <b>S</b>hort.

Note how in the second two examples, the opcode alone was not enough to identify the relevant return type, which is why that information was included separately.

##### Pops: An Aside
When calling functions, the arguments are loaded onto the stack. This means that, when removing a function call, those arguments must be popped off the stack before continuing. 
Typically, we would pop off the same number of arguments as the function has, but when the opcode is `invokestatic` there's one fewer because there's an implicit `this` argument added in other cases that `invokestatic` doesn't need.

#### InstructionCall
Just a wrapper for bytecode instructions so their arguments can be included.

### Guidance
For the most part, an extension of Zest.
For each fuzz input, runs first the CartographyClassLoader, then each of the MutationInstances it has generated that hasn't been killed by a previous fuzz input.
Saves for reason `+mutants` along with Zest's `+coverage`, etc.

### Timeout
To prevent hanging forever on an infinite loop, both MutationInstances and CartographyClassLoaders outfit their classes with a timeout functionality that essentially kills the program after some maximum number of control jumps.
This assumes that if the number of control jumps exceeds that then the program has encountered an infinite loop and will not ascertain any new information by continuing to run.
