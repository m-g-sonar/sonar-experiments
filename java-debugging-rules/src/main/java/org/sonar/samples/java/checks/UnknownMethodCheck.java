package org.sonar.samples.java.checks;

import com.google.common.collect.ImmutableList;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.plugins.java.api.tree.Tree.Kind;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(key = "UnknownMethodInvocation",
  name = "Unknown Method invocation",
  description = "This rule detects unknown method invocation",
  tags = {"debugging"})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.RELIABILITY_COMPLIANCE)
@SqaleConstantRemediation("10min")
public class UnknownMethodCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    if (mit.symbol().isUnknown()) {
      Tree reportTree = mit.methodSelect();
      if (reportTree.is(Tree.Kind.MEMBER_SELECT)) {
        reportTree = ((MemberSelectExpressionTree) reportTree).identifier();
      }
      reportIssue(reportTree, "Unknown method");
    }
  }

}
