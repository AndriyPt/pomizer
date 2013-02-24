package org.pomizer.model;

import java.util.ArrayList;
import java.util.List;

public class PackageInfo implements Comparable<PackageInfo> {

    public String name;

    public List<Integer> jarIndeces = new ArrayList<Integer>();

    public PackageInfo() {
    }

    public PackageInfo(final String nameParameter) {
        this.name = nameParameter;
    }

    public int compareTo(final PackageInfo value) {

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

        PackageInfo packageInfo = (PackageInfo) object;
        if (0 == this.compareTo(packageInfo)) {
            return true;
        }
        return false;
    }
}
