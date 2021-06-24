package edu.berkeley.cs.jqf.instrument.mutation;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * A lazy cache of {@link MutationClassLoader}s, one for each instance, with the
 * same {@code URL} list and parent {@code ClassLoader}
 */
public class MCLCache {
    private URL[] paths;
    private ClassLoader parent;
    private Map<Integer, MutationClassLoader> loaders;

    /**
     * Creates an {@code MCLCache}
     * 
     * @param paths  The paths for the {@code MutationClassLoader}
     * @param parent The parent for the {@code MutationClassLoader}
     */
    public MCLCache(URL[] paths, ClassLoader parent) {
        this.paths = paths;
        this.parent = parent;
        loaders = new HashMap<>();
    }

    /**
     * Creates, or finds in the cache a {@link MutationClassLoader}
     * 
     * @param mi The {@link MutationInstance} to be used
     * @return A {@link MutationClassLoader} which loads the given instance
     */
    public MutationClassLoader of(MutationInstance mi) {
        MutationClassLoader mcl = loaders.get(mi.id);
        if (mcl == null) {
            mcl = new MutationClassLoader(mi, paths, parent);
            loaders.put(mi.id, mcl);
        }
        return mcl;
    }
}
