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
import org.apache.commons.io.IOUtils;
import org.codenarc.rule.AbstractRule;
import org.sonar.plugins.groovy.codenarc.apt.AptParser;
import org.sonar.plugins.groovy.codenarc.apt.AptResult;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class Converter {

  private Set<Class> duplications = new HashSet<Class>();

  private Properties props = new Properties();
  private PrintStream out;
  private static int count;
  private static Map<String, Integer> rulesByVersion;
  private static Map<String, Integer> rulesByCategory;
  private static Map<String, AptResult> parametersByRule;

  private static final String RESULTS_FOLDER = "target/results";
  /**
   * location of the apt files in the codenarc project
   */
  private static final String RULES_APT_FILES_LOCATION = "../../CodeNarc/src/site/apt";

  public Converter() throws Exception {
    rulesByVersion = Maps.newHashMap();
    rulesByCategory = Maps.newHashMap();
    parametersByRule = retrieveRulesParameters();

    File rules = setUpRulesFile();

    out = new PrintStream(rules);
    String version = IOUtils.toString(Converter.class.getResourceAsStream("/codenarc-version.txt"));
    out.println("<!-- Generated using CodeNarc " + version + " -->");

    props.load(Converter.class.getResourceAsStream("/codenarc-base-messages.properties"));
  }

  private Map<String, AptResult> retrieveRulesParameters() throws Exception {
    return new AptParser().parse(getRulesAptFile());
  }

  private List<File> getRulesAptFile() {
    File aptDir = new File(RULES_APT_FILES_LOCATION);
    List<File> rulesAptFiles = Lists.newArrayList();
    if (aptDir.exists() && aptDir.isDirectory()) {
      File[] files = aptDir.listFiles();
      for (File file : files) {
        if (file.getName().startsWith("codenarc-rules-")) {
          rulesAptFiles.add(file);
        }
      }
    }
    return rulesAptFiles;
  }

  private File setUpRulesFile() throws IOException {
    File resultDir = new File(RESULTS_FOLDER);
    resultDir.mkdirs();

    File rules = new File(resultDir, "rules.xml");
    if (rules.exists()) {
      rules.delete();
    }
    rules.createNewFile();
    return rules;
  }

  /**
   * Rule format based on {@link org.sonar.api.server.rule.RulesDefinitionXmlLoader}
   */
  private void rule(Class<? extends AbstractRule> ruleClass, String since) throws Exception {
    if (duplications.contains(ruleClass)) {
      System.out.println("Duplicated rule " + ruleClass.getName());
    } else {
      duplications.add(ruleClass);
    }

    Rule rule = new Rule(ruleClass, since, props, parametersByRule);
    rule.printAsXml(out);

    updateCounters(rule);
  }

  private void updateCounters(Rule rule) {
    count++;
    Integer nbByCategory = rulesByCategory.get(rule.getTag());
    if (nbByCategory == null) {
      nbByCategory = 0;
    }
    rulesByCategory.put(rule.getTag(), nbByCategory + 1);

    String version = rule.getVersion() == null ? "legacy" : rule.getVersion();
    Integer nbByVersion = rulesByVersion.get(version);
    if (nbByVersion == null) {
      nbByVersion = 0;
    }
    rulesByVersion.put(version, nbByVersion + 1);
  }

  private void startSet(String name) {
    out.println("  <!-- " + name + " rules -->");
    out.println();
  }

  private void start() {
    out.println("<rules>");
    out.println();
  }

  private void end() {
    out.println("</rules>");
    out.flush();
    out.close();
  }

  private static final String VERSION_0 = null;
  private static final String VERSION_0_11 = "0.11";
  private static final String VERSION_0_12 = "0.12";
  private static final String VERSION_0_13 = "0.13";
  private static final String VERSION_0_14 = "0.14";
  private static final String VERSION_0_15 = "0.15";
  private static final String VERSION_0_16 = "0.16";
  private static final String VERSION_0_17 = "0.17";
  private static final String VERSION_0_18 = "0.18";
  private static final String VERSION_0_19 = "0.19";
  private static final String VERSION_0_20 = "0.20";
  private static final String VERSION_0_21 = "0.21";
  private static final String VERSION_0_22 = "0.22";
  private static final String VERSION_0_23 = "0.23";

  public static void main(String[] args) throws Exception {
    Converter converter = new Converter();
    converter.start();

    basicRuleSet(converter);
    serializationRuleSet(converter);
    bracesRuleSet(converter);
    concurrencyRuleSet(converter);
    designRuleSet(converter);
    dryRuleSet(converter);
    exceptionsRuleSet(converter);
    genericRuleSet(converter);
    grailsRuleSet(converter);
    importsRuleSet(converter);
    junitRuleSet(converter);
    loggingRuleSet(converter);
    namingRuleSet(converter);
    sizeRuleSet(converter);
    unnecessaryRuleSet(converter);
    unusedRuleSet(converter);
    jdbcRuleSet(converter);
    securityRuleSet(converter);
    formattingRuleSet(converter);
    conventionRuleSet(converter);
    groovyismRuleSet(converter);

    converter.end();

    resultsByCategory();
    resultsByVersion();
    System.out.println("\n" + count + " rules processed");
  }

  private static void resultsByVersion() {
    System.out.println("Rules by Version:");
    List<String> versions = Lists.newArrayList(rulesByVersion.keySet());
    Collections.sort(versions);
    for (String version : versions) {
      System.out.println("  - " + version + " : " + rulesByVersion.get(version));
    }
  }

  private static void resultsByCategory() {
    System.out.println("Rules by category:");
    List<String> categories = Lists.newArrayList(rulesByCategory.keySet());
    Collections.sort(categories);
    for (String category : categories) {
      System.out.println("  - " + category + " : " + rulesByCategory.get(category));
    }
  }

  private static void securityRuleSet(Converter converter) throws Exception {
    // new ruleset in 0.14 - security
    converter.startSet("security");
    converter.rule(org.codenarc.rule.security.NonFinalSubclassOfSensitiveInterfaceRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.security.InsecureRandomRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.security.FileCreateTempFileRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.security.SystemExitRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.security.ObjectFinalizeRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.security.JavaIoPackageAccessRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.security.UnsafeArrayDeclarationRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.security.PublicFinalizeMethodRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.security.NonFinalPublicFieldRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.security.UnsafeImplementationAsMapRule.class, VERSION_0_19);
  }

  private static void jdbcRuleSet(Converter converter) throws Exception {
    // new ruleset in 0.14 - jdbc
    converter.startSet("jdbc");
    converter.rule(org.codenarc.rule.jdbc.DirectConnectionManagementRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.jdbc.JdbcConnectionReferenceRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.jdbc.JdbcResultSetReferenceRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.jdbc.JdbcStatementReferenceRule.class, VERSION_0_15);
  }

  private static void unusedRuleSet(Converter converter) throws Exception {
    converter.startSet("unused");
    converter.rule(org.codenarc.rule.unused.UnusedArrayRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.unused.UnusedObjectRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.unused.UnusedPrivateFieldRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.unused.UnusedPrivateMethodParameterRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unused.UnusedPrivateMethodRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.unused.UnusedVariableRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.unused.UnusedMethodParameterRule.class, VERSION_0_16);
  }

  private static void unnecessaryRuleSet(Converter converter) throws Exception {
    converter.startSet("unnecessary");
    // moved from basic in 0.16
    converter.rule(org.codenarc.rule.unnecessary.AddEmptyStringRule.class, VERSION_0_13);
    // moved from basic in 0.16
    converter.rule(org.codenarc.rule.unnecessary.ConsecutiveLiteralAppendsRule.class, VERSION_0_13);
    // moved from basic in 0.16
    converter.rule(org.codenarc.rule.unnecessary.ConsecutiveStringConcatenationRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryBigDecimalInstantiationRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryBigIntegerInstantiationRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryBooleanExpressionRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryBooleanInstantiationRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryCallForLastElementRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryCallToSubstringRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryCatchBlockRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryCollectCallRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryCollectionCallRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryConstructorRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryDefInMethodDeclarationRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryDoubleInstantiationRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryFloatInstantiationRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryGetterRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryGStringRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryIfStatementRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryInstantiationToGetClassRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryIntegerInstantiationRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryLongInstantiationRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryModOneRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryObjectReferencesRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryNullCheckRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryNullCheckBeforeInstanceOfRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryOverridingMethodRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryPublicModifierRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryReturnKeywordRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessarySelfAssignmentRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessarySemicolonRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryStringInstantiationRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryTernaryExpressionRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryTransientModifierRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryFinalOnPrivateMethodRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryElseStatementRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryParenthesesForMethodCallWithClosureRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryPackageReferenceRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryDefInVariableDeclarationRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryDotClassRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryInstanceOfCheckRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessarySubstringRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryDefInFieldDeclarationRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryCastRule.class, VERSION_0_21);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessaryToStringRule.class, VERSION_0_21);
    converter.rule(org.codenarc.rule.unnecessary.UnnecessarySafeNavigationOperatorRule.class, VERSION_0_22);
  }

  private static void sizeRuleSet(Converter converter) throws Exception {
    converter.startSet("size");
    // deprecated in 0.18
    // converter.rule(org.codenarc.rule.size.AbcComplexityRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.size.AbcMetricRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.size.ClassSizeRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.size.CyclomaticComplexityRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.size.MethodCountRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.size.MethodSizeRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.size.NestedBlockDepthRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.size.CrapMetricRule.class, VERSION_0_17);
    converter.rule(org.codenarc.rule.size.ParameterCountRule.class, VERSION_0_23);
  }

  private static void namingRuleSet(Converter converter) throws Exception {
    converter.startSet("naming");
    converter.rule(org.codenarc.rule.naming.AbstractClassNameRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.naming.ClassNameRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.naming.ConfusingMethodNameRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.naming.FieldNameRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.naming.InterfaceNameRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.naming.MethodNameRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.naming.ObjectOverrideMisspelledMethodNameRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.naming.PackageNameRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.naming.ParameterNameRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.naming.PropertyNameRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.naming.VariableNameRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.naming.FactoryMethodNameRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.naming.ClassNameSameAsFilenameRule.class, VERSION_0_19);
    converter.rule(org.codenarc.rule.naming.PackageNameMatchesFilePathRule.class, VERSION_0_22);
  }

  private static void loggingRuleSet(Converter converter) throws Exception {
    converter.startSet("logging");
    converter.rule(org.codenarc.rule.logging.LoggerForDifferentClassRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.logging.LoggingSwallowsStacktraceRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.logging.LoggerWithWrongModifiersRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.logging.MultipleLoggersRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.logging.PrintlnRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.logging.PrintStackTraceRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.logging.SystemErrPrintRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.logging.SystemOutPrintRule.class, VERSION_0);
  }

  private static void junitRuleSet(Converter converter) throws Exception {
    converter.startSet("junit");
    converter.rule(org.codenarc.rule.junit.ChainedTestRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.junit.CoupledTestCaseRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.junit.JUnitAssertAlwaysFailsRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.junit.JUnitAssertAlwaysSucceedsRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.junit.JUnitPublicNonTestMethodRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.junit.JUnitSetUpCallsSuperRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.junit.JUnitStyleAssertionsRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.junit.JUnitTearDownCallsSuperRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.junit.JUnitUnnecessarySetUpRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.junit.JUnitUnnecessaryTearDownRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.junit.UseAssertEqualsInsteadOfAssertTrueRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.junit.UseAssertFalseInsteadOfNegationRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.junit.UseAssertTrueInsteadOfAssertEqualsRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.junit.UseAssertTrueInsteadOfNegationRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.junit.UseAssertNullInsteadOfAssertEqualsRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.junit.UseAssertSameInsteadOfAssertTrueRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.junit.JUnitFailWithoutMessageRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.junit.JUnitTestMethodWithoutAssertRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.junit.UnnecessaryFailRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.junit.SpockIgnoreRestUsedRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.junit.JUnitLostTestRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.junit.JUnitUnnecessaryThrowsExceptionRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.junit.JUnitPublicFieldRule.class, VERSION_0_19);
    converter.rule(org.codenarc.rule.junit.JUnitAssertEqualsConstantActualValueRule.class, VERSION_0_19);
    converter.rule(org.codenarc.rule.junit.JUnitPublicPropertyRule.class, VERSION_0_21);
  }

  private static void importsRuleSet(Converter converter) throws Exception {
    converter.startSet("imports");
    converter.rule(org.codenarc.rule.imports.DuplicateImportRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.imports.ImportFromSamePackageRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.imports.UnnecessaryGroovyImportRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.imports.UnusedImportRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.imports.ImportFromSunPackagesRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.imports.MisorderedStaticImportsRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.imports.NoWildcardImportsRule.class, VERSION_0_21);
  }

  private static void grailsRuleSet(Converter converter) throws Exception {
    converter.startSet("grails");
    converter.rule(org.codenarc.rule.grails.GrailsPublicControllerMethodRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.grails.GrailsSessionReferenceRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.grails.GrailsServletContextReferenceRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.grails.GrailsStatelessServiceRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.grails.GrailsDomainHasToStringRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.grails.GrailsDomainHasEqualsRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.grails.GrailsDuplicateMappingRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.grails.GrailsDuplicateConstraintRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.grails.GrailsDomainReservedSqlKeywordNameRule.class, VERSION_0_19);
    converter.rule(org.codenarc.rule.grails.GrailsDomainWithServiceReferenceRule.class, VERSION_0_19);
    converter.rule(org.codenarc.rule.grails.GrailsMassAssignmentRule.class, VERSION_0_21);
  }

  private static void genericRuleSet(Converter converter) throws Exception {
    converter.startSet("generic");
    converter.rule(org.codenarc.rule.generic.IllegalRegexRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.generic.RequiredRegexRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.generic.RequiredStringRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.generic.StatelessClassRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.generic.IllegalPackageReferenceRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.generic.IllegalClassReferenceRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.generic.IllegalClassMemberRule.class, VERSION_0_19);
    converter.rule(org.codenarc.rule.generic.IllegalStringRule.class, VERSION_0_20);
    converter.rule(org.codenarc.rule.generic.IllegalSubclassRule.class, VERSION_0_21);
  }

  private static void exceptionsRuleSet(Converter converter) throws Exception {
    converter.startSet("exceptions");
    converter.rule(org.codenarc.rule.exceptions.CatchArrayIndexOutOfBoundsExceptionRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.exceptions.CatchErrorRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.exceptions.CatchExceptionRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.exceptions.CatchIllegalMonitorStateExceptionRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.exceptions.CatchIndexOutOfBoundsExceptionRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.exceptions.CatchNullPointerExceptionRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.exceptions.CatchRuntimeExceptionRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.exceptions.CatchThrowableRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.exceptions.ConfusingClassNamedExceptionRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.exceptions.ExceptionExtendsErrorRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.exceptions.MissingNewInThrowStatementRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.exceptions.ReturnNullFromCatchBlockRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.exceptions.ThrowErrorRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.exceptions.ThrowExceptionRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.exceptions.ThrowNullPointerExceptionRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.exceptions.ThrowRuntimeExceptionRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.exceptions.ThrowThrowableRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.exceptions.SwallowThreadDeathRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.exceptions.ExceptionNotThrownRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.exceptions.ExceptionExtendsThrowableRule.class, VERSION_0_21);
  }

  private static void dryRuleSet(Converter converter) throws Exception {
    converter.startSet("dry");
    converter.rule(org.codenarc.rule.dry.DuplicateNumberLiteralRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.dry.DuplicateStringLiteralRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.dry.DuplicateMapLiteralRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.dry.DuplicateListLiteralRule.class, VERSION_0_16);
  }

  private static void basicRuleSet(Converter converter) throws Exception {
    converter.startSet("basic");
    converter.rule(org.codenarc.rule.basic.AssignmentInConditionalRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.basic.BigDecimalInstantiationRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.BooleanGetBooleanRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.basic.BrokenOddnessCheckRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.basic.ConstantIfExpressionRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.ConstantTernaryExpressionRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.DeadCodeRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.basic.DoubleNegativeRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.basic.DuplicateCaseStatementRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.basic.EmptyCatchBlockRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.EmptyElseBlockRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.EmptyFinallyBlockRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.EmptyForStatementRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.EmptyIfStatementRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.EmptyInstanceInitializerRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.basic.EmptyMethodRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.basic.EmptyStaticInitializerRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.basic.EmptySwitchStatementRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.EmptySynchronizedStatementRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.EmptyTryBlockRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.EmptyWhileStatementRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.EqualsAndHashCodeRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.ExplicitGarbageCollectionRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.basic.IntegerGetIntegerRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.basic.RemoveAllOnSelfRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.basic.ReturnFromFinallyBlockRule.class, VERSION_0);
    // removed in 0.14
    // converter.rule(org.codenarc.rule.basic.SerialVersionUIDRule.class, VERSION_0_11);
    // converter.rule(org.codenarc.rule.basic.SerializableClassMustDefineSerialVersionUIDRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.basic.ThrowExceptionFromFinallyBlockRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.basic.DuplicateMapKeyRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.basic.DuplicateSetValueRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.basic.EqualsOverloadedRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.basic.ForLoopShouldBeWhileLoopRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.basic.ClassForNameRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.basic.ComparisonOfTwoConstantsRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.basic.ComparisonWithSelfRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.basic.BitwiseOperatorInConditionalRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.basic.HardCodedWindowsFileSeparatorRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.basic.RandomDoubleCoercedToZeroRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.basic.HardCodedWindowsRootDirectoryRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.basic.AssertWithinFinallyBlockRule.class, VERSION_0_17);
    converter.rule(org.codenarc.rule.basic.ConstantAssertExpressionRule.class, VERSION_0_17);
    converter.rule(org.codenarc.rule.basic.BrokenNullCheckRule.class, VERSION_0_17);
    converter.rule(org.codenarc.rule.basic.EmptyClassRule.class, VERSION_0_19);
    converter.rule(org.codenarc.rule.basic.MultipleUnaryOperatorsRule.class, VERSION_0_21);
  }

  private static void serializationRuleSet(Converter converter) throws Exception {
    // new ruleset in 0.14 - serialization
    converter.startSet("serialization");
    converter.rule(org.codenarc.rule.serialization.SerialVersionUIDRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.serialization.SerializableClassMustDefineSerialVersionUIDRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.serialization.SerialPersistentFieldsRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.serialization.EnumCustomSerializationIgnoredRule.class, VERSION_0_19);
  }

  private static void bracesRuleSet(Converter converter) throws Exception {
    converter.startSet("braces");
    converter.rule(org.codenarc.rule.braces.IfStatementBracesRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.braces.ElseBlockBracesRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.braces.ForStatementBracesRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.braces.WhileStatementBracesRule.class, VERSION_0);
  }

  private static void concurrencyRuleSet(Converter converter) throws Exception {
    converter.startSet("concurrency");
    converter.rule(org.codenarc.rule.concurrency.BusyWaitRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.DoubleCheckedLockingRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.InconsistentPropertyLockingRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.InconsistentPropertySynchronizationRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.NestedSynchronizationRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.concurrency.StaticCalendarFieldRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.StaticDateFormatFieldRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.StaticMatcherFieldRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.SynchronizedMethodRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.concurrency.SynchronizedOnGetClassRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.concurrency.SynchronizedOnBoxedPrimitiveRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.SynchronizedOnStringRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.SynchronizedOnThisRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.concurrency.SynchronizedReadObjectMethodRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.SynchronizedOnReentrantLockRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.SystemRunFinalizersOnExitRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.concurrency.ThreadGroupRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.concurrency.ThreadLocalNotStaticFinalRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.concurrency.ThreadYieldRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.concurrency.UseOfNotifyMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.concurrency.VolatileArrayFieldRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.VolatileLongOrDoubleFieldRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.concurrency.WaitOutsideOfWhileLoopRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.concurrency.StaticConnectionRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.concurrency.StaticSimpleDateFormatFieldRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.concurrency.ThisReferenceEscapesConstructorRule.class, VERSION_0_19);
  }

  private static void designRuleSet(Converter converter) throws Exception {
    converter.startSet("design");
    // moved from basic in 0.16
    converter.rule(org.codenarc.rule.design.BooleanMethodReturnsNullRule.class, VERSION_0_11);
    // moved from basic in 0.16
    converter.rule(org.codenarc.rule.design.CloneableWithoutCloneRule.class, VERSION_0);
    // moved from basic in 0.16
    converter.rule(org.codenarc.rule.design.CompareToWithoutComparableRule.class, VERSION_0_12);
    // moved from basic in 0.16
    converter.rule(org.codenarc.rule.design.ReturnsNullInsteadOfEmptyArrayRule.class, VERSION_0_11);
    // moved from basic in 0.16
    converter.rule(org.codenarc.rule.design.ReturnsNullInsteadOfEmptyCollectionRule.class, VERSION_0_11);
    // moved from basic in 0.16
    converter.rule(org.codenarc.rule.design.SimpleDateFormatMissingLocaleRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.design.AbstractClassWithoutAbstractMethodRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.design.CloseWithoutCloseableRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.design.ConstantsOnlyInterfaceRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.design.EmptyMethodInAbstractClassRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.design.ImplementationAsTypeRule.class, VERSION_0);
    converter.rule(org.codenarc.rule.design.FinalClassWithProtectedMemberRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.design.PublicInstanceFieldRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.design.StatelessSingletonRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.design.AbstractClassWithPublicConstructorRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.design.BuilderMethodWithSideEffectsRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.design.PrivateFieldCouldBeFinalRule.class, VERSION_0_17);
    converter.rule(org.codenarc.rule.design.CloneWithoutCloneableRule.class, VERSION_0_19);
    converter.rule(org.codenarc.rule.design.LocaleSetDefaultRule.class, VERSION_0_20);
    converter.rule(org.codenarc.rule.design.ToStringReturnsNullRule.class, VERSION_0_21);
    converter.rule(org.codenarc.rule.design.InstanceofRule.class, VERSION_0_22);
    converter.rule(org.codenarc.rule.design.NestedForLoopRule.class, VERSION_0_23);
  }

  private static void formattingRuleSet(Converter converter) throws Exception {
    // new ruleset in 0.15 - formatting
    converter.startSet("formatting");
    converter.rule(org.codenarc.rule.formatting.BracesForClassRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.formatting.LineLengthRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.formatting.BracesForForLoopRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.formatting.BracesForIfElseRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.formatting.BracesForMethodRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.formatting.BracesForTryCatchFinallyRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.formatting.ClassJavadocRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.formatting.SpaceAfterCommaRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceAfterSemicolonRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceAroundOperatorRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceBeforeOpeningBraceRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceAfterOpeningBraceRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceAfterClosingBraceRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceBeforeClosingBraceRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceAfterIfRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceAfterWhileRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceAfterForRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceAfterSwitchRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceAfterCatchRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.formatting.SpaceAroundClosureArrowRule.class, VERSION_0_19);
    converter.rule(org.codenarc.rule.formatting.SpaceAroundMapEntryColonRule.class, VERSION_0_20);
    converter.rule(org.codenarc.rule.formatting.ClosureStatementOnOpeningLineOfMultipleLineClosureRule.class, VERSION_0_20);
    converter.rule(org.codenarc.rule.formatting.ConsecutiveBlankLinesRule.class, VERSION_0_21);
    converter.rule(org.codenarc.rule.formatting.BlankLineBeforePackageRule.class, VERSION_0_21);
    converter.rule(org.codenarc.rule.formatting.FileEndsWithoutNewlineRule.class, VERSION_0_21);
    converter.rule(org.codenarc.rule.formatting.MissingBlankLineAfterImportsRule.class, VERSION_0_21);
    converter.rule(org.codenarc.rule.formatting.MissingBlankLineAfterPackageRule.class, VERSION_0_21);
    converter.rule(org.codenarc.rule.formatting.TrailingWhitespaceRule.class, VERSION_0_21);
  }

  private static void conventionRuleSet(Converter converter) throws Exception {
    // new ruleset in 0.16 - convention, rules moved from basic
    converter.startSet("convention");
    converter.rule(org.codenarc.rule.convention.ConfusingTernaryRule.class, VERSION_0_12);
    converter.rule(org.codenarc.rule.convention.InvertedIfElseRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.convention.CouldBeElvisRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.convention.LongLiteralWithLowerCaseLRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.convention.ParameterReassignmentRule.class, VERSION_0_17);
    converter.rule(org.codenarc.rule.convention.TernaryCouldBeElvisRule.class, VERSION_0_17);
    converter.rule(org.codenarc.rule.convention.VectorIsObsoleteRule.class, VERSION_0_17);
    converter.rule(org.codenarc.rule.convention.HashtableIsObsoleteRule.class, VERSION_0_17);
    converter.rule(org.codenarc.rule.convention.IfStatementCouldBeTernaryRule.class, VERSION_0_18);
    converter.rule(org.codenarc.rule.convention.NoDefRule.class, VERSION_0_22);
  }

  private static void groovyismRuleSet(Converter converter) throws Exception {
    // new ruleset in 0.16 - groovyism, rules moved from basic
    converter.startSet("groovyism");
    converter.rule(org.codenarc.rule.groovyism.AssignCollectionSortRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.groovyism.ExplicitArrayListInstantiationRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToAndMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToCompareToMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToDivMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToEqualsMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToGetAtMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToLeftShiftMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToMinusMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToMultiplyMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToModMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToOrMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToPlusMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToPowerMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToRightShiftMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitCallToXorMethodRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitHashMapInstantiationRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitHashSetInstantiationRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitLinkedListInstantiationRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitStackInstantiationRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitTreeSetInstantiationRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.GroovyLangImmutableRule.class, VERSION_0_13);
    converter.rule(org.codenarc.rule.groovyism.GStringAsMapKeyRule.class, VERSION_0_11);
    converter.rule(org.codenarc.rule.groovyism.ExplicitLinkedHashMapInstantiationRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.groovyism.ClosureAsLastMethodParameterRule.class, VERSION_0_14);
    converter.rule(org.codenarc.rule.groovyism.AssignCollectionUniqueRule.class, VERSION_0_15);
    converter.rule(org.codenarc.rule.groovyism.ConfusingMultipleReturnsRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.groovyism.GetterMethodCouldBePropertyRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.groovyism.UseCollectManyRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.groovyism.CollectAllIsDeprecatedRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.groovyism.UseCollectNestedRule.class, VERSION_0_16);
    converter.rule(org.codenarc.rule.groovyism.GStringExpressionWithinStringRule.class, VERSION_0_19);
  }
}
