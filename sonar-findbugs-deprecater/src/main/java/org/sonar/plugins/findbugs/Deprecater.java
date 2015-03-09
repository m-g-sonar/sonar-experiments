package org.sonar.plugins.findbugs;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

public class Deprecater {

  public static final String PATH_TO_DEPRECATED_IDS = "src/main/resources/deprecated_findBugs_ids.txt";
  public static final String PATH_TO_RULE_HTML_DESCRIPTIONS = "../../sonar-findbugs/src/main/resources/org/sonar/l10n/findbugs/rules/findbugs";
  public static final String PATH_TO_RULE_FILE = "../../sonar-findbugs/src/main/resources/org/sonar/plugins/findbugs/rules.xml";
  private static final String RESULTS_FOLDER = "target/results";

  public static void main(String[] args) {
    Multimap<String, String> deprecations = ArrayListMultimap.create();
    try {
      deprecations = getDeprecations(PATH_TO_DEPRECATED_IDS);
    } catch (IOException e) {
      System.err.println("Unable to read file containing deprectated ids");
      return;
    }

    Map<String, File> htmlFileByFindbugsRule = getHTMLFiles();
    File existingRules = new File(PATH_TO_RULE_FILE);

    if (existingRules.exists() && !deprecations.isEmpty() && !htmlFileByFindbugsRule.isEmpty()) {
      try {
        updateHTMLFiles(deprecations, htmlFileByFindbugsRule);
      } catch (IOException e) {
        System.err.println("Unable to update HTML descriptions");
        return;
      }
      try {
        updateRuleFile(deprecations, existingRules);
      } catch (IOException e) {
        System.err.println("Unable to add deprecation tags to rule file");
        return;
      }
    }
  }

  public static Multimap<String, String> getDeprecations(String path) throws IOException {
    Multimap<String, String> results = ArrayListMultimap.create();

    File file = new File(path);
    if (file.exists() && file.isFile()) {
      for (String line : Files.readLines(file, Charset.forName("UTF-8"))) {
        if (StringUtils.isNotBlank(line) && line.contains("\t")) {
          String[] split = line.split("\t");
          String findbugsRule = split[0];
          String squidRules = split[1];
          if (squidRules.contains(",")) {
            results.putAll(findbugsRule, Arrays.asList(squidRules.split(",")));
          } else {
            results.put(findbugsRule, squidRules);
          }
        }
      }
      System.out.println(results.keySet().size() + " rules to tag as deprecated");
    }

    return results;
  }

  private static void updateHTMLFiles(Multimap<String, String> deprecations, Map<String, File> htmlFileByFindbugsRule) throws IOException {
    File resultDir = getResultDir();
    File targetDirectory = new File(resultDir, "descriptions");
    if (targetDirectory.exists()) {
      FileUtils.deleteDirectory(targetDirectory);
    }
    targetDirectory.mkdirs();

    int htmlFileUpdated = 0;

    for (String findbugRule : deprecations.keySet()) {
      File htmlDescriptionFile = htmlFileByFindbugsRule.get(findbugRule);
      if (htmlDescriptionFile == null) {
        System.err.println("No html description file corresponding to the rule " + findbugRule);
        return;
      }
      if (updateHTMLFile(htmlDescriptionFile, targetDirectory, deprecations.get(findbugRule))) {
        htmlFileUpdated++;
      }
    }
    System.out.println(htmlFileUpdated + " html description files have been updated");
  }

  private static boolean updateHTMLFile(File htmlDescriptionFile, File targetDirectory, Collection<String> rules) throws IOException {
    String htmlFilename = htmlDescriptionFile.getName();
    File newDescription = new File(targetDirectory, htmlFilename);
    newDescription.createNewFile();

    boolean alreadyDeprecated = false;
    PrintStream out = new PrintStream(newDescription);
    for (String line : Files.readLines(htmlDescriptionFile, Charset.forName("UTF-8"))) {
      if (line.startsWith("This rule is deprecated")) {
        // do not replace existing message
        alreadyDeprecated = true;
        String message = getDeprecationMessage(rules);
        if (!line.trim().equals(message)) {
          System.out.println("CONFLICT on " + htmlFilename + ": (current) " + line + " << vs >> " + message + " ( new message)");
          System.out.println("What message take ? 0 = current, 1 = new message");
          BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
          int choice = Integer.parseInt(reader.readLine());
          if (choice == 0) {
            out.println(line);
          } else {
            out.println(message);
          }
        } else {
          out.println(line);
        }
      } else {
        out.println(line);
      }
    }
    if (!alreadyDeprecated) {
      out.println();
      out.println("<p>");
      out.println(getDeprecationMessage(rules));
      out.println("</p>");
    }
    out.close();
    return !alreadyDeprecated;
  }

  private static String getDeprecationMessage(Collection<String> rules) {
    return "This rule is deprecated, use " + formatRules(rules) + " instead.";
  }

  /**
   * format : {rule:squid:#####}
   */
  private static String formatRules(Collection<String> rules) {
    String result = "";
    String separator = "";
    for (String rule : rules) {
      String ruleName = rule;
      if (rule.startsWith("RSPEC-")) {
        String ruleNumber = rule.substring(6);
        if (ruleNumber.length() == 3) {
          ruleNumber = "00" + ruleNumber;
        }
        ruleName = "S" + ruleNumber;
      }
      result += separator + "{rule:squid:" + ruleName + "}";
      separator = ", ";
    }
    return result;
  }

  private static void updateRuleFile(Multimap<String, String> deprecations, File existingRules) throws IOException {
    File resultDir = getResultDir();
    File rules = new File(resultDir, "rules.xml");
    if (rules.exists()) {
      rules.delete();
    }
    rules.createNewFile();
    PrintStream out = new PrintStream(rules);

    int newlyDeprecated = 0;
    int deprecatedRules = 0;

    boolean addDeprecated = false;
    String previousLine = "";
    for (String line : Files.readLines(existingRules, Charset.forName("UTF-8"))) {
      if (isRuleStartLine(line)) {
        String ruleName = StringUtils.substringBetween(line, "key=\"", "\">");
        addDeprecated = deprecations.containsKey(ruleName);
      } else if (addDeprecated && isRuleEndLine(line)) {
        if (!previousLine.contains("DEPRECATED")) {
          out.println("    <status>DEPRECATED</status>");
          addDeprecated = false;
          newlyDeprecated++;
        }
        deprecatedRules++;
      }
      previousLine = line;
      out.println(line);
    }
    out.close();
    System.out.println(deprecatedRules + " rules read as deprecated in \"rules.xml\" file");
    System.out.println(newlyDeprecated + " newly deprecated");
  }

  private static boolean isRuleEndLine(String line) {
    return "</rule>".equals(line.trim());
  }

  private static boolean isRuleStartLine(String line) {
    return line.trim().startsWith("<rule");
  }

  private static File getResultDir() {
    File resultDir = new File(RESULTS_FOLDER);
    if (!resultDir.exists()) {
      resultDir.mkdirs();
    }
    return resultDir;
  }

  private static Map<String, File> getHTMLFiles() {
    Map<String, File> results = Maps.newHashMap();
    File directory = new File(PATH_TO_RULE_HTML_DESCRIPTIONS);
    if (directory.exists() && directory.isDirectory()) {
      for (File file : directory.listFiles()) {
        String fileName = file.getName();
        if (fileName.endsWith(".html")) {
          results.put(fileName.substring(0, fileName.length() - 5), file);
        }
      }
    }
    return results;
  }

}
