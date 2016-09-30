class A {
  void foo() {
    new A()
      .bar()  // Noncompliant {{A}}
      .qix()  // Noncompliant {{B}}
      .yolo() // Noncompliant {{String}}
      .length(); // Noncompliant {{int}}

    new C<A>()
      .foo()  // Noncompliant {{C<A>}}
      .bar(); // Noncompliant {{A}}

    C<C<B[]>> cca = new C<>();
    cca
      .foo()   // Noncompliant {{C<C<B[]>>}}
      .boyo(); // Noncompliant {{Unknown}}
  }

  A bar() {
    return null;
  }

  B qix() {
    return null;
  }
}

class B extends A {
  String yolo() {
    return null;
  }
}

class C<T> {
  T bar() {
    return null;
  }

  C<T> foo() {
    return null;
  }
}