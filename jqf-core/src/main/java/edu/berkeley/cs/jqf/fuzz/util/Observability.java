package edu.berkeley.cs.jqf.fuzz.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import edu.berkeley.cs.jqf.fuzz.guidance.Result;

import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static edu.berkeley.cs.jqf.fuzz.guidance.Result.FAILURE;
import static edu.berkeley.cs.jqf.fuzz.guidance.Result.INVALID;

public class Observability {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String testClass;
    private final String testMethod;
    private final Path obsPath;
    private static ObjectNode testCaseJsonObject;
    private final long startTime;

    public Observability(String testClass, String testMethod, long startTime) {
        this.testClass = testClass;
        this.testMethod = testMethod;
        this.obsPath = Paths.get("target", "fuzz-results", testClass, testMethod, "observations.jsonl");
        if (obsPath.toFile().exists()) {
            obsPath.toFile().delete();
        }
        this.startTime = startTime;
        this.initializeTestCase();
    }

    public void initializeTestCase() {
        testCaseJsonObject = objectMapper.createObjectNode();
        testCaseJsonObject.putObject("features");
        testCaseJsonObject.putObject("timing");
        testCaseJsonObject.putObject("coverage");
        testCaseJsonObject.putObject("args");
        testCaseJsonObject.putObject("metadata");
        testCaseJsonObject.put("type", "test_case");
        testCaseJsonObject.put("run_start", startTime);
        testCaseJsonObject.put("property", testMethod);
    }

    public static void event(String value, Object payload) throws RuntimeException {
        // Add the payload to the features object
        JsonNode jsonFeaturesNode = testCaseJsonObject.get("features");
        ObjectNode featuresNode = (ObjectNode) jsonFeaturesNode;

        if (payload instanceof Integer) {
            featuresNode.put(value, (Integer) payload);
        } else if (payload instanceof String) {
            featuresNode.put(value, (String) payload);
        } else if (payload instanceof Float) {
            featuresNode.put(value, (Float) payload);
        } else {
            throw new RuntimeException("Unsupported payload type for event");
        }
    }

    public void addStatus(Result result) {
        if (result == INVALID) {
            testCaseJsonObject.put("status", "gave_up");
            testCaseJsonObject.put("status_reason", "assumption violated");
        } else if (result == FAILURE) {
            testCaseJsonObject.put("status", "failed");
            testCaseJsonObject.put("status_reason", "Encountered exception");
        } else {
            testCaseJsonObject.put("status", "passed");
            testCaseJsonObject.put("status_reason", "");
        }

    }

    public void addTiming(long startTime, long endGenerationTime, long endExecutionTime) {
        JsonNode timingNode = testCaseJsonObject.get("timing");
        ObjectNode timingObject = (ObjectNode) timingNode;
        timingObject.put("generation", endGenerationTime - startTime);
        timingObject.put("execution", endExecutionTime - endGenerationTime);
    }

    public void addArgs(Object[] args) {
        JsonNode argsNode = testCaseJsonObject.get("args");
        ObjectNode argsObject = (ObjectNode) argsNode;
        for (int i = 0; i < args.length; i++) {
            argsObject.put("arg" + i, args[i].toString());
        }
        add("representation", Arrays.toString(args));
    }

    public void add(String key, String value) {
        testCaseJsonObject.put(key, value);
    }

    public void writeToFile() {
        // Append the JSON object to a file followed by a newline
        try {
            String jsonString = objectMapper.writeValueAsString(testCaseJsonObject);
            try (FileWriter writer = new FileWriter(obsPath.toFile(), true)) {
                writer.write(jsonString);
                writer.write(System.lineSeparator()); // Add a new line after each object
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to write observations to file", e);
        }
    }
}
