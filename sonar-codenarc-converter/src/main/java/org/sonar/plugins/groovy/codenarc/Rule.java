/*
 * Sonar CodeNarc Converter
 * Copyright (C) 2011 SonarSource
 * dev@sonar.codehaus.org
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
package org.sonar.plugins.groovy.codenarc;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.codenarc.rule.AbstractRule;
import org.sonar.plugins.groovy.codenarc.apt.AptResult;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Rule {

  private AbstractRule rule;
  private String key;
  private String internalKey;
  private String name;
  private String description;
  private String severity;
  private String version;
  private Set<String> tags;
  private Set<RuleParameter> parameters;

  public Rule(Class<? extends AbstractRule> ruleClass, String since, Properties props, Map<String, AptResult> parametersByRule) throws Exception {
    rule = ruleClass.newInstance();
    key = ruleClass.getCanonicalName();
    internalKey = StringUtils.removeEnd(ruleClass.getSimpleName(), "Rule");
    name = cleanName(internalKey);
    severity = severity(rule.getPriority());
    tags = getTags(key, internalKey);
    version = since;

    AptResult dataFromAptFile = parametersByRule.get(internalKey);
    String descriptionFromProperty = props.getProperty(internalKey + ".description.html");
    description = extractDescription(dataFromAptFile, descriptionFromProperty);
    parameters = extractParameters(dataFromAptFile, descriptionFromProperty);
  }

  private String cleanName(String internalKey) {
    String result = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(internalKey), ' ');
    return result.replace("J Unit", "JUnit");
  }

  private Set<RuleParameter> extractParameters(AptResult results, String description) {
    Map<String, RuleParameter> parameters = Maps.newHashMap();
    addParameters(results, parameters);

    String[] params1 = StringUtils.substringsBetween(description, "<em>", "</em> property");
    if (params1 != null) {
      for (int i = 0; i < params1.length; i++) {
        String paramName = params1[i];
        if (paramName.contains("<em>")) {
          params1[i] = paramName.substring(paramName.lastIndexOf("<em>") + 4);
        }
      }
      addParameters(params1, rule, parameters);
    }
    String[] params2 = StringUtils.substringsBetween(description, "configured in <em>", "</em>");
    if (params2 != null) {
      addParameters(params2, rule, parameters);
    }
    if (StringUtils.contains(description, "length property")) {
      addParameter("length", rule, parameters);
    }
    if (StringUtils.contains(description, "sameLine property")) {
      addParameter("sameLine", rule, parameters);
    }
    return Sets.newHashSet(parameters.values());
  }

  private void addParameters(AptResult results, Map<String, RuleParameter> parameters) {
    if (results.hasParameters()) {
      for (RuleParameter param : results.getParameters()) {
        parameters.put(param.key, param);
      }
    }
  }

  private void addParameters(String[] parameterNames, AbstractRule ruleInstance, Map<String, RuleParameter> parameters) {
    for (String parameterName : parameterNames) {
      addParameter(parameterName, ruleInstance, parameters);
    }
  }

  private void addParameter(String parameterName, AbstractRule ruleInstance, Map<String, RuleParameter> parameters) {
    RuleParameter current = parameters.get(parameterName);
    RuleParameter parameter = new RuleParameter(parameterName);
    parameter.defaultValue = extractDefaultValue(parameterName, ruleInstance);
    if (current == null) {
      current = parameter;
    } else {
      current.merge(parameter);
    }
    parameters.put(current.key, current);

  }

  private String extractDefaultValue(String parameterName, AbstractRule ruleInstance) {
    String result = "";
    try {
      // Hack to get the default value
      Field f = rule.getClass().getDeclaredField(parameterName);
      f.setAccessible(true);
      Object value = f.get(rule);
      return value.toString();
    } catch (Exception e) {
      // do nothing, there is probably no default value
    }
    return result;
  }

  private String extractDescription(AptResult dataFromAptFile, String property) {
    String result = property;
    if (dataFromAptFile != null && StringUtils.isNotBlank(dataFromAptFile.getDescription())) {
      result = dataFromAptFile.getDescription();
    }
    return cleanDescription(result);
  }

  private String severity(int priority) {
    switch (priority) {
      case 1:
        return "INFO";
      case 2:
        return "MINOR";
      case 3:
        return "MAJOR";
      default:
        throw new RuntimeException("Should never happen");
    }
  }

  private Set<String> getTags(String key, String internalKey) {
    String[] split = key.split("\\.");
    String codeNarcCategory = split[split.length - 2];
    Set<String> results = Sets.newHashSet();

    switch (codeNarcCategory) {
      case "unnecessary":
        results.add("clumsy");
        break;
      case "formatting":
      case "naming ":
        results.add("convention");
        break;
      case "concurrency":
        results.add("multi-threading");
        break;
      case "exceptions":
        results.add("error-handling");
        break;
      case "basic":
        results.addAll(handleBasicCategory(internalKey));
        break;
      case "grails":
      case "groovyism":
      case "junit":
      case "design":
        results.add(codeNarcCategory);
        break;
      default:
        results.add("bug");
        break;
    }
    return results;
  }

  private Set<String> handleBasicCategory(String internalKey) {
    Set<String> results = Sets.newHashSet();
    if (internalKey.startsWith("Empty")) {
      results.add("unused");
    } else if (internalKey.startsWith("Broken")) {
      results.add("bug");
    } else if (internalKey.startsWith("Equals")) {
      results.add("pitfall");
    } else if (internalKey.contains("Get") && !internalKey.startsWith("Get")) {
      results.add("bug");
    } else if (internalKey.endsWith("FinallyBlock")) {
      results.add("error-handling");
    } else {
      results.addAll(handleParticularCases(internalKey));
    }
    return results;
  }

  private Set<String> handleParticularCases(String internalKey) {
    Set<String> results = Sets.newHashSet();
    if (internalKey.equals("DeadCode")) {
      results.add("unused");
    } else if (internalKey.equals("ExplicitGarbageCollection")) {
      results.add("unpredictable");
    } else if (internalKey.equals("HardCodedWindowsFileSeparator") || internalKey.equals("HardCodedWindowsRootDirectory")) {
      results.add("pitfall");
    } else if (internalKey.equals("ForLoopShouldBeWhileLoop")) {
      results.add("clumsy");
    } else if (internalKey.equals("ClassForName")) {
      results.add("leak");
      results.add("owasp-a1");
    } else {
      results.add("bug");
    }
    return results;
  }

  private String cleanDescription(String description) {
    String copy = description;
    String[] refToParams = StringUtils.substringsBetween(description, " (${rule.", "})");
    if (refToParams != null) {
      for (String ref : refToParams) {
        String paramRef = " (${rule." + ref + "})";
        copy = copy.replace(paramRef, "");
      }
    }
    return handleUrls(copy);
  }

  /**
   * Covers URLs such as:
   * {{http://blog.bjhargrave.com/2007/09/classforname-caches-defined-class-in.html}} <--- direct link
   * {{{http://en.wikipedia.org/wiki/Double-checked_locking}}}                        <--- direct link
   * {{{http://jira.codehaus.org/browse/JETTY-352}JETTY-352}}                         <--- renamed link
   */
  private String handleUrls(String description) {
    String result = description;
    String[] urls = extractUrls(description);
    if (urls != null) {
      for (String url : urls) {
        String copy = url;
        boolean trailingAcc = false;
        if (!copy.startsWith("{")) {
          copy = "<a>" + copy + "</a>";
        } else if (copy.startsWith("{http")) {
          if ('}' == result.charAt(result.indexOf(copy) + copy.length() + 2)) {
            trailingAcc = true;
            copy = "<a>" + copy.substring(1) + "</a>";
          } else {
            copy = "<a href=\"" + copy.replace("{", "").replace("}", "\">") + "</a>";
          }
        } else if (copy.startsWith("{./")) {
          copy = "<a href=\"http://codenarc.sourceforge.net/" + copy.replace("{./", "").replace("}", "\">") + "</a>";
        }
        result = result.replace("{{" + url + "}}" + (trailingAcc ? "}" : ""), copy);
      }
    }
    return result;
  }

  private String[] extractUrls(String description) {
    List<String> urls = Lists.newArrayList();
    int index = 0;
    while (index < description.length()) {
      int start = description.indexOf("{{", index);
      if (start == -1) {
        break;
      }
      int end = description.indexOf("}}", start);
      if (end == -1) {
        break;
      }
      urls.add(description.substring(start + 2, end));
      index = end;
    }
    if (urls.isEmpty()) {
      return null;
    }
    return urls.toArray(new String[urls.size()]);
  }

  /**
   * Rule format based on {@link org.sonar.api.server.rule.RulesDefinitionXmlLoader}
   */
  public void printAsXml(PrintStream out) {
    if (version != null) {
      out.println("  <!-- since " + version + " -->");
    }
    out.println("  <rule>");
    out.println("    <key>" + key + "</key>");
    out.println("    <severity>" + severity + "</severity>");
    out.println("    <name><![CDATA[" + name + "]]></name>");
    out.println("    <internalKey><![CDATA[" + internalKey + "]]></internalKey>");
    out.println("    <description><![CDATA[" + description + "]]></description>");
    if (!tags.isEmpty()) {
      for (String tag : tags) {
        out.println("    <tag>" + tag + "</tag>");
      }
    }

    if (!parameters.isEmpty()) {
      List<RuleParameter> sortedParameters = Lists.newArrayList(parameters);
      Collections.sort(sortedParameters, new Comparator<RuleParameter>() {
        @Override
        public int compare(RuleParameter o1, RuleParameter o2) {
          return o1.key.compareTo(o2.key);
        }
      });
      for (RuleParameter parameter : sortedParameters) {
        out.println("    <param>");
        out.println("      <key>" + parameter.key + "</key>");
        if (StringUtils.isNotBlank(parameter.description)) {
          out.println("      <description><![CDATA[" + parameter.description + "]]></description>");
        }
        if (StringUtils.isNotBlank(parameter.defaultValue)) {
          out.println("      <defaultValue>" + parameter.defaultValue + "</defaultValue>");
        }
        out.println("    </param>");
      }
    }

    out.println("  </rule>");
    out.println();
  }

  public String getVersion() {
    return version;
  }

  public Set<String> getTags() {
    return tags;
  }

  public String getKey() {
    return key;
  }
}
