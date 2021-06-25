/*
 * Copyright (c) 2021 Isabella Laybourn, Rohan Padhye
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
package edu.berkeley.cs.jqf.plugin;

import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import edu.berkeley.cs.jqf.fuzz.util.ThrowingRunnable;
import edu.berkeley.cs.jqf.instrument.mutation.CartographyClassLoader;
import edu.berkeley.cs.jqf.instrument.mutation.MutationClassLoader;
import edu.berkeley.cs.jqf.instrument.mutation.MutationInstance;
import edu.berkeley.cs.jqf.instrument.util.ThrowingFunction;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.junit.runner.JUnitCore;
import org.junit.runner.Request;
import org.junit.runner.Result;
import org.junit.runner.Runner;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mutation goal
 *
 * @author Bella Laybourn
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

    private String[] splitRegex(String regex) {
        return (includeRegex == null || includeRegex.equals("")) ? new String[0] : regex.split(",");
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String targetName = testClassName + "#" + testMethod;
        String[] includeArray = splitRegex(includeRegex), excludeArray = splitRegex(excludeRegex);
        if (outputDirectory == null || outputDirectory.equals("")) {
            outputDirectory = "mutation-results" + File.separator + testClassName;
        }

        File resultsDir = new File(target, outputDirectory);
        try {
            IOUtils.createDirectory(resultsDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create log file", e);
        }

        try (PrintWriter log = new PrintWriter(
                new FileWriter(new File(resultsDir.getPath() + File.separator + testMethod + ".txt")))) {
            // Create main classloader
            ClassLoader papa = getClass().getClassLoader();

            URL urls[] = project.getTestClasspathElements().stream()
                    .map(ThrowingFunction.wrap(x -> new File(x).toURI().toURL())).toArray(URL[]::new);

            CartographyClassLoader ccl = new CartographyClassLoader(urls, includeArray, excludeArray, papa);

            // Run initial test to compute mutants dynamically
            Result initialResults = runTest(ccl);
            if (!initialResults.wasSuccessful()) {
                System.err.println("Initial test run fails!");
                System.err.println(initialResults.getFailures());
                return;
            }

            // Mutants created after initial run
            List<MutationInstance> mutationInstances = ccl.getCartograph();
            System.out.printf("Mutants created: %d\n", mutationInstances.size());

            // Set up stats
            long totalRun = 0, totalFail = 0, totalIgnore = 0;
            long runByTest = 0, failByTest = 0;
            List<MutationInstance> killedMutants = new ArrayList<>();

            // Run each mutant
            for (MutationInstance mutationInstance : mutationInstances) {
                System.out.println(mutationInstance);
                log.println(mutationInstance);
                MutationClassLoader mcl = new MutationClassLoader(mutationInstance, urls, papa);
                mutationInstance.resetTimer();
                Result mutantTestResult = runTest(mcl);
                runByTest++;
                if (mutantTestResult.getFailureCount() > 0) {
                    failByTest++;
                    killedMutants.add(mutationInstance);
                    log.println(mutantTestResult.getFailures());
                    Throwable t = mutantTestResult.getFailures().get(0).getException();
                    if (t instanceof VerifyError) {
                        log.println(t.getMessage());
                    }
                }
            }

            System.out.println("Mutants Run: " + runByTest + ", Killed Mutants: " + failByTest);
            log.println("Mutants Run: " + runByTest + ", Killed Mutants: " + failByTest);
        } catch (ClassNotFoundException | DependencyResolutionRequiredException e) {
            throw new MojoFailureException("Bad Request", e);
        } catch (IOException e) {
            throw new MojoExecutionException("IO Exception", e);
        }
    }

    public Result runTest(ClassLoader cl) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(testClassName, true, cl);
        Request testRequest = Request.method(clazz, testMethod);
        Runner testRunner = testRequest.getRunner();
        JUnitCore junit = new JUnitCore();
        return junit.run(testRunner);
    }
}
