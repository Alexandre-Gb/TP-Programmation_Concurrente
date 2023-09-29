import java.util.ArrayList;
import java.util.stream.IntStream;

public final class HelloThreadJoin {
  private final int nbThread;
  private final ArrayList<Thread> threads = new ArrayList<>();

  public HelloThreadJoin(int nbThread) {
    if (nbThread < 0) {
      throw new IllegalArgumentException("nbThread must be positive");
    }

    this.nbThread = nbThread;
  }

  public void run() {
    IntStream.range(0, nbThread).forEach(i -> {
      Runnable runnable = () -> {
        for(int j = 0; j <= 5000; j++) {
          System.out.println("hello " + i + " " + j);
        }
      };
      Thread thread = Thread.ofPlatform().start(runnable);
      threads.add(thread);
    });
    threads.forEach(thread -> {
        try {
          thread.join();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    );
    System.out.println("Le thread a fini son Runnable");
  }

  public static void main(String[] args) {
    var threads = new HelloThreadJoin(4);
    threads.run();
  }
}