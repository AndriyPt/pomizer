package org.pomizer.render;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import org.pomizer.constant.GlobalSettings;
import org.pomizer.constant.XmlConstants;
import org.pomizer.model.Dependency;

public class PomRenderer {

    public static void writeHeaderToPomFile(final FileWriter fileWritter, final String projectName) throws IOException {

        fileWritter
                .write(String
                        .format("<project xmlns=\""
                                + XmlConstants.POM_DEFAULT_NAMESPACE
                                + "\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" \n"
                                + "xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd\"> \n"
                                + "  <modelVersion>4.0.0</modelVersion> \n" + "  <groupId>org.my.projects</groupId> \n"
                                + "  <artifactId>%s</artifactId> \n" + "  <packaging>jar</packaging> \n"
                                + "  <version>1.0-SNAPSHOT</version> \n" + "  <name>%s</name> \n"
                                + "  <url>http://maven.apache.org</url> \n", projectName, projectName));

    }

    public static void writeNotResolvedDependenciesToPomFile(final FileWriter fileWritter,
            final List<String> notResolvedDependencies) throws IOException {

        fileWritter.write("\n  <!-- Not resolved JARs \n");
        for (int i = 0; i < notResolvedDependencies.size(); i++) {
            fileWritter.write("    " + notResolvedDependencies.get(i) + "\n");
        }
        fileWritter.write("  --> \n\n");
    }

    public static void writeDependeciesToPomFile(final FileWriter fileWritter, final List<Dependency> dependencies)
            throws IOException {

        fileWritter.write("  <dependencies> \n");
        for (int i = 0; i < dependencies.size(); i++) {
            Dependency dependency = dependencies.get(i);
            fileWritter.write("    <dependency> \n");
            fileWritter.write("      <groupId>" + dependency.groupId + "</groupId>\n");
            fileWritter.write("      <artifactId>" + dependency.artifactId + "</artifactId>\n");
            fileWritter.write("      <version>" + dependency.version + "</version>\n");
            fileWritter.write("    </dependency> \n");
        }
        fileWritter.write("  </dependencies> \n");
    }

    public static void writeFooterToPomFile(final FileWriter fileWritter) throws IOException {
        fileWritter.write("</project>\n");
    }

    public static void writeSourcesPathToPomFile(final FileWriter fileWritter, final String sourcesPath)
            throws IOException {

        fileWritter.write("  <build>\n");
        if ((null != sourcesPath) && ("" != sourcesPath.trim())) {
            fileWritter.write("    <sourceDirectory>" + sourcesPath + "</sourceDirectory>\n");
        }
        fileWritter.write("    <plugins>\n");
        fileWritter.write("      <plugin>\n");
        fileWritter.write("        <groupId>org.apache.maven.plugins</groupId>\n");
        fileWritter.write("        <artifactId>maven-compiler-plugin</artifactId>\n");
        fileWritter.write("        <version>3.0</version>\n");
        fileWritter.write("        <configuration>\n");
        fileWritter.write("          <source>" + GlobalSettings.JAVA_VERSION + "</source>\n");
        fileWritter.write("          <target>" + GlobalSettings.JAVA_VERSION + "</target>\n");
        fileWritter.write("        </configuration>\n");
        fileWritter.write("      </plugin>\n");
        fileWritter.write("    </plugins>\n");
        fileWritter.write("  </build>\n");
    }
}
