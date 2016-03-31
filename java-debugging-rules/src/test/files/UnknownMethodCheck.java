package org.foo;

class A {
  A(String s, int i) {}

  void foo() {
    new A("hello", 0); // Compliant
    new A( // Noncompliant [[sc=9;ec=10;secondary=9]] {{Unknown constructor}}
      unknownVar
      , 1);
    new org.foo.A(); // noncompliant [[sc=17;ec=18]] {{Unknown constructor}}

    foo(); // Compliant
    gul("hello", 0); // Compliant

    bar(); // Noncompliant [[sc=5;ec=8]] {{Unknown method}}
    Unknown.bar(); // Noncompliant [[sc=13;ec=16]]
    this.bar(); // Noncompliant [[sc=10;ec=13]]
    gul( // Noncompliant [[sc=5;ec=8;secondary=20]]
      unknownVar
      , 1);
  }

  void gul(String s, int i) {}
}