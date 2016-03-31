package org.sonar.samples.java.checks;

import com.google.common.collect.ImmutableList;

import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.check.Rule;
import org.sonar.plugins.java.api.tree.NewClassTree;
import org.sonar.plugins.java.api.tree.Tree;
import org.sonar.squidbridge.annotations.SqaleConstantRemediation;
import org.sonar.squidbridge.annotations.SqaleSubCharacteristic;

import java.util.List;

@Rule(key = "UnknownConstructorCall",
  name = "Unknown Constructor Call",
  description = "This rule detects unknown constructors (not resolved)",
  tags = {"debugging"})
@SqaleSubCharacteristic(RulesDefinition.SubCharacteristics.RELIABILITY_COMPLIANCE)
@SqaleConstantRemediation("10min")
public class UnknownConstructorCheck extends UnknownTypeAbstractCheck {

  @Override
  public List<Tree.Kind> nodesToVisit() {
    return ImmutableList.of(Tree.Kind.NEW_CLASS);
  }

  @Override
  public void visitNode(Tree tree) {
    NewClassTree nct = (NewClassTree) tree;
    if (nct.constructorSymbol().isUnknown()) {
      reportUnknownType(nct.identifier(), "Unknown constructor", getMissingArguments(nct.arguments()));
    }
  }

}
