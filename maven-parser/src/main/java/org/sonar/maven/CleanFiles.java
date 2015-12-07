/*
 * Sonar Maven XSD Parser
 * Copyright (C) 2015 SonarSource
 * sonarqube@googlegroups.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.maven;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

public class CleanFiles {

  private static final Logger LOG = LoggerFactory.getLogger(CleanFiles.class);

  private static final String SOURCE_FOLDER = "target/generated-sources/xjc/org/sonar/maven/model/maven2";
  private static final String RESULT_FOLDER = "target/generated-sources/cleaned";
  private static boolean previousLineIsEmpty = false;

  public static void main(String[] args) {
    String root = Paths.get("").toAbsolutePath().toString();
    File sourceFolder = new File(root + "/" + SOURCE_FOLDER);
    File resultFolder = new File(root + "/" + RESULT_FOLDER);
    new CleanFiles().clean(sourceFolder, resultFolder);
  }

  public void clean(File sourceFolder, File resultFolder) {
    if (resultFolder.exists()) {
      try {
        FileUtils.deleteDirectory(resultFolder);
      } catch (IOException e) {
        throw new IllegalStateException("unable to clean destination folder");
      }
    }
    if (!resultFolder.mkdirs()) {
      throw new IllegalStateException("unable to create destination folder");
    }
    if (sourceFolder.isDirectory()) {
      for (File file : sourceFolder.listFiles()) {
        cleanFile(resultFolder, file);
      }
    }
  }

  private static void cleanFile(File resultFolder, File file) {
    try (
      BufferedReader reader = new BufferedReader(new FileReader(file));
      BufferedWriter writer = new BufferedWriter(new FileWriter(new File(resultFolder, file.getName())))) {

      String line;

      do {
        line = reader.readLine();
        if (!isJavaDoc(line)) {

          line = refactorXmlAnyType(reader, writer, line);
          line = skipSetter(reader, line);
          line = skipDefaultValue(reader, line);
          line = allClassesExtendsLocatedTree(line);
          line = refactorListGetters(line);
          line = useLocatedAttributeInsteadOfString(line);
          line = handleDiamondOperator(line);

          writeLine(writer, line);
        }
      } while (line != null);
    } catch (Exception e) {
      LOG.error("can not find file " + file.getName());
    }
  }

  private static String useLocatedAttributeInsteadOfString(String line) {
    return line.replaceAll("String", "LocatedAttribute");
  }

  private static String allClassesExtendsLocatedTree(String line) {
    if (line.startsWith("public class") || line.trim().startsWith("public static class")) {
      return line.replaceAll("\\{", "") + "extends LocatedTreeImpl {";
    }
    return line;
  }

  private static String refactorListGetters(String line) {
    if (line.contains("public List") && line.contains("get")) {
      return line.substring(0, line.indexOf("get")) + "getValues() {";
    }
    return line;
  }

  private static String skipDefaultValue(BufferedReader reader, String line) throws IOException {
    if (line.trim().contains("@XmlElement(defaultValue =")) {
      return reader.readLine();
    }
    return line;
  }

  private static String handleDiamondOperator(String line) {
    String newArrayList = "new ArrayList<";
    int index = line.indexOf(newArrayList);
    if (index < 0) {
      return line;
    }
    return line.substring(0, index + newArrayList.length()) + ">();";
  }

  private static String skipSetter(BufferedReader reader, String line) throws IOException {
    String nextLine = line;
    if (nextLine.contains("void set")) {
      do {
        nextLine = reader.readLine();
      } while (!nextLine.trim().equals("}"));
      nextLine = reader.readLine();
    }
    return nextLine;
  }

  private static String refactorXmlAnyType(BufferedReader reader, BufferedWriter writer, String line) throws IOException {
    String nextLine = line;
    // skip imports
    if (nextLine.trim().contains("org.w3c.dom.Element") || nextLine.trim().contains("javax.xml.bind.annotation.XmlAnyElement")) {
      return reader.readLine();
    }
    if (nextLine.trim().startsWith("@XmlType")) {
      String previousXmlTypeLine = line;
      nextLine = reader.readLine();
      if (nextLine.trim().contains("any")) {
        writeLine(writer, previousXmlTypeLine.substring(0, previousXmlTypeLine.indexOf("@XmlType")) + "@XmlType(name = \"\")");
        // skip 2 lines
        reader.readLine();
        nextLine = reader.readLine();
        if (nextLine.trim().startsWith("public static class")) {
          writeLine(writer, nextLine.replaceAll("\\{", "") + "extends LocatedElements {");
          int closeBraceCount = 0;
          do {
            nextLine = reader.readLine();
            if (!isJavaDoc(nextLine) && nextLine.trim().contains("}")) {
              closeBraceCount++;
            }
          } while (closeBraceCount < 3);
          return nextLine;
        }
      } else {
        // write the annotation
        writeLine(writer, previousXmlTypeLine);
        return nextLine;
      }
    }
    return nextLine;
  }

  private static void writeLine(BufferedWriter writer, String line) throws IOException {
    if (line.isEmpty() && previousLineIsEmpty) {
      return;
    }
    writer.write(line.replaceAll("  ", " "));
    writer.newLine();
    previousLineIsEmpty = line.isEmpty();
  }

  private static boolean isJavaDoc(String line) {
    String trimedLine = line.trim();
    return trimedLine.startsWith("/*") || trimedLine.endsWith("*/") || trimedLine.startsWith("*");
  }

}
