package org.pomizer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;

public class JavaUtils {

    public static boolean isWindows() {

        final String OS = System.getProperty("os.name").toLowerCase();

        return (OS.indexOf("win") >= 0);
    }

    public static String getCurrentTime() {
        return new SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Calendar.getInstance().getTime());
    }

    public static void printToConsole(final String message) {
        System.out.println(getCurrentTime() + ": " + message);
    }

    public static String getCurrentDirectory() {
        return new File(".").getAbsolutePath();
    }

    public static String combinePaths(final String path1, final String path2) {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2.getAbsolutePath();
    }

    public static String getPathToFileInCurrentDirectory(final String filename) {
        return combinePaths(getCurrentDirectory(), filename);
    }

    public static boolean containsDirectoriesInPath(final String path, final String... directories) {

        boolean result = false;
        final String normalizedPath = path.toLowerCase();
        for (int i = 0; (i < directories.length) && !result; i++) {
            String normalizedDirectory = directories[i].toLowerCase();
            if (normalizedPath.indexOf(normalizedDirectory + File.separator) > -1) {
                result = true;
            }
        }
        return result;
    }

    public static void checkFileExists(final String fileName) {

        File projectFile = new File(fileName);
        if (!projectFile.exists()) {
            JavaUtils.printToConsole(String.format("File \"%s\" doesn\'t exists \n", fileName));
            System.exit(1);
        }

        if (!projectFile.isFile()) {
            JavaUtils.printToConsole(String.format("Entiny \"%s\" is not a file \n", fileName));
            System.exit(1);
        }

        if (!projectFile.canRead()) {
            JavaUtils.printToConsole(String.format("Could not read following file \"%s\" \n", fileName));
            System.exit(1);
        }
    }

    public static void checkDirectoryExists(final String directoryName) {

        File directoryFile = new File(directoryName);
        if (!directoryFile.exists()) {
            JavaUtils.printToConsole(String.format("Directory \"%s\" doesn\'t exists \n", directoryName));
            System.exit(1);
        }

        if (!directoryFile.isDirectory()) {
            JavaUtils.printToConsole(String.format("Entiny \"%s\" is not a directory \n", directoryName));
            System.exit(1);
        }

        if (!directoryFile.canRead()) {
            JavaUtils.printToConsole(String.format("Could not read following directory \"%s\" \n", directoryName));
            System.exit(1);
        }
    }
    
    public static String adjustCommandLine(final String commandLine) {
        
        String result = commandLine;
        
        if (isWindows()) {
            result = "cmd /c " + commandLine;
        }
        return result;
    }
    
    public static boolean isParentOf(final String directoryPath, final String fileName) 
            throws IOException {

        boolean result = false;
        
        File directoryFile = new File(directoryPath).getCanonicalFile();
        File childFile = new File(fileName).getCanonicalFile();

        File parentFile = childFile;
        while (null != parentFile) {
            if (directoryFile.equals(parentFile)) {
                result = true;
                break;
            }
            parentFile = parentFile.getParentFile();
        }
        return result;
    }
    
    public static String ensurePathHasSeparatorAtTheEnd(final String path) {
        String result = path;
        if (!StringUtils.isNullOrEmpty(result)) {
            if (!result.endsWith(File.separator)) {
                result += File.separator;
            }
        }
        return result;
    }
    
    public static boolean isFile(final String path) {
        final File pathFile = new File(path); 
        return pathFile.isFile();
    }
    
    public static boolean isTheSamePath(final String first, final String second) throws IOException {
        final File firstFile = new File(first);
        final File secondFile = new File(second);
        return firstFile.getCanonicalPath().equals(secondFile.getCanonicalPath());
    }
    
    public static String getParentFolder(final String path) {
        final File pathFile = new File(path);
        return pathFile.getParentFile().getAbsolutePath();
    }
    
    public static String getFileName(final String fullFileName) {
        final File file = new File(fullFileName);
        return file.getName();
    }
    
    
    public static String getFileNameWithoutExtension(final String fullFileName) {
        
        String result = getFileName(fullFileName);
        int index = result.lastIndexOf('.');
        
        if (-1 != index) {
            result = result.substring(0, index);
        }
        
        return result;
    }

    public static void addFilesToExistingZip(String jarPath, Map<String, Collection<File>> files) throws IOException {
        
        final File tempFile = File.createTempFile("jar_temp_file", null);
        tempFile.delete();
        
        final File jarFile = new File(jarPath);
        FileUtils.copyFile(jarFile, tempFile);

        byte[] buffer = new byte[1024];

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(tempFile));
        //TODO: Check if we can output to real file under JBoss
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(jarFile));

        ZipEntry entry = zipInputStream.getNextEntry();
        while (null != entry) {
            String name = entry.getName();
            boolean fileChanged = true;
            for (String parentPath : files.keySet()) {
                for (File file : files.get(parentPath)) { 
                    if (file.getName().equals(name)) {
                        fileChanged = false;
                        break;
                    }
                }
            }
            if (fileChanged) {
                zipOutputStream.putNextEntry(new ZipEntry(name));
                int length;
                while ((length = zipInputStream.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, length);
                }
            }
            entry = zipInputStream.getNextEntry();
        }
        
        zipInputStream.close();
        
        for (String parentPath : files.keySet()) {
            for (File file : files.get(parentPath)) { 
                InputStream in = new FileInputStream(file);
                zipOutputStream.putNextEntry(new ZipEntry(file.getName()));
                int len;
                while ((len = in.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, len);
                }
                zipOutputStream.closeEntry();
                in.close();
            }
        }
        zipOutputStream.close();
        tempFile.delete();
    }
    
    public static void downloadUrl(final String url) throws IOException {
        final URL downloader = new URL(url);
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(
                downloader.openStream()));
        while (null != inputStream.readLine());
        inputStream.close();        
    }

    public static void executeCommand(final String commandLine) {
        Runtime runtime = Runtime.getRuntime();
        Process proc;
        try {
            proc = runtime.exec(adjustCommandLine(commandLine));
            InputStream in = proc.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while (null != (line = br.readLine())) {
                printToConsole(line);
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
