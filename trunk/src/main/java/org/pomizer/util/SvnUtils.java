package org.pomizer.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.pomizer.exception.ApplicationException;

public class SvnUtils {
    
    private static final String STATUS_TAG = "<status>";
    
    private static String readInputStream(final InputStream inputStream) throws IOException {
        
        BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
        StringBuffer sb = new StringBuffer();
        String line;
        while (null != (line = br.readLine())) {
            sb.append(line);
        }
        inputStream.close();
        return sb.toString();
    }
    
    public static Document getChangeset(final String rootFolder) 
            throws IOException, DocumentException, ApplicationException {
        
        Document result = null;
        final String commandLine = JavaUtils.adjustCommandLine(
                String.format("svn status --xml \"%s\"" , rootFolder));
        
        final Runtime runtime = Runtime.getRuntime();
        
        final Process process = runtime.exec(commandLine);
        final String outputText = readInputStream(process.getInputStream());
        
        final String errorMessageFormat = "Error during execution of SVN command. \n" 
                + "Make sure that SVN binary is in the system search path. \n" 
                + "Command output: \n"
                + "%s";
        
        if (-1 == outputText.indexOf(STATUS_TAG)) {
            final String errorText = readInputStream(process.getErrorStream());
            if (StringUtils.isNullOrEmptyOrBlank(errorText)) {
                throw new ApplicationException(String.format(errorMessageFormat, outputText));
            }
            else {
                throw new ApplicationException(String.format(errorMessageFormat, errorText));
            }
        }
        
        result = XmlUtils.loadXmlDocumentFromString(outputText);
        
        return result;
    }

}
