package org.pomizer.model;

public class RawJarInfo extends JarInfo {
    
    public long lastModified;

    public RawJarInfo(final String name, final int basePathIndex, final long lastModified) {
        super(name, basePathIndex);
        
        this.lastModified = lastModified;
    }
}
