package edu.berkeley.cs.jqf.fuzz.util;

import edu.berkeley.cs.jqf.fuzz.ei.state.AbstractExecutionIndexingState;
import edu.berkeley.cs.jqf.fuzz.ei.state.FastExecutionIndexingState;
import edu.berkeley.cs.jqf.fuzz.ei.state.JanalaExecutionIndexingState;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class CoverageFactory {

    public static final String propFile = System.getProperty("janala.conf", "janala.conf");

    private static boolean FAST_NON_COLLIDING_COVERAGE_ENABLED;
    static
    {
        Properties properties = new Properties();
        try (InputStream propStream = new FileInputStream(propFile)) {
            properties.load(propStream);
        } catch (IOException e) {
            // Swallow exception and continue with defaults
            // System.err.println("Warning: No janala.conf file found");
        }
        properties.putAll(System.getProperties());
        FAST_NON_COLLIDING_COVERAGE_ENABLED = Boolean.parseBoolean(properties.getProperty("useFastNonCollidingCoverageInstrumentation", "false"));
    }

    public static ICoverage newInstance() {
        if (FAST_NON_COLLIDING_COVERAGE_ENABLED) {
            return new FastNonCollidingCoverage();
        } else {
            return new Coverage();
        }
    }

    public static AbstractExecutionIndexingState newEIState() {
        if (FAST_NON_COLLIDING_COVERAGE_ENABLED) {
            return new FastExecutionIndexingState();
        } else {
            return new JanalaExecutionIndexingState();
        }
    }
}
