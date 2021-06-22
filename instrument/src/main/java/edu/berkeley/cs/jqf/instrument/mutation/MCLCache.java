package edu.berkeley.cs.jqf.instrument.mutation;

import java.net.URL;

/**
 * A lazy cache of {@link MutationClassLoader}s, one for each instance, with the
 * same {@code URL} list and parent {@code ClassLoader}
 */
public class MCLCache {
    private URL[] paths;
    private ClassLoader parent;
    private MutationClassLoader loaders[];

    /**
     * Creates an {@code MCLCache}
     * 
     * @param paths  The paths for the {@code MutationClassLoader}
     * @param parent The parent for the {@code MutationClassLoader}
     */
    public MCLCache(URL[] paths, ClassLoader parent) {
        this.paths = paths;
        this.parent = parent;
        loaders = new MutationClassLoader[MutationInstance.getNumInstances()];
    }

    /**
     * Creates, or finds in the cache a {@link MutationClassLoader}
     * 
     * @param mi The {@link MutationInstance} to be used
     * @return A {@link MutationClassLoader} which loads the given instance
     */
    public MutationClassLoader of(MutationInstance mi) {
        // Maybe preload these?
        if (loaders[mi.id] == null)
            loaders[mi.id] = new MutationClassLoader(mi, paths, parent);
        return loaders[mi.id];
    }
}
