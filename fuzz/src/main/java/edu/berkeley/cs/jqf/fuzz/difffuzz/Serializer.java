package edu.berkeley.cs.jqf.fuzz.difffuzz;

import java.io.*;

public class Serializer {
    public static Object[] translate(Object[] original, ClassLoader newCL) throws IOException, ClassNotFoundException {
        return deserialize(serialize(original), newCL, original);
    }

    public static Object translate(Object original, ClassLoader newCL) throws IOException, ClassNotFoundException {
        Object[] arr = new Object[]{original};
        return deserialize(serialize(arr), newCL, arr)[0];
    }

    public static byte[] serialize(Object[] items) throws IOException {
        try(ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(out)) {
            for (Object item : items) {
                if(item != null) oos.writeObject(item);
            }
            return out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
            throw e;
        }
    }

    public static Object[] deserialize(byte[] bytes, ClassLoader cl, Object[] original) throws ClassNotFoundException, IOException {
        try(ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes)) {
            @Override
            public Class<?> resolveClass(ObjectStreamClass osc) throws IOException, ClassNotFoundException {
                String className = osc.getName();
                
                // Only allow specific classes to be deserialized
                if (!found) {
                    // First class must be the expected main class
                    if (!className.equals(mainClass.getName())) {
                        throw new InvalidClassException("Unexpected class: ", className);
                    } else {
                        found = true;
                    }
                } else {
                    // All subsequent classes must be in the allowed components list
                    if (!components.contains(className)) {
                        throw new InvalidClassException("Unexpected class: ", className);
                    }
                }
                
                // Only load the class after validation
                try {
                    // Using "false" as the second parameter to avoid executing static initializers
                    return Class.forName(className, false, cl);
                } catch (Exception e) {
                    // Log without exposing stack trace
                    logger.warn("Failed to load class: " + className);
                    throw new ClassNotFoundException("Failed to resolve class: " + className);
                }
            }
        }) {
            Object[] itemArr = new Object[original.length];
            for(int c = 0; c < original.length; c++) {
                if(original[c] != null) itemArr[c] = ois.readObject();
                else original[c] = null;
            }
            return itemArr;
        } catch (ClassNotFoundException | IOException e) {
            e.printStackTrace();
            throw e;
        }
    }
}
