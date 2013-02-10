package org.pomizer.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import sun.misc.BASE64Encoder;

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

    public static String getPathToFileInCurrentDirectory(final String filename) {
        return FilenameUtils.concat(getCurrentDirectory(), filename);
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
    
    public static void addFilesToExistingZip(final File jarFile, 
            final Map<String, List<String>> files) throws IOException {
        
        final File sourceTempFile = File.createTempFile("jar_source_temp_file_", null);
        sourceTempFile.delete();
        
        FileUtils.copyFile(jarFile, sourceTempFile);
        
        byte[] buffer = new byte[1024];

        ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(sourceTempFile));
        ZipOutputStream zipOutputStream = new ZipOutputStream(new FileOutputStream(jarFile));
        zipOutputStream.setLevel(ZipOutputStream.STORED);

        ZipEntry entry = zipInputStream.getNextEntry();
        while (null != entry) {
            String name = entry.getName();
            boolean fileChanged = false;
            for (String parentPath : files.keySet()) {
                for (String fileName : files.get(parentPath)) { 
                    if (adjustJarFileName(fileName).equals(name)) {
                        fileChanged = true;
                        break;
                    }
                }
            }
            if (!fileChanged) {
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
            for (String fileName : files.get(parentPath)) { 
                InputStream in = new FileInputStream(FilenameUtils.concat(parentPath, fileName));
                zipOutputStream.putNextEntry(new ZipEntry(adjustJarFileName(fileName)));
                int len;
                while ((len = in.read(buffer)) > 0) {
                    zipOutputStream.write(buffer, 0, len);
                }
                zipOutputStream.closeEntry();
                in.close();
            }
        }
        zipOutputStream.close();
        sourceTempFile.delete();
    }

    private static String adjustJarFileName(final String fileName) {
        if (File.separatorChar == '/') {
            return fileName;
        }
        return fileName.replace(File.separatorChar, '/');
    }
    
    public static void downloadUrl(final String url) throws IOException {
        final URL downloader = new URL(url);
        final URLConnection urlConnection = downloader.openConnection();
        if (null != downloader.getUserInfo()) {
            BASE64Encoder encoder = new BASE64Encoder();
            final String basicAuth = "Basic " + new String(encoder.encode(
                    downloader.getUserInfo().getBytes()));
            urlConnection.setRequestProperty("Authorization", basicAuth);
        }
        final BufferedReader inputStream = new BufferedReader(new InputStreamReader(
                urlConnection.getInputStream()));
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
