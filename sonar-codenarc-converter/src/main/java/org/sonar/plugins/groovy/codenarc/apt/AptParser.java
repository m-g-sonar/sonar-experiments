package org.sonar.plugins.groovy.codenarc.apt;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.sonar.plugins.groovy.codenarc.RuleParameter;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

public class AptParser {

  private static final String LIST_PREFIX = "* {{";
  private static final String NEW_RULE_PREFIX = "* ";
  private static final String PARAMETER_START_SEPARATOR = "\\*(-)+\\+(-)+\\+(-)+\\+";
  private static final String PARAMETER_SEPARATOR = "\\+((-)+\\+)+";
  private static final String PARAMETER_CONTENT = "(\\|(.)*)+";
  private static final String EXAMPLE_SEPARATOR_1 = "(-)+";
  private static final String EXAMPLE_SEPARATOR_2 = "\\+(-)+";

  public Map<String, AptResult> parse(List<File> files) throws Exception {
    Map<String, AptResult> results = Maps.newHashMap();
    if (!files.isEmpty()) {
      for (File file : files) {
        Map<String, AptResult> parametersByFile = readFile(file);
        mergeParameters(results, parametersByFile);
      }
    }
    return results;
  }

  private Map<String, AptResult> readFile(File file) throws Exception {
    Map<String, AptResult> results = Maps.newHashMap();
    List<String> lines = Files.readAllLines(file.toPath(), Charset.defaultCharset());

    boolean inRule = false;
    boolean inParameters = false;
    boolean inExample = false;
    boolean inDescription = false;
    String currentRule = null;
    AptResult currentResult = null;
    RuleParameter currentParameter = null;
    int[] splits = new int[3];
    for (int index = 0; index < lines.size(); index++) {
      String fullLine = lines.get(index);
      String line = fullLine.trim();
      if (line.startsWith(NEW_RULE_PREFIX) && !line.startsWith(LIST_PREFIX) && inRule) {
        results.put(currentRule, currentResult);
        inRule = false;
        inDescription = false;
        currentRule = null;
        currentResult = null;
      }
      if (!inRule && line.startsWith(NEW_RULE_PREFIX)) {
        currentRule = getRuleName(line.trim());
        inRule = currentRule != null;
        if (inRule) {
          currentResult = results.get(currentRule);
          if (currentResult == null) {
            currentResult = new AptResult(currentRule);
          }
        }
      } else if (inRule && !inExample && isExampleSeparator(line)) {
        inExample = true;
        inDescription = false;
        currentResult.description += "<pre>\n";
      } else if (inRule && !inExample && !inDescription && !inParameters && isValidDescriptionLine(line)) {
        inDescription = true;
        if (StringUtils.isNotBlank(line)) {
          currentResult.description += "<p>" + line;
        }
      } else if (inRule && !inExample && inDescription && !inParameters && !currentResult.description.endsWith("</pre>\n") && isValidDescriptionLine(line)) {
        if (isEndOfParagraph(currentResult, line)) {
          currentResult.description += "</p>\n";
        } else {
          currentResult.description += getParagraphLine(currentResult, line);
        }
      } else if (inRule && inExample && isExampleSeparator(line)) {
        inExample = false;
        inDescription = true;
        currentResult.description += "</pre>\n";
      } else if (inRule && inExample) {
        currentResult.description += fullLine + "\n";
      } else if (inRule && !inParameters && line.matches(PARAMETER_START_SEPARATOR)) {
        inDescription = false;
        inParameters = true;
        currentParameter = new RuleParameter();
        splits[0] = line.indexOf('*') + 1;
        splits[1] = line.indexOf('+', splits[0]) + 1;
        splits[2] = line.indexOf('+', splits[1]) + 1;
      } else if (inRule && inParameters && (line.matches(PARAMETER_CONTENT))) {
        String[] blocks = new String[3];
        blocks[0] = line.substring(splits[0], splits[1] - 1);
        blocks[1] = line.substring(splits[1], splits[2] - 1);
        blocks[2] = line.substring(splits[2], line.length() - 1);
        if (blocks.length == 3 && !isHeaderLine(blocks)) {
          String key = blocks[0].trim();
          String description = blocks[1].trim();
          String defaultValue = blocks[2].trim();
          if (StringUtils.isNotBlank(key)) {
            currentParameter.setKey(currentParameter.getKey() + key.replaceAll("(-)+", ""));
          }
          if (StringUtils.isNotBlank(defaultValue) && !currentParameter.hasDefaultValue()) {
            currentParameter.setDefaultValue(cleanDefaultValue(defaultValue));
          }
          if (StringUtils.isNotBlank(description)) {
            currentParameter.setDescription(currentParameter.getDescription() + cleanDescription(description, true) + " ");
          }
        }
      } else if (inRule && inParameters && isParameterSeparator(line)) {
        if (!currentParameter.isEmpty()) {
          currentResult.parameters.add(currentParameter);
        }
        currentParameter = new RuleParameter();
      } else if (inRule && inParameters) {
        if (!currentParameter.isEmpty()) {
          currentResult.parameters.add(currentParameter);
        }
        currentParameter = new RuleParameter();
        inParameters = false;
        inDescription = true;
      }
    }

    // last item
    if (inRule) {
      results.put(currentRule, currentResult);
    }

    return results;
  }

  private boolean isExampleSeparator(String line) {
    return line.matches(EXAMPLE_SEPARATOR_1) || line.matches(EXAMPLE_SEPARATOR_2);
  }

  private String getParagraphLine(AptResult currentResult, String line) {
    return (StringUtils.isNotBlank(line) && currentResult.description.endsWith("\n") || StringUtils.isBlank(currentResult.description) ? "<p>" : "") + cleanDescription(line, true)
      + " ";
  }

  private boolean isEndOfParagraph(AptResult currentResult, String line) {
    return StringUtils.isBlank(line) && StringUtils.isNotBlank(currentResult.description) && !currentResult.description.endsWith("</p>\n");
  }

  private boolean isValidDescriptionLine(String line) {
    return !line.startsWith("<Since")
      && !line.startsWith("~~~")
      && !line.startsWith("<New")
      && !line.startsWith("** ")
      && !line.startsWith("[]")
      && !line.startsWith("*----")
      && !line.startsWith("+----")
      && !line.startsWith("|")
      && !isParameterSeparator(line);
  }

  private boolean isParameterSeparator(String line) {
    return line.matches(PARAMETER_SEPARATOR) || line.matches(PARAMETER_START_SEPARATOR);
  }

  private String cleanDescription(String description, boolean isForParameter) {
    String result = description;
    if (!isForParameter) {
      result = result.replaceAll("<<<", "<code>");
      result = result.replaceAll(">>>", "</code>");
    } else {
      result = result.replaceAll("<<<", "");
      result = result.replaceAll(">>>", "");
    }
    return result;
  }

  private String cleanDefaultValue(String defaultValue) {
    String result = defaultValue.replaceAll("<<<", "");
    result = result.replaceAll(">>>", "");
    if ((result.startsWith("'") && result.endsWith("'")) || (result.startsWith("<") && result.endsWith(">"))) {
      result = result.substring(1, result.length() - 1);
    }
    return result;
  }

  private boolean isHeaderLine(String[] blocks) {
    return "<<Property>>".equalsIgnoreCase(blocks[0].trim());
  }

  private String getRuleName(String line) {
    if (StringUtils.isBlank(line)) {
      return null;
    }
    String result = null;
    if (line.startsWith("* {")) {
      result = StringUtils.substringBetween(line, "* {", "} Rule").trim();
    } else {
      result = line.substring(2).trim();
    }
    if (result.endsWith("Rule")) {
      result = result.substring(0, result.length() - 4);
    }
    if (StringUtils.isAllLowerCase(result) || !StringUtils.isAlphanumeric(result) || "References".equals(result)) {
      // false positive
      return null;
    }
    return result;
  }

  private void mergeParameters(Map<String, AptResult> results, Map<String, AptResult> parametersByFile) {
    for (String rule : parametersByFile.keySet()) {
      AptResult currentRuleResult = results.get(rule);
      if (currentRuleResult == null) {
        currentRuleResult = new AptResult(rule);
      }
      AptResult resultForRuleInFile = parametersByFile.get(rule);
      if (resultForRuleInFile.parameters != null) {
        for (RuleParameter parameter : resultForRuleInFile.getParameters()) {
          if (!currentRuleResult.parameters.contains(parameter)) {
            currentRuleResult.parameters.add(parameter);
          }
        }
      }
      boolean alreadyHasExample = StringUtils.isNotBlank(currentRuleResult.description);
      boolean provideNewExample = StringUtils.isNotBlank(resultForRuleInFile.description);
      if (!alreadyHasExample && provideNewExample) {
        currentRuleResult.description = resultForRuleInFile.description;
      } else if (alreadyHasExample && provideNewExample) {
        System.out.println("CONFLICT RULE " + rule);
        System.out.println(currentRuleResult.description);
        System.out.println("WITH");
        System.out.println(resultForRuleInFile.description);
      }
      results.put(rule, currentRuleResult);
    }

  }
}
