/*
 * Copyright (c) 2021 Isabella Laybourn
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

import java.io.*;
import java.util.ArrayList;
import java.util.List;

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

            byte[] bytes;
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("edu/berkeley/cs/jqf/instrument/mutation/MutationTimeoutException.class")) {
                if (in == null) {
                    throw new ClassNotFoundException("Cannot find class " + "edu/berkeley/cs/jqf/instrument/mutation/MutationTimeoutException");
                }
                BufferedInputStream buf = new BufferedInputStream(in);

                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                int b;
                while ((b = buf.read()) != -1) {
                    baos.write(b);
                }
                bytes = baos.toByteArray();
            } catch (IOException e) {
                throw new ClassNotFoundException("I/O exception while loading class.", e);
            }

            CartographyClassLoader ccl = new CartographyClassLoader(classpaths.toArray(new String[0]), includeArray, excludeArray, getClass().getClassLoader(), bytes);
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
        Class<?> clazz = Class.forName(testClassName, true, cl);
        Request testRequest = Request.method(clazz, testMethod);
        Runner testRunner = testRequest.getRunner();
        JUnitCore junit = new JUnitCore();
        return junit.run(testRunner);
    }
}