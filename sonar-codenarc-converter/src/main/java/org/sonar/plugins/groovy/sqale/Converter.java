package org.sonar.plugins.groovy.sqale;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.FileReader;

public class Converter {
  private static final String SQALE_MODEL_LOCATION = "/home/mgumowski/wks/git/sonar-groovy/sonar-groovy-plugin/src/main/resources/com/sonar/sqale/groovy-model.xml";

  public static void main(String[] args) throws Exception {
    Document sqaleModel = parseXml(new File(SQALE_MODEL_LOCATION));
    Node sqale = sqaleModel.getFirstChild();
    NodeList categories = sqale.getChildNodes();

    Converter converter = new Converter();
    converter.handleCategories(categories);
  }

  private int ruleCounter = 0;

  private void handleCategories(NodeList categories) {
    for (int c = 0; c < categories.getLength(); c++) {
      Node category = categories.item(c);
      NodeList childNodes = category.getChildNodes();
      String categoryName = "UNKNOWN";
      for (int i = 0; i < childNodes.getLength(); i++) {
        Node child = childNodes.item(i);
        String nodeName = child.getNodeName();
        if ("key".equals(nodeName)) {
          categoryName = getTextValue(child);
        } else if ("chc".equals(nodeName)) {
          handleSubCategories(child, categoryName);
        }
      }
    }
  }

  private void handleSubCategories(Node subCategory, String categoryName) {
    NodeList childNodes = subCategory.getChildNodes();
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node child = childNodes.item(i);
      String nodeName = child.getNodeName();
      String subCategoryName = "UNKNOWN";
      if ("key".equals(nodeName)) {
        subCategoryName = getTextValue(child);
      } else if ("chc".equals(nodeName)) {
        ExtractedRule extractedRule = handleRule(child, ++ruleCounter, categoryName, subCategoryName);
        showRule(extractedRule);
      }
    }
  }

  private void showRule(ExtractedRule rule) {
    System.out.println(rule.toString());
  }

  private static ExtractedRule handleRule(Node rule, int id, String categoryName, String subCategoryName) {
    NodeList childNodes = rule.getChildNodes();
    ExtractedRule extractedRule = new ExtractedRule(id, categoryName, subCategoryName);
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node child = childNodes.item(i);
      String nodeName = child.getNodeName();
      if ("rule-key".equals(nodeName)) {
        extractedRule.ruleKey = getTextValue(child);
      } else if ("prop".equals(nodeName)) {
        handleProperty(extractedRule, child);
      }
    }
    return extractedRule;
  }

  private static void handleProperty(ExtractedRule extractedRule, Node node) {
    NodeList childNodes = node.getChildNodes();
    Property property = Property.OTHER;
    for (int i = 0; i < childNodes.getLength(); i++) {
      Node child = childNodes.item(i);
      String nodeName = child.getNodeName();
      if ("key".equals(nodeName)) {
        property = Property.getProp(getTextValue(child));
      } else if ("val".equals(nodeName) || "txt".equals(nodeName)) {
        extractedRule.setProperty(property, getTextValue(child));
      }
    }
  }

  private enum Property {
    REMEDIATION_FUNCTION, REMEDIATION_FACTOR, OFFSET, OTHER;

  private static Property getProp(String s) {
      if ("remediationFunction".equals(s)) {
        return REMEDIATION_FUNCTION;
      } else if ("remediationFactor".equals(s)) {
        return REMEDIATION_FACTOR;
      } else if ("offset".equals(s)) {
        return OFFSET;
      }
      return OTHER;
    }
  }

  private static class ExtractedRule {
    final int id;
    final String category;
    final String subCategory;

    String ruleKey;
    String remediationFunction = "";
    String remediationFactor = "";
    String offset = "";

    public ExtractedRule(int id, String category, String subCategory) {
      this.id = id;
      this.category = category;
      this.subCategory = subCategory;
    }

    public void setProperty(Property property, String textValue) {
      switch (property) {
        case REMEDIATION_FACTOR:
          remediationFactor += textValue;
          break;
        case REMEDIATION_FUNCTION:
          remediationFunction += textValue;
          break;
        case OFFSET:
          offset += textValue;
          break;
        default:
          // do nothing
          break;
      }
    }

    @Override
    public String toString() {
      return ruleKey + ";" + remediationFunction + ";" + remediationFactor;
    }
  }

  private static String getTextValue(Node node) {
    return node.getFirstChild().getTextContent();
  }

  private static Document parseXml(File f) throws Exception {
    try {
      DocumentBuilderFactory documentBuilderFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder documentBuilder;
      documentBuilder = documentBuilderFactory.newDocumentBuilder();
      InputSource is = new InputSource();
      is.setCharacterStream(new FileReader(f));
      return documentBuilder.parse(is);
    } catch (Exception e) {
      throw new Exception(e);
    }
  }
}
