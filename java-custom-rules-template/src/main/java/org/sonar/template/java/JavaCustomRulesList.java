package org.sonar.template.java;

import com.google.common.collect.ImmutableList;

import org.sonar.plugins.java.api.JavaCheck;
import org.sonar.template.java.checks.MyFirstCustomCheck;

import java.util.List;

public final class JavaCustomRulesList {

  private JavaCustomRulesList() {
  }

  public static List<Class> getChecks() {
    return ImmutableList.<Class>builder().addAll(getJavaChecks()).addAll(getJavaTestChecks()).build();
  }

  public static List<Class<? extends JavaCheck>> getJavaChecks() {
    return ImmutableList.<Class<? extends JavaCheck>>builder()
      .add(MyFirstCustomCheck.class)
      .build();
  }

  public static List<Class<? extends JavaCheck>> getJavaTestChecks() {
    return ImmutableList.<Class<? extends JavaCheck>>builder()
      // add HERE rules targeting test files
      .build();
  }
}
