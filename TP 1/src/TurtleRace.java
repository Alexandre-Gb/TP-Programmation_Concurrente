import java.util.stream.IntStream;

public final class TurtleRace {
  public static void main(String[] args) {
    System.out.println("On your mark!");

    try {
      Thread.sleep(15_000);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    System.out.println("Go!");
    int[] times = {25_000, 10_000, 20_000, 5_000, 50_000, 60_000};

    IntStream.range(0, times.length).forEach(i -> {
      Thread.ofPlatform().start(() -> {
        try {
          Thread.sleep(times[i]);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
        System.out.println("Turtle " + i + " has finished !");
      });
    });
  }
}
