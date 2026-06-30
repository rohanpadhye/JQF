/*
 * Copyright (c) 2020, The Regents of the University of California
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
package edu.berkeley.cs.jqf.fuzz.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Utility class containing static methods for common I/O operations
 *
 * @author Rohan Padhye
 */
public class IOUtils {

    /**
     * Resolves an input file or a directory of files.
     *
     * This is useful for operations like providing seed inputs to a
     * fuzzing engine or repro'ing a corpus of test inputs.
     *
     * @param file a file or directory
     * @return a listing of files, if `file` is a directory;
     *         a list containing only `file`, if it is a regular file;
     *         `null` if `file` is also `null`
     * @throws FileNotFoundException if {@code file} does not exist
     */
    public static File[] resolveInputFileOrDirectory(File file) throws FileNotFoundException {
        if (file == null) {
            return null;
        } else if (file.isDirectory()) {
            File[] files = file.listFiles();
            Arrays.sort(files, Comparator.comparing(File::getName));
            return files;
        } else if (file.isFile()) {
            return new File[]{file};
        } else {
            throw new FileNotFoundException("Could not find file: " + file);
        }
    }

    /**
     * Creates a new writable directory in a given parent directory.
     *
     * @param parent the parent directory
     * @param name   the name of the new directory to create
     * @return the newly created directory
     * @throws IOException if a writable directory was not created
     */
    public static File createDirectory(File parent, String name) throws IOException {
        File newDir = new File(parent, name);
        return createDirectory(newDir);
    }

    /**
     * Creates a new writable directory.
     *
     * @param newDir the new directory to create
     * @return the newly created directory (same as `newDir`)
     * @throws IOException if a writable directory was not created
     */
    public static File createDirectory(File newDir) throws IOException {
        newDir.mkdirs();

        if (!newDir.isDirectory() || !newDir.canWrite()) {
            throw new IOException("Could not create directory: " + newDir.getAbsolutePath());
        } else {
            return newDir;
        }
    }

}
