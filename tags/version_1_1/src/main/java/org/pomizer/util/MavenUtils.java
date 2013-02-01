package org.pomizer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;

import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.tree.DefaultElement;
import org.jaxen.JaxenException;
import org.jaxen.SimpleNamespaceContext;
import org.jaxen.XPath;
import org.jaxen.dom4j.Dom4jXPath;
import org.pomizer.constant.CompilationConstants;
import org.pomizer.constant.XmlConstants;
import org.pomizer.model.Dependency;
import org.pomizer.render.PomRenderer;

public class MavenUtils {

    private static final String MANUAL_GROUP_ID = "manual";

    public static void savePomFile(final List<Dependency> dependencies, final String projectName,
            final String pomFileName, final String sourcesPath) {
        FileWriter fileWritter = null;
        try {
            fileWritter = new FileWriter(new File(pomFileName));
            PomRenderer.writeHeaderToPomFile(fileWritter, projectName);
            PomRenderer.writeSourcesPathToPomFile(fileWritter, sourcesPath);
            PomRenderer.writeDependeciesToPomFile(fileWritter, dependencies);
            PomRenderer.writeFooterToPomFile(fileWritter);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            if (null != fileWritter) {
                try {
                    fileWritter.flush();
                    fileWritter.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static DefaultElement readPomDependencies(final Document pomDocument, final List<Dependency> dependencies) {

        DefaultElement dependenciesNode = null;
        
        try {

            HashMap map = new HashMap();
            map.put("default", XmlConstants.POM_DEFAULT_NAMESPACE);

            XPath xpath = new Dom4jXPath("/default:project/default:dependencies");
            xpath.setNamespaceContext(new SimpleNamespaceContext(map));
            
            dependenciesNode = (DefaultElement)xpath.selectSingleNode(pomDocument);
            readDependenciesListFromXmlNode(dependencies, dependenciesNode);
        }
        catch (JaxenException e) {
            e.printStackTrace();
        }
        
        return dependenciesNode;
    }

    private static void readDependenciesListFromXmlNode(final List<Dependency> dependencies, 
            final DefaultElement dependenciesNode) {
        
        dependencies.clear();
        
        if (null != dependenciesNode) {
            for (int i = 0; i < dependenciesNode.nodeCount(); i++) {
                Node dependecyNode = (Node) dependenciesNode.node(i);
                if (Node.ELEMENT_NODE == dependecyNode.getNodeType()) {
                    Dependency dependency = XmlUtils.readDependencyFromXml(dependecyNode, true);
                    if (-1 == dependencies.indexOf(dependency)) {
                        dependencies.add(dependency);
                    }
                }
            }
        }
    }
    

    public static void mergeDependencies(final DefaultElement dependenciesNode, final List<Dependency> dependencies) 
            throws Exception {
        
        List<Dependency> pomDependeciesList = new ArrayList<Dependency>();
        readDependenciesListFromXmlNode(pomDependeciesList, dependenciesNode);

        int pomDependenciesIndex = pomDependeciesList.size() - 1; 
        for (int i = dependenciesNode.nodeCount() - 1; i >= 0; i--) {
            Node dependecyNode = (Node) dependenciesNode.node(i);
            if (Node.ELEMENT_NODE == dependecyNode.getNodeType()) {
                if (pomDependenciesIndex < 0) {
                    throw new Exception("Unsynchronized dependencies list with XML on index: " + i);
                }
                Dependency dependency = pomDependeciesList.get(pomDependenciesIndex);
                if (-1 == dependencies.indexOf(dependency)) {
                    dependecyNode.detach();
                    pomDependeciesList.remove(pomDependenciesIndex);
                }
                pomDependenciesIndex--;
            }
        }
            
        for (int i = 0; i < dependencies.size(); i++) {
            Dependency dependency = dependencies.get(i);
            if (-1 == pomDependeciesList.indexOf(dependency)) {
                XmlUtils.addDependencyToXmlParent(dependenciesNode, dependency);
                pomDependeciesList.add(dependency);
            }
        }
    }
    
    private static String buildMavenCommandLine(final String action, final String parameters) {
        String commandLine = "mvn " + action + " ";
        if (!StringUtils.isNullOrEmpty(parameters)) {
            commandLine += parameters;
        }
        if (JavaUtils.isWindows()) {
            commandLine = "cmd /c " + commandLine;
        }
        return commandLine;
    }
    
    public static void executeCleanTask(final String pomFileName) {
        JavaUtils.printToConsole("Cleaning compiled sources...");
        String commandLine = buildMavenCommandLine("clean", String.format("-f \"%s\"", pomFileName));
        
        Runtime runtime = Runtime.getRuntime();
        Process proc;
        try {
            proc = runtime.exec(commandLine);
            InputStream in = proc.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            while (null != br.readLine());
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    
    public static boolean compilePomFile(final String pomFileName, final List<String> missingPackageErrors,
            final List<String> missingClassErrors, List<SimpleEntry<String, String>> missingClassWithPackageErrors) {
        
        boolean result = true;

        JavaUtils.printToConsole("Compiling POM file...");
        String commandLine = buildMavenCommandLine("compile", String.format("-f \"%s\"", pomFileName));
        
        Runtime runtime = Runtime.getRuntime();
        Process proc;
        try {
            proc = runtime.exec(commandLine);
            InputStream in = proc.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            boolean foundCompilationErrorSection = false;
            String line = null;

            while ((null != (line = br.readLine())) && !foundCompilationErrorSection) {
                foundCompilationErrorSection = CompilationConstants.COMPILATION_ERRORS_SECTION_START.matcher(
                        line).matches();
            }
            
            if (foundCompilationErrorSection) {
                result = false;
                
                while ((null != line) && !CompilationConstants.ERROR_LINE.matcher(line).matches()) {
                    line = br.readLine();
                }
                
                while ((null != line) && CompilationConstants.ERROR_LINE.matcher(line).matches()) {
                    StringBuilder sb = new StringBuilder(line);
                    while ((null != (line = br.readLine())) && 
                            !CompilationConstants.ANY_MESSAGE_TYPE_LINE.matcher(line).matches()) {
                        sb.append(' ');
                        sb.append(line);
                    }
                    
                    processErrorPatterns(sb.toString(), missingPackageErrors, missingClassErrors, 
                            missingClassWithPackageErrors);
                }
                
                while (null != (line = br.readLine()));
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
    
    private static void processErrorPatterns(final String errorLine, final List<String> missingPackageErrors,
            final List<String> missingClassErrors, final List<SimpleEntry<String, String>> missingClassWithPackageErrors) {
        
        Matcher matcher;
        
        if ((matcher = CompilationConstants.CANNOT_FIND_PACKAGE.matcher(errorLine)).find()) {
            if (-1 == missingPackageErrors.indexOf(matcher.group(1))) {
                missingPackageErrors.add(matcher.group(1));
            }
        }
        else if ((matcher = CompilationConstants.CANNOT_FIND_SYMBOL.matcher(errorLine)).find()) {
            if (3 == matcher.groupCount()) {
                addFullClassName(matcher.group(3), missingClassErrors, missingClassWithPackageErrors);
            }
        } 
        else if ((matcher = CompilationConstants.CANNOT_FIND_CLASS_WITH_PACKAGE.matcher(errorLine)).find()) {
            if (2 == matcher.groupCount()) {
                addClassWithPackage(matcher.group(2), matcher.group(1), missingClassWithPackageErrors);
            }
        } 
        else if ((matcher = CompilationConstants.CANNOT_FIND_CLASS.matcher(errorLine)).find()) {
            addMissingClass(matcher.group(1), missingClassErrors);
        }
        else if ((matcher = CompilationConstants.CANNOT_ACCESS_CLASS.matcher(errorLine)).find()) {
            addFullClassName(matcher.group(1), missingClassErrors, missingClassWithPackageErrors);
        }
        else {
            JavaUtils.printToConsole("Not processed error: \"" + errorLine + "\"");
        }
    }

    private static void addFullClassName(final String fullClassName,
            final List<String> missingClassErrors, final List<SimpleEntry<String, String>> missingClassWithPackageErrors) {
        final String packageName = ClassUtils.getPackageFromFullName(fullClassName);
        final String className = ClassUtils.getClassNameFromFullName(fullClassName);
        
        if (StringUtils.isNullOrEmpty(packageName)) {
            addMissingClass(className, missingClassErrors);
        }
        else {
            addClassWithPackage(packageName, className, missingClassWithPackageErrors);
        }
    }

    private static void addMissingClass(final String className, final List<String> missingClassErrors) {
        if (-1 == missingClassErrors.indexOf(className)) {
            missingClassErrors.add(className);
        }
    }

    private static void addClassWithPackage(final String packageName, final String className,
            final List<SimpleEntry<String, String>> missingClassWithPackageErrors) {
        
        AbstractMap.SimpleEntry<String, String> classWithPackage = new SimpleEntry<String, String>(
                className, packageName);
        if (-1 == missingClassWithPackageErrors.indexOf(classWithPackage)) {
            missingClassWithPackageErrors.add(classWithPackage);
        }
    }
    
    public static Dependency createDependecy(final String basePath, final String relativeFileName, final String version) {

        Dependency result = null;
        
        File jarFilePath = new File(basePath, relativeFileName);
        
        String groupId = MANUAL_GROUP_ID + "." + StringUtils.convertPathToObjectName(jarFilePath.getParent());
        String fileName = jarFilePath.getName();
        fileName = fileName.substring(0, fileName.length() - ".jar".length());
        String artifactId = fileName;
        
        result = new Dependency();
        result.groupId = groupId;
        result.artifactId = artifactId;
        result.version = version;
        
        return result;
    }

    public static Dependency installJarFile(final String version, final String basePath, final String relativeFileName)
            throws Exception {
        Dependency result = null;

        boolean successful = false;

        File jarFilePath = new File(basePath, relativeFileName);
        if (!jarFilePath.exists()) {
            throw new Exception("Jar file \"" + jarFilePath.getAbsolutePath() + "\" doesn\'t exist");
        }
        
        result = createDependecy(basePath, relativeFileName, version);

        String commandLine = buildMavenCommandLine(
                "install:install-file",
                String.format("-Dfile=\"%s\" -DgroupId=%s -DartifactId=%s -Dversion=%s -Dpackaging=jar",
                        jarFilePath.getAbsolutePath(), result.groupId, result.artifactId, result.version));

        Runtime runtime = Runtime.getRuntime();
        Process proc;
        try {
            proc = runtime.exec(commandLine);
            InputStream in = proc.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = null;

            while (null != (line = br.readLine())) {
                if (line.startsWith(CompilationConstants.BUILD_SUCCESSFUL)) {
                    successful = true;
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        if (!successful) {
            throw new Exception("Installation of \"" + jarFilePath.getAbsolutePath() + "\" failed");
        }


        return result;
    }
}
