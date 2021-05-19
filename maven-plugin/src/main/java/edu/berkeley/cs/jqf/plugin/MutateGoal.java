package edu.berkeley.cs.jqf.plugin;

import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import edu.berkeley.cs.jqf.instrument.mutation.CartographyClassLoader;
import edu.berkeley.cs.jqf.instrument.mutation.MutationInstance;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Mutation goal
 */
@Mojo(name="mutate",
        requiresDependencyResolution= ResolutionScope.TEST)
public class MutateGoal extends AbstractMojo {
    @Parameter(defaultValue="${project}", required=true, readonly=true)
    MavenProject project;

    @Parameter(defaultValue="${project.build.directory}", readonly=true)
    private File target;

    @Parameter(property="out", required = false, defaultValue = "")
    private String outputDirectory;

    /**
     * Test class
     */
    @Parameter(property = "class", required=true)
    String testClassName;

    /**
     * Test method
     */
    @Parameter(property = "method", required=true)
    private String testMethod;

    /**
     * classes to be mutated
     */
    @Parameter(property = "includeClasses")
    String includeRegex;

    /**
     * classes to be mutated
     */
    @Parameter(property = "excludeClasses")
    String excludeRegex;

    @Override
    public void execute() {
        String targetName = testClassName + "#" + testMethod;
        String[] includeArray, excludeArray;
        if(includeRegex == null || includeRegex.equals("")) {
            includeArray = new String[0];
        } else {
            includeArray = includeRegex.split(",");
        }
        if(excludeRegex == null || excludeRegex.equals("")) {
            excludeArray = new String[0];
        } else {
            excludeArray = excludeRegex.split(",");
        }
        if(outputDirectory == null || outputDirectory.equals("")) {
            outputDirectory = "mutation-results" + File.separator + testClassName;
        }
        try {
            File resultsDir = new File(target, outputDirectory);
            IOUtils.createDirectory(resultsDir);
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(resultsDir.getPath() + File.separator + testMethod + ".txt")));
            List<String> classpaths =  project.getTestClasspathElements();
            CartographyClassLoader ccl = new CartographyClassLoader(classpaths.toArray(new String[0]), includeArray, excludeArray, getClass().getClassLoader());
            Result cclResult = runTest(ccl);
            writer.write("Tested CartographyClassLoader (original): " + cclResult.getFailures() + "\n");
            List<MutationInstance> instanceMap = ccl.getCartograph();
            long totalRun = 0, totalFail = 0, totalIgnore = 0;
            long runByTest = 0, failByTest = 0;
            List<MutationInstance> killedMutants = new ArrayList<>();
            for(MutationInstance mcl : instanceMap) {
                System.out.println(mcl);
                Result mclResult = runTest(mcl);
                writer.write("Failures from MutationClassLoader " + mcl + ":\n" + mclResult.getFailures() + "\n  --> Failed: " + mclResult.getFailureCount() + ", Ignored: " + mclResult.getIgnoreCount() + ", Run: " + mclResult.getRunCount() + "\n");
                totalRun += mclResult.getRunCount();
                totalFail += mclResult.getFailureCount();
                totalIgnore += mclResult.getIgnoreCount();
                runByTest++;
                if(mclResult.getFailureCount() > 0) {
                    failByTest++;
                    killedMutants.add(mcl);
                }
            }
            writer.write("Totals:\nFailures: " + totalFail + ", Ignored: " + totalIgnore + ", Run: " + totalRun);
            System.out.println("Totals:\nFailures: " + totalFail + ", Ignored: " + totalIgnore + ", Run: " + totalRun);
            System.out.println("Mutants Run: " + runByTest + ", Failing Mutants: " + failByTest);
            writer.close();
        } catch (ClassNotFoundException | DependencyResolutionRequiredException | IOException e) {
            e.printStackTrace();
        }
    }

    public Result runTest(ClassLoader cl) throws ClassNotFoundException {
        Request testRequest = Request.method(Class.forName(testClassName, true, cl), testMethod);
        Runner testRunner = testRequest.getRunner();
        JUnitCore junit = new JUnitCore();
        return junit.run(testRunner);
    }
}