package org.foo;

import java.util.regex.Pattern;

public class A {

  public static void main(String[] args) {
    String regex = "[^a-c]+";
    Pattern pattern = Pattern.compile(regex);
    System.out.println(pattern.matcher("def").matches());
    System.out.println(pattern.matcher("abc").matches());
  }
}
