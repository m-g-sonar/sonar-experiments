package org.foo;

import java.util.regex.Pattern;

public class A {

  public static void main(String[] args) {
    Pattern p = Pattern.compile("""
      [

      ]
      """);
    System.out.println(p);
  }
}
