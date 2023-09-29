import java.util.ArrayList;
import java.util.stream.IntStream;

public class HelloListBug  {
  public static void main(String[] args) throws InterruptedException {
    var nbThreads = 4;
    var threads = new Thread[nbThreads];
    var list = new ArrayList<Integer>(5000 * nbThreads);

    IntStream.range(0, nbThreads).forEach(j -> {
      Runnable runnable = () -> {
        for (var i = 0; i < 5000; i++) {
          list.add(i);
        }
      };

      threads[j] = Thread.ofPlatform().start(runnable);
    });

    for (var thread : threads) {
      thread.join();
    }

    // Print once all threads are finished
    System.out.println("list.size() = " + list.size());

    System.out.println("le programme est fini");
  }
}
