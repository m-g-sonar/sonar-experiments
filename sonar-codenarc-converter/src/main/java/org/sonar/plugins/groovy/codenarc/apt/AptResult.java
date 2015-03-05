package org.sonar.plugins.groovy.codenarc.apt;

import com.google.common.collect.Sets;
import org.apache.commons.lang.StringUtils;
import org.sonar.plugins.groovy.codenarc.RuleParameter;

import java.util.Set;

public class AptResult {
  String rule;
  Set<RuleParameter> parameters = Sets.newHashSet();
  String description = "";

  public AptResult(String rule) {
    this.rule = rule;
  }

  void display(int ruleFileCounter, int ruleTotalCounter, String filename) {
    System.out.println("==========================================");
    System.out.println("Rule #" + ruleTotalCounter + " : " + rule + " (" + filename + " #" + ruleFileCounter
      + ")");
    if (StringUtils.isNotBlank(description)) {
      System.out.println("------------------------------------------");
      System.out.println(description);
    }
    if (!parameters.isEmpty()) {
      System.out.println("------------------------------------------");
      System.out.println("Parameters: ");
      for (RuleParameter parameter : parameters) {
        System.out.println("  * \"" + parameter.getKey() + "\"");
        System.out.println("    - defaultValue: "
          + (parameter.getDefaultValue() == null ? "" : parameter.getDefaultValue()));
        System.out.println("    - description: " + parameter.getDescription());
      }
    }
  }

  public String getRule() {
    return rule;
  }

  public Set<RuleParameter> getParameters() {
    return parameters;
  }

  public String getDescription() {
    return description;
  }

  public boolean hasParameters() {
    return parameters != null && !parameters.isEmpty();
  }
}
