package org.pomizer.model;

public class IndexInfo {

    public String version;

    public int basePathsCount;
    public int jarsCount;
    public int packagesCount;
    public int classesCount;

    public String[] basePaths;
    public String[] jarNames;
    public int[] jarNamesBasePathIndex;

    public String[] packageNames;
    public int[] packageNamesJarIndex;

    public String[] classNames;
    public int[] classNamesJarIndex;
    public int[] classNamesPackageIndex;

    public IndexInfo(final String version, final int basePathsCount, final int jarsCount, final int packagesCount,
            final int classesCount) {

        this.version = version;

        this.basePathsCount = basePathsCount;
        this.jarsCount = jarsCount;
        this.packagesCount = packagesCount;
        this.classesCount = classesCount;

        this.basePaths = new String[basePathsCount];

        this.jarNames = new String[jarsCount];
        this.jarNamesBasePathIndex = new int[jarsCount];

        this.packageNames = new String[packagesCount];
        this.packageNamesJarIndex = new int[packagesCount];

        this.classNames = new String[classesCount];
        this.classNamesJarIndex = new int[classesCount];
        this.classNamesPackageIndex = new int[classesCount];
    }
}
