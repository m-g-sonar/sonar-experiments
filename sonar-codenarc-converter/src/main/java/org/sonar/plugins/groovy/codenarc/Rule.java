package org.sonar.plugins.groovy.codenarc;

import com.google.common.collect.Maps;
import org.apache.commons.lang.StringUtils;
import org.codenarc.rule.AbstractRule;

import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Properties;

public class Rule {

  AbstractRule rule;
  String key;
  String internalKey;
  String name;
  String description;
  String severity;
  String version;
  String tag;
  Map<String, Parameter> parameters;

  public class Parameter {
    String key;
    String description;
    String defaultValue;

    Parameter(String key, String defaultValue) {
      this.key = key;
      this.defaultValue = defaultValue;
    }

    Parameter(String key, String defaultValue, String description) {
      this.key = key;
      this.defaultValue = defaultValue;
      this.description = description;
    }

    boolean hasDifferentDefaultValue(String defaultValue) {
      return StringUtils.isNotBlank(this.defaultValue) && !this.defaultValue.equals(defaultValue);
    }
  }

  public Rule(Class<? extends AbstractRule> ruleClass, String since, Properties props) throws Exception {
    rule = ruleClass.newInstance();
    key = ruleClass.getCanonicalName();
    internalKey = StringUtils.removeEnd(ruleClass.getSimpleName(), "Rule");
    name = StringUtils.join(StringUtils.splitByCharacterTypeCamelCase(internalKey), ' ');
    severity = severity(rule.getPriority());
    description = props.getProperty(internalKey + ".description.html");
    tag = getTag(ruleClass.getCanonicalName());
    parameters = extractParameters(description, rule);
    description = cleanDescription(description);
    version = since;
  }

  private Map<String, Parameter> extractParameters(String description, AbstractRule rule) {
    Map<String, Parameter> parameters = Maps.newHashMap();

    String[] params1 = StringUtils.substringsBetween(description, "<em>", "</em> property");
    if (params1 != null) {
      for (int i = 0; i < params1.length; i++) {
        String paramName = params1[i];
        if (paramName.contains("<em>")) {
          params1[i] = paramName.substring(paramName.lastIndexOf("<em>") + 4);
        }
      }
      addParameters(parameters, params1, rule);
    }
    String[] params2 = StringUtils.substringsBetween(description, "configured in <em>", "</em>");
    if (params2 != null) {
      addParameters(parameters, params2, rule);
    }
    if (StringUtils.contains(description, "length property")) {
      addParameter(parameters, "length", rule);
    }
    if (StringUtils.contains(description, "sameLine property")) {
      addParameter(parameters, "sameLine", rule);
    }
    return parameters;
  }

  private void addParameters(Map<String, Parameter> parameters, String[] parameterNames, AbstractRule rule) {
    for (String parameterName : parameterNames) {
      addParameter(parameters, parameterName, rule);
    }
  }

  private void addParameter(Map<String, Parameter> parameters, String parameterName, AbstractRule rule) {
    String defaultValue = getParamDefaultValue(rule, parameterName);
    Parameter parameter = parameters.get(parameterName);
    if (parameter != null && parameter.hasDifferentDefaultValue(defaultValue)) {
      parameter.defaultValue = defaultValue;
    } else {
      parameter = new Parameter(parameterName, defaultValue);
    }
    parameters.put(parameterName, parameter);
  }

  private String getParamDefaultValue(AbstractRule rule, String param) {
    String result = null;
    try {
      // Hack to get the default value
      Field f = rule.getClass().getDeclaredField(param);
      f.setAccessible(true);
      Object value = f.get(rule);
      return value.toString();
    } catch (Exception e) {
      // do nothing, there is probably no default value
    }
    return result;
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

  private String getTag(String canonicalName) {
    String[] split = canonicalName.split("\\.");
    return split[split.length - 2];
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
    return copy;
  }

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
    out.println("    <tag>" + tag + "</tag>");

    if (!parameters.isEmpty()) {
      for (String paramName : parameters.keySet()) {
        Parameter parameter = parameters.get(paramName);
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
}
