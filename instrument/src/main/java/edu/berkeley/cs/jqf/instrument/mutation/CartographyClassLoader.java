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
package edu.berkeley.cs.jqf.instrument.mutation;

import janala.instrument.SnoopInstructionTransformer;
import org.objectweb.asm.*;

import edu.berkeley.cs.jqf.instrument.InstrumentingClassLoader;

import java.io.*;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * ClassLoader for initial run in mutation guidance
 * Runs like InstrumentingClassLoader while also prepping MutationInstances
 *
 * @author Bella Laybourn
 */
public class CartographyClassLoader extends URLClassLoader {
    /** List of available MutationInstances */
    private final List<MutationInstance> cartograph;

    /** if nonempty, class must be here to be mutable */
    private final List<String> includeClasses;

    /** class must not be here to be mutable */
    private final List<String> excludeClasses;

    /** see {@link InstrumentingClassLoader} */
    private final ClassFileTransformer lineCoverageTransformer = new SnoopInstructionTransformer();

    /** Constructor */
    public CartographyClassLoader(URL[] paths, String[] mutables, String[] immutables, ClassLoader parent) throws MalformedURLException {
        super(paths, parent);
        includeClasses = new ArrayList<>(Arrays.asList(mutables));
        excludeClasses = new ArrayList<>(Arrays.asList(immutables));
        cartograph = new ArrayList<>();
    }

    public List<MutationInstance> getCartograph() {
        return cartograph;
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes;

        String internalName = name.replace('.', '/');
        String path = internalName.concat(".class");
        try (InputStream in = super.getResourceAsStream(path)) {
            if (in == null) {
                throw new ClassNotFoundException("Cannot find class " + name);
            }
            bytes = in.readAllBytes();
        } catch (IOException e) {
            throw new ClassNotFoundException("I/O exception while loading class.", e);
        }

        // Check includes + excludes
        boolean mutable = includeClasses.isEmpty();
        for(String s : includeClasses) {
            if (name.startsWith(s)) {
                mutable = true;
                break;
            }
        }
        for (String s : excludeClasses) {
            if (name.startsWith(s)) {
                mutable = false;
                break;
            }
        }

        // Make cartograph
        if (mutable) {
            Cartographer c = Cartographer.explore(new ClassReader(bytes));

            for (List<MutationInstance> opportunities : c.getOpportunities().values())
                for (MutationInstance chance: opportunities)
                    cartograph.add(chance);

            bytes = c.toByteArray();
        }

        // Instrument class to measure both line coverage and mutation coverage
        //
        try {
            byte[] instrumented;
            instrumented = lineCoverageTransformer.transform(this, internalName, null, null, bytes.clone());
            if (instrumented != null)
                bytes = instrumented;
        } catch (IllegalClassFormatException __) {
        }

        return defineClass(name, bytes, 0, bytes.length);
    }
}
