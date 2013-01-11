package org.pomizer.model;

public class ClassInfo implements Comparable<ClassInfo> {

    public String name;

    public int[] jarFileIndeces;

    public int packageIndex;

    public int compareTo(final ClassInfo value) {

        if (null == value) {
            return -1;

        }

        if (null == this.name) {
            return 1;
        }

        return this.name.compareTo(value.name);

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

        ClassInfo classInfo = (ClassInfo) object;
        if (0 == this.compareTo(classInfo)) {
            return true;
        }
        return false;
    }
}
