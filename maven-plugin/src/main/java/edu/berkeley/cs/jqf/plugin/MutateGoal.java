package edu.berkeley.cs.jqf.plugin;

import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import mutation.CartographyClassLoader;
import mutation.MutationClassLoader;
import mutation.Mutator;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

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
        System.out.println("targetName: " + targetName);
        String[] includeArray, excludeArray;
        if(includeRegex == null || includeRegex.equals("")) {
            includeArray = new String[0];
        } else {
            includeArray = includeRegex.split(":");
        }
        if(excludeRegex == null || excludeRegex.equals("")) {
            excludeArray = new String[0];
        } else {
            excludeArray = excludeRegex.split(":");
        }
        if(outputDirectory == null || outputDirectory.equals("")) {
            outputDirectory = "mutation-results" + File.separator + testClassName;
        }
        try {
            File resultsDir = new File(target, outputDirectory);
            IOUtils.createDirectory(resultsDir);
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(resultsDir.getPath() + File.separator + testMethod + ".txt")));
            List<String> otherClasspaths =  project.getTestClasspathElements();
            CartographyClassLoader ccl = new CartographyClassLoader(otherClasspaths.toArray(new String[0]), includeArray, excludeArray, getClass().getClassLoader());
            Result cclResult = test(ccl);
            writer.write("Tested CartographyClassLoader (original): " + cclResult.getFailures() + "\n");
            Map<String, Map<String, Map<Mutator, Long>>> instanceMap = ccl.getCartograph();
            System.out.println(instanceMap);
            for(String s : instanceMap.keySet()) { //TODO this giant for is parallelizable
                for(String i : instanceMap.get(s).keySet()) {
                    for(Mutator m : instanceMap.get(s).get(i).keySet()) {
                        for(int c = 0; c < instanceMap.get(s).get(i).get(m); c++) {
                            MutationClassLoader mcl = new MutationClassLoader(otherClasspaths.toArray(new String[0]), getClass().getClassLoader(), m, c, s);
                            Result mclResult = test(mcl);
                            writer.write("Failures from instance " + c + " of mutator " + m + " in file " + s + ":\n" + mclResult.getFailures() + "\n  --> Failed: " + mclResult.getFailureCount() + ", Ignored: " + mclResult.getIgnoreCount() + ", Run: " + mclResult.getRunCount() + "\n");
                            //extensions: might realize there are other classes; dynamically growing instancemap?
                            //think about how to guidance - wrap tests? (by overriding TrialRunner?)
                        }
                    }
                }
            }
            writer.close();
        } catch (ClassNotFoundException | DependencyResolutionRequiredException | IOException e) {
            e.printStackTrace();
        }
    }

    public Result test(ClassLoader cl) throws ClassNotFoundException {
        Request testRequest = Request.method(Class.forName(testClassName, true, cl), testMethod);
        Runner testRunner = testRequest.getRunner();
        JUnitCore junit = new JUnitCore();
        //junit.addListener(new TextListener(System.out));
        return junit.run(testRunner);
    }
}
