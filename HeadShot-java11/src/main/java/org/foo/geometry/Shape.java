package org.foo.geometry;

public abstract sealed class Shape permits Circle,Rectangle,Square {

  private static final String intro = """
    Hello Ladies,
    did I just say that?
    """;

  private static final String html = """
                <html>
                    <body>
                        <p>Hello, world</p>
                    </body>
                </html>
                """;

  private static final String format = String.format("""
    SELECT "EMP_ID", "LAST_NAME" FROM "EMPLOYEE_TB"
    WHERE "CITY" = 'INDIANAPOLIS' AND "POPULATION" > %d
    ORDER BY "EMP_ID", "LAST_NAME";
    """, 50000);

  private static final String empty = """
    ""
    """;

  public static void main(String[] args) {
    Range r = new Range(0, 42);
    System.out.println(Range.average);
    try {
      new Range(13, 0);
    } catch (IllegalArgumentException e) {
      System.out.println("caught \"" + e.getMessage() + "\"");
    }
    System.out.println(Range.average);
    System.out.println(r.average());

    patternMatchingInstanceOf(r);
    patternMatchingInstanceOf("hello");
    patternMatchingInstanceOf("hell");
  }

  private static void patternMatchingInstanceOf(Object o) {
    if (o instanceof Range r) {
      System.out.println(r.hi());
    } else if (o instanceof String s && s.length() > 4) {
      System.out.println(s);
    } else {
      System.out.println("unknown");
    }

    if (!(o instanceof String s)) {
      System.out.println(o.toString());
    } else {
        System.out.println(s);
    }
  }

  record Range(int lo, int hi) {

    static int average;

    Range {
      if (lo > hi) {
        throw new IllegalArgumentException(String.format("(%d,%d)", lo, hi));
      }
      average = (hi + lo) / 2;
    }

    int average() {
      return (hi + lo) / 2;
    }

    @Override
    public String toString() {
      return String.format("(%d,%d)", lo, hi);
    }
  }
}

final class Circle extends Shape {
}

sealed class Rectangle extends Shape permits TransparentRectangle,FilledRectangle {
}

final class TransparentRectangle extends Rectangle {
}

final class FilledRectangle extends Rectangle {
}

non-sealed class Square extends Shape {
}
