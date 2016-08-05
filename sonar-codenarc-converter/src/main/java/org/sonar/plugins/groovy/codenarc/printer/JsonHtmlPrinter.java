/*
 * Sonar CodeNarc Converter
 * Copyright (C) 2011-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package org.sonar.plugins.groovy.codenarc.printer;

import com.google.common.collect.Multimap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.sonar.plugins.groovy.codenarc.Converter;
import org.sonar.plugins.groovy.codenarc.Rule;
import org.sonar.plugins.groovy.codenarc.RuleParameter;
import org.sonar.plugins.groovy.codenarc.RuleSet;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class JsonHtmlPrinter implements Printer {

  private Converter converter;

  private static final String RULES_FOLDER = "rules";

  private List<String> ruleKeys = new ArrayList<>();
  private Map<String, String> htmlDescriptionByRuleKey = new HashMap<>();
  private Map<String, JsonRule> jsonRuleByRuleKey = new HashMap<>();

  @Override
  public Printer init(Converter converter) {
    this.converter = converter;
    return this;
  }

  @Override
  public Printer process(Multimap<RuleSet, Rule> rulesBySet) throws Exception {
    for (RuleSet ruleSet : RuleSet.values()) {
      for (Rule rule : rulesBySet.get(ruleSet)) {
        converter.startPrintingRule(rule);

        String fixedRuleKey = rule.fixedRuleKey();
        ruleKeys.add(fixedRuleKey);

        JsonRule value = new JsonRule(rule);
        jsonRuleByRuleKey.put(fixedRuleKey, value);
        htmlDescriptionByRuleKey.put(fixedRuleKey, rule.description);
      }
    }
    return this;
  }

  @Override
  public File printAll(File resultDir) throws Exception {
    File ruleFolder = prepareRulesFolder(resultDir);

    for (String ruleKey : ruleKeys) {
      String fileName = ruleKey.replaceAll("\\.", "_");
      File jsonFile = new File(ruleFolder, fileName + ".json");
      if (jsonFile.createNewFile()) {
        print(jsonFile, jsonRuleByRuleKey.get(ruleKey).toJson());
      }
      File htmlFile = new File(ruleFolder, fileName + ".html");
      if (htmlFile.createNewFile()) {
        print(htmlFile, htmlDescriptionByRuleKey.get(ruleKey));
      }
    }

    return ruleFolder;
  }

  private static File prepareRulesFolder(File resultDir) {
    File ruleFolder = new File(resultDir, RULES_FOLDER);
    if (ruleFolder.exists()) {
      for (File existingFiles : ruleFolder.listFiles()) {
        existingFiles.delete();
      }
    }
    ruleFolder.mkdir();
    return ruleFolder;
  }

  private static void print(File file, String text) throws Exception {
    PrintStream out = new PrintStream(file, "UTF-8");
    out.print(text);
    out.flush();
    out.close();
  }

  private static class JsonRule {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    final String key;
    final String internalKey;
    final String name;
    final String severity;
    final String codeNarcVersion;
    final List<String> tags;
    final List<RuleParameter> ruleParameters;

    JsonRule(Rule rule) {
      this.key = rule.fixedRuleKey();
      this.internalKey = rule.internalKey;
      this.name = rule.name;
      this.tags = rule.tags.stream().sorted().collect(Collectors.toList());
      this.ruleParameters = rule.parameters.stream().sorted().collect(Collectors.toList());
      this.severity = rule.severity;
      this.codeNarcVersion = rule.version;
    }

    String toJson() {
      return GSON.toJson(this);
    }
  }

}
