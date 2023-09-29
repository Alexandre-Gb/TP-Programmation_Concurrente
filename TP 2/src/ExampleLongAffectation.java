class ExampleLongAffectation {
  long l = -1L;

  public static void main(String[] args) {
    var e = new ExampleLongAffectation();
    Thread.ofPlatform().start(() -> {
      System.out.println("l = " + e.l);
    });
    e.l = 0;
  }
}