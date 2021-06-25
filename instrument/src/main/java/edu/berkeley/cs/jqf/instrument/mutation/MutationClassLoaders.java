package edu.berkeley.cs.jqf.instrument.mutation;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A lazily instantiated collection of {@link MutationClassLoader}s,
 * one for each instance, with the same {@code URL} list of class paths
 * and parent {@code ClassLoader}.
 */
public class MutationClassLoaders {
    private URL[] paths;
    private ClassLoader parent;
    private Map<MutationInstance, MutationClassLoader> loaders;

    /**
     * Creates an {@code MCLCache}
     * 
     * @param paths  The paths for the {@code MutationClassLoader}
     * @param parent The parent for the {@code MutationClassLoader}
     */
    public MutationClassLoaders(URL[] paths, ClassLoader parent) {
        this.paths = paths;
        this.parent = parent;
        loaders = new HashMap<>();
    }

    /**
     * Retrieves a {@link MutationClassLoader} for a given
     * {@link MutationInstance}, creating a new classloader if
     * such a mapping does not yet exist.
     * 
     * @param mi The {@link MutationInstance} to be used
     * @return A {@link MutationClassLoader} which loads the given instance
     */
    public MutationClassLoader get(MutationInstance mi) {
        MutationClassLoader mcl = loaders.get(mi);
        if (mcl == null) {
            mcl = new MutationClassLoader(mi, paths, parent);
            loaders.put(mi, mcl);
        }
        return mcl;
    }
}
