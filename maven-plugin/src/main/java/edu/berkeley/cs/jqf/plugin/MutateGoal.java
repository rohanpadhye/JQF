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

import cmu.pasta.mu2.MutationInstance;
import cmu.pasta.mu2.instrument.CartographyClassLoader;
import cmu.pasta.mu2.instrument.MutationClassLoader;
import cmu.pasta.mu2.instrument.MutationClassLoaders;
import cmu.pasta.mu2.instrument.OptLevel;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.fuzz.repro.ReproGuidance;
import edu.berkeley.cs.jqf.fuzz.util.IOUtils;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.AbstractMojoExecutionException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.junit.runner.Result;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader.stringsToUrls;

/**
 * Repro goal for mu2. Performs mutation testing to calculate
 * mutation score for a saved test input corpus.
 *
 * @author Bella Laybourn
 */
@Mojo(name="mutate",
      requiresDependencyResolution= ResolutionScope.TEST)
public class MutateGoal extends AbstractMojo {
    @Parameter(defaultValue="${project}", required=true, readonly=true)
    MavenProject project;

    @Parameter(property="resultsDir", defaultValue="${project.build.directory}", readonly=true)
    private File resultsDir;

    /**
     * The corpus of inputs to repro
     */
    @Parameter(property="input", required=true)
    private File input;

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
    String includes;

    /**
     * classes to be mutated
     */
    @Parameter(property = "excludes")
    String excludes;

    /**
     * Allows user to set optimization level for mutation-guided fuzzing.
     */
    @Parameter(property="optLevel", defaultValue = "none")
    private String optLevel;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        Log log = getLog();
        OptLevel ol;
        switch(optLevel) {
            case "none": ol = OptLevel.NONE; break;
            case "execution": ol = OptLevel.EXECUTION; break;
            case "infection": ol = OptLevel.INFECTION; break;
            case "propagation": ol = OptLevel.PROPAGATION; break;
            default: throw new MojoExecutionException("OptLevel must be set to 'none', 'execution', 'infection', or 'propagation'");
        }

        try {
            // Get project-specific classpath and output directory
            List<String> classpathElements = project.getTestClasspathElements();
            String[] classPath = classpathElements.toArray(new String[0]);
            IOUtils.createDirectory(resultsDir);

            // Create mu2 classloaders from the test classpath
            MutationClassLoaders mcls = new MutationClassLoaders(classPath, includes, ol);
            CartographyClassLoader ccl = mcls.getCartographyClassLoader();

            // Run initial test to compute mutants dynamically
            System.out.println("Starting Initial Run:");
            Result initialResults = runRepro(ccl);
            if (!initialResults.wasSuccessful()) {
                throw new MojoFailureException("Initial test run fails",
                        initialResults.getFailures().get(0).getException());
            }

            // Retrieve dynamically collected mutation instances
            List<MutationInstance> mutationInstances = ccl.getMutationInstances();

            // Track which mutants get killed
            List<MutationInstance> killedMutants = new ArrayList<>();

            // Run a repro on all mutants
            for (MutationInstance mutationInstance : mutationInstances) {
                log.info("Running Mutant " + mutationInstance.toString());
                MutationClassLoader mcl = mcls.getMutationClassLoader(mutationInstance);
                Result res = runRepro(mcl);
                if (!res.wasSuccessful()) {
                    killedMutants.add(mutationInstance);
                }
            }

            File mutantReport = new File(resultsDir, "mutant-report.csv");
            try (PrintWriter pw = new PrintWriter(mutantReport)) {
                for (MutationInstance mi : mutationInstances) {
                    pw.printf("%s,%s\n",
                              mi.toString(),
                              killedMutants.contains(mi) ? "Killed" : "Alive");
                }
            }

            String ls = String.format("Mutants Run: %d, Killed Mutants: %d",
                                      mutationInstances.size(),
                                      killedMutants.size());
            System.out.println(ls);
        } catch (AbstractMojoExecutionException e) {
            throw e; // Propagate as is
        } catch (Exception e) {
            throw new MojoExecutionException(e.toString(), e);
        }
    }

    // Executes a fresh repro with a given classloader
    private Result runRepro(ClassLoader classLoader) throws ClassNotFoundException, IOException {
        ReproGuidance repro = new ReproGuidance(input, null);
        repro.setStopOnFailure(true);
        return GuidedFuzzing.run(testClassName, testMethod, classLoader, repro, null);
    }
}
