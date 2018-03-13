/*
 * Copyright (c) 2018, University of California, Berkeley
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
package edu.berkeley.cs.jqf;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.MalformedURLException;
import java.util.List;

import edu.berkeley.cs.jqf.fuzz.ei.ExecutionIndexingGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.junit.GuidedFuzzing;
import edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * The main Mojo for the JQF Maven plugin.
 *
 * <p>This is the entry point for fuzzing via Maven.</p>
 *
 * <p>Enables "mvn jqf:fuzz".</p>
 *
 * @author Rohan Padhye
 */
@Mojo(name="fuzz",
        requiresDependencyResolution= ResolutionScope.TEST,
        defaultPhase=LifecyclePhase.VERIFY)
public class FuzzPlugin extends AbstractMojo {

    @Parameter(defaultValue="${project}", required=true, readonly=true)
    MavenProject project;

    @Parameter(defaultValue="${project.build.directory}", readonly=true)
    private File target;

    @Parameter(property="class", required=true)
    private String testClassName;

    @Parameter(property="method", required=true)
    private String testMethod;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {


        ClassLoader loader;
        Guidance guidance;
        PrintStream out = System.out; // TODO: Re-route to logger from super.getLog()

        // TODO: Configure these with @Parameter
        System.setProperty("janala.excludes", "java/,com/sun/proxy/,com/intellij/,edu/berkeley/cs/jqf/,org/junit/,com/pholser/junit/quickcheck/,ru/vyarus/java/generics/resolver/,org/javaruntype/,ognl,org/hamcrest/,org/omg/,org/netbeans/,org/mozilla/javascript/gen");
        System.setProperty("janala.includes", "edu/berkeley/cs/jqf/examples");

        try {
            List<String> classpathElements = project.getTestClasspathElements();

            loader = new InstrumentingClassLoader(
                    classpathElements.toArray(new String[0]),
                    getClass().getClassLoader());
        } catch (DependencyResolutionRequiredException|MalformedURLException e) {
            throw new MojoExecutionException("Could not get project classpath", e);
        }

        try {
            File resultsDir = new File(target, "fuzz-results");
            guidance = new ExecutionIndexingGuidance(10_000, resultsDir);
        } catch (IOException e) {
            throw new MojoExecutionException("Could not create output directory", e);
        }

        try {
            GuidedFuzzing.run(testClassName, testMethod, loader, guidance, out);
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException("Could not load test class", e);
        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException("Bad request", e);
        } catch (RuntimeException e) {
            throw new MojoExecutionException("Internal error", e);
        }
    }
}
