package org.sonar.samples.java.checks;

import com.google.common.collect.ImmutableList;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.JavaFileScannerContext;
import org.sonar.plugins.java.api.JavaFileScannerContext.Location;
import org.sonar.plugins.java.api.tree.Arguments;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.ArrayList;
import java.util.List;

@Rule(key = "UnknownMethodInvocation",
  name = "Unknown Method invocation",
  description = "This rule detects unknown method invocation",
  tags = {"debugging"})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.RELIABILITY_COMPLIANCE)
@SqaleConstantRemediation("10min")
public class UnknownMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION, Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    if (tree.is(Tree.Kind.METHOD_INVOCATION)) {
      handleMethodInvocation((MethodInvocationTree) tree);
    } else {
      handleConstructor((NewClassTree) tree);
    }

  }

  private void handleConstructor(NewClassTree tree) {
    if (tree.constructorSymbol().isUnknown()) {
      reportUnknownMethod(tree.identifier(), "Unknown constructor", getMissingArguments(tree.arguments()));
    }
  }

  private void handleMethodInvocation(MethodInvocationTree tree) {
    if (tree.symbol().isUnknown()) {
      reportUnknownMethod(tree.methodSelect(), "Unknown method", getMissingArguments(tree.arguments()));
    }
  }

  private static List<Location> getMissingArguments(Arguments arguments) {
    List<JavaFileScannerContext.Location> secondaries = new ArrayList<>();
    for (ExpressionTree argument : arguments) {
      if (argument.symbolType().isUnknown()) {
        secondaries.add(new JavaFileScannerContext.Location("Unknown type", argument));
      }
    }
    return secondaries;
  }

  void reportUnknownMethod(Tree reportTree, String message, List<JavaFileScannerContext.Location> secondaries) {
    Tree target = reportTree;
    if (target.is(Tree.Kind.MEMBER_SELECT)) {
      target = ((MemberSelectExpressionTree) target).identifier();
    }
    reportIssue(target, message, secondaries, null);
  }

}
