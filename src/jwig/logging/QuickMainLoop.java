/*
 * Copyright (c) 2017, University of California, Berkeley
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

package jwig.logging;

import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;

/**
 * @author Rohan Padhye
 */
public class QuickMainLoop {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("No main class provided");
        }

        URL[] classPath = getAppClassPath();

        int N = 100;

        for (int i = 0; i < N; i++) {

            // Find main class and main() method
            Method mainMethod = getMainMethod(args[0], classPath);

            new Thread("main"+i) {
                public void run() {
                    try {
                        // Set-up args[]
                        String[] argzz = new String[args.length - 1];
                        System.arraycopy(args, 1, argzz, 0, argzz.length);

                        // Start tracing
                        SingleSnoop.startSnooping();

                        // Call main()
                        Object[] params = {argzz};
                        mainMethod.invoke(null, params);
                    } catch (Throwable e) {
                        // Ignore
                        // TODO: Handle errors
                    }

                }
            }.start();
        }


    }

    private static URL[] getAppClassPath() throws MalformedURLException {
        String jwigClassPath = System.getProperty("jwig.classpath");
        String[] paths = jwigClassPath.split(";");
        URL[] urls = new URL[paths.length];
        for (int i = 0; i < urls.length; i++) {
            urls[i] = new URL("file", null, -1, paths[i]);
        }
        return urls;
    }

    private static Method getMainMethod(String mainClassName, URL[] classpath) throws ClassNotFoundException, NoSuchMethodException {
        ClassLoader loader = new URLClassLoader(classpath);
        Class<?> mainClazz = Class.forName(mainClassName, true, loader);
        return mainClazz.getMethod("main", new String[]{}.getClass());
    }
}
