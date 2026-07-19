package edu.berkeley.cs.jqf.examples.chocopy;

import chocopy.common.astnodes.Program;
import chocopy.reference.RefAnalysis;
import chocopy.reference.RefCodeGen;
import chocopy.reference.RefParser;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.runner.RunWith;

import static org.junit.Assume.assumeTrue;

@RunWith(JQF.class)
public class ChocoPyTest {

    /** Entry point for fuzzing reference ChocoPy semantic analysis with ChocoPy code generator */
    @Fuzz
    public void fuzzSemanticAnalysis(@From(ChocoPySemanticGenerator.class) String code) {
        Program program = RefParser.process(code, false);
        assumeTrue(!program.hasErrors());
        Program typedProgram = RefAnalysis.process(program);
        assumeTrue(!typedProgram.hasErrors());
    }

    /** Entry point for fuzzing reference ChocoPy semantic analysis with ChocoPy code generator */
    @Fuzz
    public void fuzzSemanticAnalysisTypeDirected(@From(ChocoPySemanticGeneratorTypeDirected.class) String code) {
        Program program = RefParser.process(code, false);
        assumeTrue(!program.hasErrors());
        Program typedProgram = RefAnalysis.process(program);
        assumeTrue(!typedProgram.hasErrors());
    }

    @Fuzz
    public void fuzzCodeGen(@From(ChocoPySemanticGenerator.class) String code) {
        Program program = RefParser.process(code, false);
        assumeTrue(!program.hasErrors());
        Program typedProgram = RefAnalysis.process(program);
        assumeTrue(!typedProgram.hasErrors());
        RefCodeGen.process(typedProgram);
    }

    @Fuzz
    public void fuzzCodeGenTypeDirected(@From(ChocoPySemanticGeneratorTypeDirected.class) String code) {
        Program program = RefParser.process(code, false);
        assumeTrue(!program.hasErrors());
        Program typedProgram = RefAnalysis.process(program);
        assumeTrue(!typedProgram.hasErrors());
        RefCodeGen.process(typedProgram);
    }
}
