package org.pomizer.model;

public class JarInfo implements Comparable<JarInfo> {

    public String name;

    public int basePathIndex;
    
    public JarInfo(final String name, final int basePathIndex) {
        this.name = name;
        this.basePathIndex = basePathIndex;
    }

    public int compareTo(final JarInfo value) {

        if (null == value) {
            return -1;
        }

        if (null == this.name) {
            return 1;
        }

        int result = this.name.compareTo(value.name);

        if (0 == result) {
            if (this.basePathIndex < value.basePathIndex) {
                result = -1;
            }
            else {
                if (this.basePathIndex > value.basePathIndex) {
                    result = 1;
                }
            }
        }

        return result;
    }

    @Override
    public boolean equals(final Object object) {

        if (null == object) {
            return false;
        }

        if (object == this) {
            return true;
        }

        if (object.getClass() != getClass()) {
            return false;
        }

        JarInfo jarInfo = (JarInfo) object;
        if (0 == this.compareTo(jarInfo)) {
            return true;
        }
        return false;
    }
}
