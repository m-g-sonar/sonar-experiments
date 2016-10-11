package org.sonar.samples.java.checks;

import com.google.common.collect.ImmutableList;

import org.sonar.check.Rule;
import org.sonar.plugins.java.api.IssuableSubscriptionVisitor;
import org.sonar.plugins.java.api.semantic.Type;
import org.sonar.plugins.java.api.tree.ExpressionTree;
import org.sonar.plugins.java.api.tree.MemberSelectExpressionTree;
import org.sonar.plugins.java.api.tree.MethodInvocationTree;
import org.sonar.plugins.java.api.tree.Tree;

import java.util.List;

@Rule(key = "MethodInvocationReturnType")
public class MethodInvocationReturnTypeCheck extends IssuableSubscriptionVisitor {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.METHOD_INVOCATION);
  }

  @Override
  public void visitNode(Tree tree) {
    MethodInvocationTree mit = (MethodInvocationTree) tree;
    ExpressionTree methodSelect = mit.methodSelect();

    Tree report = methodSelect.is(Tree.Kind.MEMBER_SELECT) ? ((MemberSelectExpressionTree) methodSelect).identifier() : methodSelect;

    reportIssue(report, prettyPrint(mit.symbolType()));
  }

  private static String prettyPrint(Type type) {
    if (type.isUnknown()) {
      return "Unknown";
    }
    return type.toString();
  }

}
