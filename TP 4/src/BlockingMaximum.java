import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class BlockingMaximum {
  public static final int MAX_VALUE = 10_000;
  private Integer currentMax;
  private final Object lock = new Object();

  public Optional<Integer> getMax() throws InterruptedException {
    synchronized (lock) {
      lock.wait();
      return Optional.ofNullable(currentMax);
    }
  }

  public void suggest(int value) {
    synchronized (lock) {
      if (value > 8_000 && (currentMax == null || value > currentMax)) {
        currentMax = value;
        lock.notifyAll();
      }
    }
  }

  public static class Application {
    public static void main(String[] args) {
      int nbThreads = 4;
      var blockingMaximum = new BlockingMaximum();

      IntStream.range(0, nbThreads).forEach(i -> {
        Thread.ofPlatform().start(() -> {
          for(;;) {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              throw new AssertionError(e);
            }
            int randint = ThreadLocalRandom.current().nextInt(BlockingMaximum.MAX_VALUE);

            System.out.println(Thread.currentThread().getName() + " a tiré " + randint);
            blockingMaximum.suggest(randint);
          }
        });
      });

      for(;;) {
        Optional<Integer> max; // = Optional.empty() by default
        try {
          max = blockingMaximum.getMax();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
        max.ifPresent(value -> System.out.println("Premier max après 8 000 : " + value));
      }
    }
  }
}
