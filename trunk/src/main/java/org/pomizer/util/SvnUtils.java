package org.pomizer.util;

import java.io.IOException;
import java.io.InputStream;

import org.dom4j.Document;
import org.dom4j.DocumentException;

public class SvnUtils {
    
    public static Document getChangeset(final String rootFolder) throws IOException, DocumentException {
        Document result = null;
        final String commandLine = JavaUtils.adjustCommandLine(
                String.format("svn status --xml \"%s\"" , rootFolder));
        
        final Runtime runtime = Runtime.getRuntime();
        
        final Process process = runtime.exec(commandLine);
        InputStream inputStream = process.getInputStream();
        result = XmlUtils.loadXmlDocument(inputStream);
        return result;
    }

}
