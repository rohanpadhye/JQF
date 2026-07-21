/*
 * Copyright (c) 2026 Vladimir Sitnikov and JQF Contributors
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
package edu.berkeley.cs.jqf.junit5;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import edu.berkeley.cs.jqf.fuzz.ei.ZestGuidance;
import edu.berkeley.cs.jqf.fuzz.guidance.Guidance;
import edu.berkeley.cs.jqf.fuzz.repro.ReproGuidance;

/**
 * Reads a {@code @FuzzTest}'s run configuration from its annotation, system
 * properties, the environment, and any externally installed guidance.
 *
 * <p>This is the single place that decides between the two modes (regression and
 * fuzzing), builds the {@link Guidance} for a fuzzing campaign, and enumerates the
 * inputs replayed by a regression run.
 */
public final class JUnit5FuzzConfig {

    /** Default fuzzing duration (minutes) when neither a duration nor a trial limit is set. */
    private static final long DEFAULT_DURATION_MINUTES = 10L;

    /**
     * A guidance installed from outside (for example a future command-line or
     * build-tool launcher). When set, every {@code @FuzzTest} runs in fuzzing mode
     * driven by this guidance, mirroring {@code GuidedFuzzing.setGuidance} on the
     * JUnit 4 path.
     */
    private static volatile Guidance installedGuidance;

    private final Class<?> testClass;
    private final Method method;
    private final FuzzTest annotation;

    private JUnit5FuzzConfig(Class<?> testClass, Method method) {
        this.testClass = testClass;
        this.method = method;
        this.annotation = method.getAnnotation(FuzzTest.class);
    }

    /**
     * Reads the configuration for one fuzz-test method.
     *
     * @param testClass the test class
     * @param method    the {@code @FuzzTest} method
     * @return the configuration
     */
    public static JUnit5FuzzConfig from(Class<?> testClass, Method method) {
        return new JUnit5FuzzConfig(testClass, method);
    }

    /**
     * Installs a guidance to drive every subsequent {@code @FuzzTest} run.
     *
     * @param guidance the guidance to install
     */
    public static void installGuidance(Guidance guidance) {
        installedGuidance = guidance;
    }

    /**
     * Returns the externally installed guidance, or {@code null} if none is set.
     *
     * @return the installed guidance, or {@code null}
     */
    public static Guidance installedGuidance() {
        return installedGuidance;
    }

    /**
     * Clears any externally installed guidance.
     */
    public static void clearInstalledGuidance() {
        installedGuidance = null;
    }

    /**
     * Reports whether this run should fuzz rather than replay a bounded regression.
     *
     * <p>Fuzzing is selected by an installed guidance, the {@code jqf.fuzz} system
     * property, the {@code JQF_FUZZ} environment variable, or a {@code repro} input.
     *
     * @return {@code true} for a fuzzing campaign, {@code false} for regression
     */
    public boolean isFuzzingMode() {
        if (installedGuidance != null) {
            return true;
        }
        if (Boolean.getBoolean("jqf.fuzz")) {
            return true;
        }
        String env = System.getenv("JQF_FUZZ");
        if (env != null && !env.isEmpty() && !"false".equalsIgnoreCase(env) && !"0".equals(env)) {
            return true;
        }
        return !reproPath().isEmpty();
    }

    /**
     * Builds the guidance for a fuzzing campaign.
     *
     * @return an installed, repro, or fresh Zest guidance
     * @throws IOException if the guidance's output directory cannot be prepared
     */
    public Guidance createGuidance() throws IOException {
        if (installedGuidance != null) {
            return installedGuidance;
        }
        String repro = reproPath();
        if (!repro.isEmpty()) {
            return new ReproGuidance(new File(repro), null);
        }
        Duration duration = maxDuration();
        Long trials = maxTrials();
        if (duration == null && trials == null) {
            // Keep a `-Djqf.fuzz=true` run finite even when neither bound is given.
            duration = Duration.ofMinutes(Long.getLong("jqf.fuzz.defaultDurationMinutes", DEFAULT_DURATION_MINUTES));
        }
        String testName = testClass.getName() + "#" + method.getName();
        return new ZestGuidance(testName, duration, trials, outputDirectory(), new Random());
    }

    /**
     * Enumerates the inputs replayed by a regression run: the saved corpus from a
     * previous fuzzing run, any {@code seeds}, and one empty (all-zero) input.
     *
     * @return the regression inputs, in replay order
     */
    public List<NamedInput> regressionInputs() {
        List<NamedInput> inputs = new ArrayList<>();
        addFiles(inputs, new File(outputDirectory(), "corpus"));
        String seeds = annotation != null ? annotation.seeds() : "";
        if (!seeds.isEmpty()) {
            addFiles(inputs, new File(seeds));
        }
        inputs.add(NamedInput.zeros());
        return inputs;
    }

    /**
     * Returns the output directory for this method's fuzzing results,
     * {@code <base>/<FQCN>/<method>} where the base is {@code target/fuzz-results}
     * unless overridden by the {@code jqf.fuzz.out} system property.
     *
     * @return the output directory
     */
    public File outputDirectory() {
        String base = System.getProperty("jqf.fuzz.out", "target" + File.separator + "fuzz-results");
        return new File(new File(base, testClass.getName()), method.getName());
    }

    String reproPath() {
        String sys = System.getProperty("jqf.repro", "");
        if (!sys.isEmpty()) {
            return sys;
        }
        return annotation != null ? annotation.repro() : "";
    }

    Long maxTrials() {
        String sys = System.getProperty("jqf.fuzz.trials");
        if (sys != null && !sys.isEmpty()) {
            return Long.parseLong(sys);
        }
        long trials = annotation != null ? annotation.maxTrials() : 0L;
        return trials > 0 ? trials : null;
    }

    Duration maxDuration() {
        String sys = System.getProperty("jqf.fuzz.duration");
        String spec = (sys != null && !sys.isEmpty())
                ? sys
                : (annotation != null ? annotation.maxDuration() : "");
        return parseDuration(spec);
    }

    private static void addFiles(List<NamedInput> inputs, File fileOrDirectory) {
        if (fileOrDirectory == null || !fileOrDirectory.exists()) {
            return;
        }
        if (fileOrDirectory.isDirectory()) {
            File[] files = fileOrDirectory.listFiles(File::isFile);
            if (files != null) {
                Arrays.sort(files);
                for (File file : files) {
                    inputs.add(NamedInput.ofFile(file));
                }
            }
        } else {
            inputs.add(NamedInput.ofFile(fileOrDirectory));
        }
    }

    /**
     * Parses a human-friendly duration such as {@code "30s"}, {@code "10m"},
     * {@code "2h"} or {@code "1d"}; a bare number is read as seconds.
     *
     * @param spec the duration specification (may be {@code null} or empty)
     * @return the parsed duration, or {@code null} if {@code spec} is blank
     */
    static Duration parseDuration(String spec) {
        if (spec == null) {
            return null;
        }
        String trimmed = spec.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            char unit = trimmed.charAt(trimmed.length() - 1);
            if (Character.isLetter(unit)) {
                long value = Long.parseLong(trimmed.substring(0, trimmed.length() - 1).trim());
                switch (Character.toLowerCase(unit)) {
                    case 's':
                        return Duration.ofSeconds(value);
                    case 'm':
                        return Duration.ofMinutes(value);
                    case 'h':
                        return Duration.ofHours(value);
                    case 'd':
                        return Duration.ofDays(value);
                    default:
                        throw new IllegalArgumentException("Unknown duration unit '" + unit + "' in '" + spec + "'");
                }
            }
            return Duration.ofSeconds(Long.parseLong(trimmed));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid duration: '" + spec + "'. Use forms like 30s, 10m, 2h, 1d, or a number of seconds.", e);
        }
    }

    /**
     * A named source of one regression input's bytes.
     */
    public static final class NamedInput {

        private final String name;
        private final InputSource source;

        private NamedInput(String name, InputSource source) {
            this.name = name;
            this.source = source;
        }

        /**
         * Returns a display name for this input.
         *
         * @return the input name
         */
        public String name() {
            return name;
        }

        /**
         * Opens a fresh stream over this input's bytes.
         *
         * @return the input stream
         * @throws IOException if the input cannot be opened
         */
        public InputStream open() throws IOException {
            return source.open();
        }

        static NamedInput ofFile(File file) {
            return new NamedInput(file.getName(), () -> new BufferedInputStream(new FileInputStream(file)));
        }

        static NamedInput zeros() {
            return new NamedInput("zeros", () -> new InputStream() {
                @Override
                public int read() {
                    return 0;
                }
            });
        }

        private interface InputSource {
            InputStream open() throws IOException;
        }
    }
}
