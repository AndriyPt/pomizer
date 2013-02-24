package org.pomizer.comparator;

import java.util.Comparator;

import org.pomizer.model.RawClassInfo;

public class RawClassInfoComparator implements Comparator<RawClassInfo> {

    public int compare(final RawClassInfo o1, final RawClassInfo o2) {
        
        if (null == o1) {
            if (null == o2) {
                return 0;
            }
            return -1;
        }
        
        int result = o1.compareTo(o2);
        
        if (0 == result) {
            if (null == o1.packageInfo) {
                if (null != o2.packageInfo) {
                    result = -1;
                }
            }
            else {
                result = o1.packageInfo.compareTo(o2.packageInfo);
            }
        }
        
        return result;
    }
}
