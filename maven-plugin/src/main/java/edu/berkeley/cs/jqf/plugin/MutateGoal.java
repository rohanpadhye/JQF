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

import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.fuzz.repro.ReproGuidance;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import edu.berkeley.cs.jqf.instrument.mutation.CartographyClassLoader;
import edu.berkeley.cs.jqf.instrument.mutation.MutationClassLoader;
import edu.berkeley.cs.jqf.instrument.mutation.MutationClassLoaders;
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
import java.nio.file.Path;
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
     * The corpus to be run against
     */
    @Parameter(property="corpus", required=false)
    private String corpus;

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
    @Parameter(property = "includes")
    String includeRegex;

    /**
     * classes to be mutated
     */
    @Parameter(property = "excludes")
    String excludeRegex;

    private String[] splitRegex(String regex) {
        return (includeRegex == null || includeRegex.equals("")) ? new String[0] : regex.split(",");
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        String targetName = testClassName + "#" + testMethod;
        String[] includeArray = splitRegex(includeRegex), excludeArray = splitRegex(excludeRegex);
        if (outputDirectory == null || outputDirectory.equals("")) {
            outputDirectory = "fuzz-results" + File.separator + testClassName;
        }

        File resultsDir = new File(outputDirectory, File.pathSeparator + testClassName);
        try {
            IOUtils.createDirectory(resultsDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create log file", e);
        }

        // Create main classloader
        ClassLoader papa = getClass().getClassLoader();

        try {
            URL urls[] = project.getTestClasspathElements().stream()
                .map(ThrowingFunction.wrap(x -> new File(x).toURI().toURL())).toArray(URL[]::new);
            
            if (corpus != null)
                GuidedFuzzing.setGuidance(new ReproGuidance(new File(corpus), null));
            
            System.err.println("Starting Initial Run:");
            
            CartographyClassLoader ccl = new CartographyClassLoader(urls, includeArray, excludeArray, papa);
            
            // Run initial test to compute mutants dynamically
            Result initialResults = runTest(ccl);
            if (!initialResults.wasSuccessful()) {
                System.err.println("Initial test run fails!");
                System.err.println(initialResults.getFailures());
                return;
            }
            
            if (corpus != null)
                GuidedFuzzing.unsetGuidance();
            
            List<MutationInstance> mutationInstances = ccl.getCartograph();
            
            List<MutationInstance> killedMutants = new ArrayList<>();
            
            MutationClassLoaders mcls = new MutationClassLoaders(urls, papa);
            if (corpus != null) {
                // Use ReproGuidance
                for (MutationInstance mutationInstance : mutationInstances) {
                    System.err.printf("Running Mutant %d\n", mutationInstance.id);
                    for (File input : new File(corpus).listFiles()) {
                        ReproGuidance rg = new ReproGuidance(input, null);
                        MutationClassLoader mcl = mcls.get(mutationInstance);
                        Result res = GuidedFuzzing.run(testClassName, testMethod, mcl, rg, null);
                        if (!res.wasSuccessful()) {
                            killedMutants.add(mutationInstance);
                            break;
                        }
                    }
                }
            } else {
                // Run each mutant without input
                for (MutationInstance mutationInstance : mutationInstances) {
                    System.out.println(mutationInstance);
                    MutationClassLoader mcl = new MutationClassLoader(mutationInstance, urls, papa);
                    mutationInstance.resetTimer();
                    Result mutantTestResult = runTest(mcl);
                    if (mutantTestResult.getFailureCount() > 0) {
                        killedMutants.add(mutationInstance);
                        Throwable t = mutantTestResult.getFailures().get(0).getException();
                    }
                }
            }
            
            File mutantReport = new File(resultsDir, "mutant-report");
            
            try (PrintWriter pw = new PrintWriter(mutantReport)) {
                for (MutationInstance mi : mutationInstances) {
                    pw.printf("%s - %s\n",
                              mi.toString(),
                              killedMutants.contains(mi) ? "Killed" : "Alive");
                }
            }
            
            String ls = String.format("Mutants Run: %d, Killed Mutants: %d",
                                      mutationInstances.size(),
                                      killedMutants.size());
            System.err.println(ls);
        } catch (Exception e) {
            throw new MojoExecutionException(e.toString());
        }
    }

    private Result runTest(ClassLoader cl) throws ClassNotFoundException {
        Class<?> clazz = Class.forName(testClassName, true, cl);
        Request testRequest = Request.method(clazz, testMethod);
        Runner testRunner = testRequest.getRunner();
        JUnitCore junit = new JUnitCore();
        return junit.run(testRunner);
    }
}
