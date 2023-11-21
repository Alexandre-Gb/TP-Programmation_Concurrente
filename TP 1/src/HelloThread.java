import java.util.stream.IntStream;

public final class HelloThread {
  private final int nbThread;

  public HelloThread(int nbThread) {
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
      Thread.ofPlatform().start(runnable);
    });
  }

  public static void main(String[] args) {
    var threads = new HelloThread(4);
    threads.run();
  }
}