package edu.berkeley.cs.jqf.fuzz.difffuzz;

import edu.berkeley.cs.jqf.fuzz.repro.ReproGuidance;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DiffFuzzReproGuidance extends ReproGuidance implements DiffFuzzGuidance {
    private Method compare;
    protected List<Outcome> cmpTo;
    public static final List<Outcome> recentOutcomes = new ArrayList<>();

    public DiffFuzzReproGuidance(File[] inputFile, File traceDir) throws IOException {
        super(inputFile, traceDir);
        cmpTo = null;
        recentOutcomes.clear();
        try {
            compare = Objects.class.getMethod("equals", Object.class, Object.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    public DiffFuzzReproGuidance(File[] inputFile, File traceDir, List<Outcome> cmpRes) throws IOException {
        this(inputFile, traceDir);
        cmpTo = cmpRes;
    }

    public DiffFuzzReproGuidance(File inputFile, File traceDir) throws IOException {
        this(IOUtils.resolveInputFileOrDirectory(inputFile), traceDir);
    }

    public DiffFuzzReproGuidance(File inputFile, File traceDir, List<Outcome> cmpRes) throws IOException {
        this(IOUtils.resolveInputFileOrDirectory(inputFile), traceDir, cmpRes);
    }

    @Override
    public void setCompare(Method m) {
        compare = m;
    }

    @Override
    public void run(TestClass testClass, FrameworkMethod method, Object[] args) throws Throwable {
        Outcome out = getOutcome(testClass.getJavaClass(), method, args);
        recentOutcomes.add(out);

        if (cmpTo == null) { // not comparing
            if (out.thrown != null) throw out.thrown;
            return;
        }

        // use serialization to load both outputs with the same ClassLoader
        //TODO may not want serialization for all diff repros
        Outcome cmpOut = cmpTo.get(recentOutcomes.size() - 1);
        ClassLoader cmpCL = compare.getDeclaringClass().getClassLoader();
        Outcome cmpSerial = new Outcome(Serializer.translate(cmpOut.output, cmpCL), cmpOut.thrown);
        Outcome outSerial = new Outcome(Serializer.translate(out.output, cmpCL), out.thrown);

        if (!Outcome.same(cmpSerial, outSerial, compare)) {
            throw new DiffException(cmpTo.get(recentOutcomes.size() - 1), out);
        }
    }
}
