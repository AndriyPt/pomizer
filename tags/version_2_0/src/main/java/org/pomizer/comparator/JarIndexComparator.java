package org.pomizer.comparator;

import java.util.Comparator;
import java.util.List;

import org.pomizer.model.RawJarInfo;

public class JarIndexComparator implements Comparator<Integer> {
    
    private List<RawJarInfo> jarNames;
    
    public JarIndexComparator(final List<RawJarInfo> jarNames) {
        
        this.jarNames = jarNames;
    }

    public int compare(Integer o1, Integer o2) {
        int result = 0;
        
        RawJarInfo jarInfo1 = jarNames.get(o1);
        RawJarInfo jarInfo2 = jarNames.get(o2);

        if (jarInfo1.lastModified < jarInfo2.lastModified) {
            result = 1;
        }
        else {
            if (jarInfo1.lastModified > jarInfo2.lastModified) {
                result = -1;
            }
        }
        return result;
    }

}
