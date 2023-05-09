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
                try {
                    return Class.forName(osc.getName(), true, cl);
                } catch (Exception e) {
                    e.printStackTrace();
                    return super.resolveClass(osc);
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
