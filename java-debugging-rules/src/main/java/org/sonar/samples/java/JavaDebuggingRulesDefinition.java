/*
 * Copyright (C) 2009-2014 SonarSource SA
 * All rights reserved
 * mailto:contact AT sonarsource DOT com
 */
package org.sonar.samples.java;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.plugins.java.Java;
import org.sonar.squidbridge.annotations.AnnotationBasedRulesDefinition;

/**
 * Declare rule metadata in server repository of rules. 
 * That allows to list the rules in the page "Rules".
 */
public class JavaDebuggingRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "java-debugging-rules";

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, Java.KEY);
    repository.setName("Java Debugging Rules");

    AnnotationBasedRulesDefinition.load(repository, "java", RulesList.getChecks());
    repository.done();
  }
}
