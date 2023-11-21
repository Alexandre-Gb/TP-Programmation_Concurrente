import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class CurrentMaximum {
  public static int MAX_VALUE = 10_000;
  private Integer value;
  private Thread maxThread;
  private final Object lock = new Object();

  public Optional<Integer> max() {
    synchronized (lock) {
      // return value == null ? Optional.empty() : Optional.of(value);
      return Optional.ofNullable(value);
    }
  }

  public void propose(int newValue) {
    synchronized (lock) {
      if (value == null || newValue > value) {
        value = newValue;
        maxThread = Thread.currentThread();
      }
    }
  }

  public Optional<Thread> maxThread() {
    synchronized (lock) {
      return Optional.ofNullable(maxThread);
    }
  }

  public static class Main {
    public static void main(String[] args) {
      var nbThreads = 4;
      var nbLoops = 10;
      var threadList = new ArrayList<Thread>(nbThreads);
      var currentMaximum = new CurrentMaximum();

      IntStream.range(0, nbThreads).forEach(i -> {
        threadList.add(Thread.ofPlatform().name("Thread-" + i).start(() -> {
          for (int j = 0; j < nbLoops; j++) {
            try {
              Thread.sleep(1_000);
            } catch (InterruptedException e) {
              throw new AssertionError(e);
            }

            var newValue = ThreadLocalRandom.current().nextInt(MAX_VALUE);
            System.out.println(Thread.currentThread().getName() + " propose " + newValue);
            currentMaximum.propose(newValue);
          }
        }));
      });

      Optional<Integer> max;
      Optional<Thread> maxThread;
      for (int i = 0; i < nbLoops; i++) {
        try {
          Thread.sleep(1_000);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }

        max = currentMaximum.max();
        maxThread = currentMaximum.maxThread();
        if (max.isPresent() && maxThread.isPresent()) {
          System.out.println("Max courant : " + max.get() + " proposé par " + maxThread.get().getName());
        } else {
          System.out.println("Pas de max courant pour le moment.");
        }
//        max.ifPresentOrElse(
//                val -> System.out.println("Max courant : " + val + " proposé par " + maxThread.get()),
//                () -> System.out.println("Pas de max courant pour le moment.")
//        );
      }

      for (var thread : threadList) {
        try {
          thread.join();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }

      max = currentMaximum.max();
      maxThread = currentMaximum.maxThread();
      if (max.isPresent() && maxThread.isPresent()) {
        System.out.println("Max final : " + max.get() + " proposé par " + maxThread.get().getName());
      } else {
        System.out.println("Pas de max final.");
      }
//      max.ifPresentOrElse(
//              val -> System.out.println("Max final : " + val + " proposé par " + ),
//              () -> System.out.println("Pas de max final.")
//      );
    }
  }
}
