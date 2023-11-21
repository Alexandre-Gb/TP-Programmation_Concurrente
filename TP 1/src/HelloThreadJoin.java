import java.util.ArrayList;
import java.util.stream.IntStream;

public final class HelloThreadJoin {
  private final int nbThread;

  public HelloThreadJoin(int nbThread) {
    if (nbThread < 0) {
      throw new IllegalArgumentException("nbThread must be positive");
    }

    this.nbThread = nbThread;
  }

  public void run() {
    var threads = new ArrayList<Thread>(nbThread);
    IntStream.range(0, nbThread).forEach(i -> threads.add(Thread.ofPlatform().start(() -> {
      for(int j = 0; j <= 5000; j++) {
        System.out.println("hello " + i + " " + j);
      }
    })));

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