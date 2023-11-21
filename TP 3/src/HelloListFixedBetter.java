import java.util.stream.IntStream;

public class HelloListFixedBetter {
  public static void main(String[] args) throws InterruptedException {
    var nbThreads = 4;
    var threads = new Thread[nbThreads];
    var fixedList = new ThreadSafeList();

    IntStream.range(0, nbThreads).forEach(j -> {
      Runnable runnable = () -> {
        for (var i = 0; i < 5_000; i++) {
          fixedList.add(i);
        }
      };

      threads[j] = Thread.ofPlatform().start(runnable);
    });

    for (Thread thread : threads) {
      thread.join();
    }

    System.out.println("taille de la liste:" + fixedList.size());
    System.out.println("liste: " + fixedList);
  }
}
