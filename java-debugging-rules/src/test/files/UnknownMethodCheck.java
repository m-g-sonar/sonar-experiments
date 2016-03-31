class A {
  void foo() {
    foo(); // Compliant
    bar(); // Noncompliant [[sc=5;ec=8]]
    Unknown.bar(); // Noncompliant [[sc=13;ec=16]]
    this.bar(); // Noncompliant [[sc=10;ec=13]]
    gul("hello", 0); // Compliant
    gul( // Noncompliant [[sc=5;ec=8;secondary=9]]
      unknownVar,
      1);
  }

  void gul(String s, int i) {}
}