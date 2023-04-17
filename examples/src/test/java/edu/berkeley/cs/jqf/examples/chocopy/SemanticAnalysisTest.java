package edu.berkeley.cs.jqf.examples.chocopy;

import chocopy.ChocoPy;
import chocopy.common.astnodes.Program;
import chocopy.reference.RefAnalysis;
import chocopy.reference.RefParser;
import com.pholser.junit.quickcheck.From;
import edu.berkeley.cs.jqf.fuzz.Fuzz;
import edu.berkeley.cs.jqf.fuzz.JQF;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

@RunWith(JQF.class)
public class SemanticAnalysisTest {

    /** Entry point for fuzzing reference ChocoPy semantic analysis with ChocoPy code generator */
    @Fuzz
    public void fuzzSemanticAnalysis(@From(ChocoPySemanticGenerator.class) String code) {
        Program program = RefParser.process(code, false);
        assumeTrue(!program.hasErrors());
        RefAnalysis.process(program);
    }
}
