class A {
  void foo() {
    foo(); // Compliant
    bar(); // Noncompliant
  }
}