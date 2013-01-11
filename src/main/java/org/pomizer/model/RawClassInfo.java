package org.pomizer.model;

import java.util.ArrayList;
import java.util.List;

public class RawClassInfo extends ClassInfo {

    public List<RawJarInfo> jarInfoList = new ArrayList<RawJarInfo>();

    public PackageInfo packageInfo;
}
