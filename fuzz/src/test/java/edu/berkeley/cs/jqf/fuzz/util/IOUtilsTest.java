package edu.berkeley.cs.jqf.fuzz.util;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class IOUtilsTest {
    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    @Test
    public void shouldResolveSingleFile() throws IOException {
        final File file = temp.newFile();
        final File[] resolved = IOUtils.resolveInputFileOrDirectory(file);
        Assert.assertEquals(1, resolved.length);
        Assert.assertEquals(file, resolved[0]);
    }

    @Test
    public void shouldResolveFilesInDirectory() throws IOException {
        final File dir = temp.newFolder();
        final File a = dir.toPath().resolve("a").toFile();
        Assume.assumeTrue(a.createNewFile());
        final File b = dir.toPath().resolve("b").toFile();
        Assume.assumeTrue(b.createNewFile());
        final File[] resolved = IOUtils.resolveInputFileOrDirectory(dir);
        Assert.assertEquals(2, resolved.length);
        Assert.assertEquals(a, resolved[0]);
        Assert.assertEquals(b, resolved[1]);
    }

    @Test
    public void shouldResolveFilesInSubdirectories() throws IOException {
        final File dir = temp.newFolder();
        final File a = dir.toPath().resolve("a").toFile();
        Assume.assumeTrue(a.createNewFile());
        final File b = dir.toPath().resolve("b").toFile();
        Assume.assumeTrue(b.createNewFile());
        final File sub = dir.toPath().resolve("sub").toFile();
        Assume.assumeTrue(sub.mkdir());
        final File subA = dir.toPath().resolve("a").toFile();
        Assume.assumeTrue(subA.createNewFile());
        final File subB = dir.toPath().resolve("b").toFile();
        Assume.assumeTrue(subB.createNewFile());
        final File[] resolved = IOUtils.resolveInputFileOrDirectory(dir);
        Assert.assertEquals(4, resolved.length);
        Assert.assertEquals(a, resolved[0]);
        Assert.assertEquals(b, resolved[1]);
        Assert.assertEquals(subA, resolved[2]);
        Assert.assertEquals(subB, resolved[3]);
    }
}
