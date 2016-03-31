class A {
  void foo() {
    foo(); // Compliant
    bar(); // Noncompliant [[sc=5;ec=8]]
    Unknown.bar(); // Noncompliant [[sc=5;ec=16]]
  }
}