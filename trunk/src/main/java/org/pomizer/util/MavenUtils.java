package org.pomizer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap;
import java.util.AbstractMap.SimpleEntry;
import java.util.HashMap;
import java.util.List;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
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
    public static void readPomDependencies(final String pomFileName, final List<Dependency> dependencies) {

        SAXReader reader = new SAXReader();
        try {
            Document pomDocument = reader.read(pomFileName);

            HashMap map = new HashMap();
            map.put("default", XmlConstants.POM_DEFAULT_NAMESPACE);

            XPath xpath = new Dom4jXPath("/default:project/default:dependencies/default:dependency");
            xpath.setNamespaceContext(new SimpleNamespaceContext(map));

            List dependenciesNodes = xpath.selectNodes(pomDocument);
            for (int i = 0; i < dependenciesNodes.size(); i++) {
                Node dependecyNode = (Node) dependenciesNodes.get(i);
                Dependency dependency = XmlUtils.readDependencyFromXml(dependecyNode, true);
                if (-1 == dependencies.indexOf(dependency)) {
                    dependencies.add(dependency);
                }
            }
        }
        catch (DocumentException e) {
            e.printStackTrace();
        }
        catch (JaxenException e) {
            e.printStackTrace();
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
            String line = null;

            while (null != (line = br.readLine())) {

                if (line.contains(CompilationConstants.COMPILATION_ERROR_HEADER)) {
                    result = false;
                }

                //TODO: Add logic here to read all errors line till next error or end of section
                if (line.startsWith(CompilationConstants.ERROR_PREFIX)) {
                    int lastDelimiterIndex = line.lastIndexOf(':');
                    line = line.substring(lastDelimiterIndex + 1).trim();

                    if (!compilationErrorPackageDoesntExists(missingPackageErrors, line)) { 
                        if (line.contains(CompilationConstants.SYMBOL_ERROR_MESSAGE)) {
                            line = br.readLine();
                            if (null != line) {

                                br.mark(5000);
                                compilationErrorCannotFindClass(missingClassErrors, line);
                                br.reset();

                                compilationErrorCannotFindClassWithPackage(missingClassErrors,
                                        missingClassWithPackageErrors, br, line);
                                br.reset();

                                compilationErrorCannotFindMethodInInterface(missingClassErrors,
                                        missingClassWithPackageErrors, br, line);
                                br.reset();
                                
                                compilationErrorCannotFindMethodInClass(missingClassErrors,
                                        missingClassWithPackageErrors, br, line);
                                br.reset();
                                
                                compilationErrorCannotFindVariableInInterface(missingClassErrors,
                                        missingClassWithPackageErrors, br, line);
                                br.reset();
                                
                                compilationErrorCannotFindVariableInClass(missingClassErrors,
                                        missingClassWithPackageErrors, br, line);
                                br.reset();
                            }
                        }

                        compilationErrorCannotAccessClass(missingClassWithPackageErrors, br, line);
                    }
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String compilationErrorCannotAccessClass(
            List<SimpleEntry<String, String>> missingClassWithPackageErrors, BufferedReader br, String line)
            throws IOException {
        
        int lastDelimiterIndex;
        
        // Example of situation
        // [ERROR] /C:/test/src/TestClass.java:7: cannot access com.test.second.third.servlet.RequestSupport
        // class file for com.test.second.third.servlet.RequestSupport not found
        if (line.contains(CompilationConstants.CANNOT_ACCESS_ERROR)) {
            line = br.readLine();
            if (line.startsWith(CompilationConstants.CANNOT_ACCESS_ERROR_PREFIX)
                    && line.endsWith(CompilationConstants.CANNOT_ACCESS_ERROR_SUFIX)) {

                String fullClassName = line.substring(
                        CompilationConstants.CANNOT_ACCESS_ERROR_PREFIX.length(),
                        line.length() - CompilationConstants.CANNOT_ACCESS_ERROR_SUFIX.length()).trim();
                lastDelimiterIndex = fullClassName.lastIndexOf('.');
                if (-1 < lastDelimiterIndex) {
                    AbstractMap.SimpleEntry<String, String> classWithPackage = new SimpleEntry<String, String>(
                            fullClassName.substring(lastDelimiterIndex + 1), fullClassName.substring(0,
                                    lastDelimiterIndex));
                    if (-1 == missingClassWithPackageErrors.indexOf(classWithPackage)) {
                        missingClassWithPackageErrors.add(classWithPackage);
                    }
                }
            }
        }
        return line;
    }
    
    //TODO: Update errors parsing algorithm
    private static String compilationErrorPatternParsing(final String missingErrorPrefix, final String locationPrefix, 
            final List<String> missingClassErrors, List<SimpleEntry<String, String>> missingClassWithPackageErrors, 
            BufferedReader br, String line) throws IOException {
        
        // [ERROR] /C:/test/src/TestClass.java:54: cannot find symbol
        // <missingErrorPrefix> <some text>
        // <locationPrefix> com.test.second.third.SomeClass
        if (line.startsWith(missingErrorPrefix)) {
            line = br.readLine();
            if (line.startsWith(locationPrefix)) {
                String fullClassName = line.substring(locationPrefix.length()).trim();

                final String packageName = ClassUtils.getPackageFromFullName(fullClassName);
                final String className = ClassUtils.getClassNameFromFullName(fullClassName);

                if (StringUtils.isNullOrEmpty(packageName)) {
                    if (-1 == missingClassErrors.indexOf(className)) {
                        missingClassErrors.add(className);
                    }
                }
                else {
                    AbstractMap.SimpleEntry<String, String> classWithPackage = new SimpleEntry<String, String>(
                            className, packageName);

                    if (-1 == missingClassWithPackageErrors.indexOf(classWithPackage)) {
                        missingClassWithPackageErrors.add(classWithPackage);
                    }
                }
            }
        }
        return line;
    }
    
    private static String compilationErrorCannotFindVariableInInterface(final List<String> missingClassErrors,
            List<SimpleEntry<String, String>> missingClassWithPackageErrors, BufferedReader br, String line)
            throws IOException {
        
        // [ERROR] /C:/test/src/TestClass.java:54: cannot find symbol
        // symbol  : variable TEST_12
        // location: interface com.test.second.third.SomeInterface
        return compilationErrorPatternParsing(CompilationConstants.SYMBOL_VARIABLE_ERROR_PREFIX, 
                CompilationConstants.SYMBOL_LOCATION_INTERFACE_PREFIX, missingClassErrors, 
                missingClassWithPackageErrors, br, line);
    }
    
    private static String compilationErrorCannotFindVariableInClass(final List<String> missingClassErrors,
            List<SimpleEntry<String, String>> missingClassWithPackageErrors, BufferedReader br, String line)
            throws IOException {
        
        // [ERROR] /C:/test/src/TestClass.java:54: cannot find symbol
        // symbol  : variable TEST_12
        // location: class com.test.second.third.SomeClass
        return compilationErrorPatternParsing(CompilationConstants.SYMBOL_VARIABLE_ERROR_PREFIX, 
                CompilationConstants.SYMBOL_LOCATION_CLASS_PREFIX, missingClassErrors, 
                missingClassWithPackageErrors, br, line);
    }
    

    private static String compilationErrorCannotFindMethodInInterface(final List<String> missingClassErrors,
            List<SimpleEntry<String, String>> missingClassWithPackageErrors, BufferedReader br, String line)
            throws IOException {
        
        // [ERROR] /C:/test/src/TestClass.java:54: cannot find symbol
        // symbol : method send(javax.jms.ObjectMessage)
        // location: interface javax.jms.MessageProducer
        return compilationErrorPatternParsing(CompilationConstants.SYMBOL_METHOD_ERROR_PREFIX, 
                CompilationConstants.SYMBOL_LOCATION_INTERFACE_PREFIX, missingClassErrors, 
                missingClassWithPackageErrors, br, line);
    }
    
    private static String compilationErrorCannotFindMethodInClass(final List<String> missingClassErrors,
            List<SimpleEntry<String, String>> missingClassWithPackageErrors, BufferedReader br, String line)
            throws IOException {
        
        // [ERROR] /C:/test/src/TestClass.java:54: cannot find symbol
        //symbol  : method send(javax.jms.ObjectMessage)
        //location: class javax.jms.MessageProducer
        return compilationErrorPatternParsing(CompilationConstants.SYMBOL_METHOD_ERROR_PREFIX, 
                CompilationConstants.SYMBOL_LOCATION_CLASS_PREFIX, missingClassErrors, 
                missingClassWithPackageErrors, br, line);
    }

    private static String compilationErrorCannotFindClassWithPackage(final List<String> missingClassErrors,
            List<SimpleEntry<String, String>> missingClassWithPackageErrors, BufferedReader br, String line)
            throws IOException {
        
        // Example of situation
        // [ERROR] /C:/test/src/TestClass.java:[5,29] : cannot find symbol
        // symbol : class TestClass
        // location: package com.test.second.third
        if (line.startsWith(CompilationConstants.SYMBOL_CLASS_ERROR_PREFIX)) {
            String className = line.substring(
                    CompilationConstants.SYMBOL_CLASS_ERROR_PREFIX.length()).trim();
            line = br.readLine();
            if (line.startsWith(CompilationConstants.CLASS_LOCATION_PACKAGE_PREFIX)) {
                String packageName = line.substring(
                        CompilationConstants.CLASS_LOCATION_PACKAGE_PREFIX.length()).trim();

                AbstractMap.SimpleEntry<String, String> classWithPackage = new SimpleEntry<String, String>(
                        className, packageName);

                if (-1 == missingClassWithPackageErrors.indexOf(classWithPackage)) {
                    missingClassWithPackageErrors.add(classWithPackage);
                }
            }
            else {
                if (-1 == missingClassErrors.indexOf(className)) {
                    missingClassErrors.add(className);
                }
            }
        }
        return line;
    }

    private static void compilationErrorCannotFindClass(final List<String> missingClassErrors, String line) {
        // Example of situation
        // [ERROR] /C:/test/src/TestClass.java:[42,36] : cannot find symbol
        // symbol: class Action
        if (line.startsWith(CompilationConstants.SYMBOL_ERROR_PREFIX)) {
            String className = line
                    .substring(CompilationConstants.SYMBOL_ERROR_PREFIX.length()).trim();
            if (-1 == missingClassErrors.indexOf(className)) {
                missingClassErrors.add(className);
            }
        }
    }

    private static boolean compilationErrorPackageDoesntExists(final List<String> missingPackageErrors, final String line) {
        
        boolean result = false;
        
        if (line.startsWith(CompilationConstants.PACKAGE_ERROR_PREFIX)
                && line.endsWith(CompilationConstants.PACKAGE_ERROR_SUFIX)) {

            result = true;
            String packageName = line.substring(CompilationConstants.PACKAGE_ERROR_PREFIX.length(),
                    line.length() - CompilationConstants.PACKAGE_ERROR_SUFIX.length()).trim();
            if (-1 == missingPackageErrors.indexOf(packageName)) {
                missingPackageErrors.add(packageName);
            }
        }
        return result;
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
