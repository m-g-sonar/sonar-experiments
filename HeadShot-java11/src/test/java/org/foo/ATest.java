package org.foo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ATest {

  @Test
  void testName() throws Exception {
    assertEquals(-1, new A().foo(false));
  }
}

