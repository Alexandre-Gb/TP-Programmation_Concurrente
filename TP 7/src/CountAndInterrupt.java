import java.util.ArrayList;
import java.util.Scanner;
import java.util.stream.IntStream;

public class CountAndInterrupt {
  public static void main(String[] args) {
    var nbThreads = 4;
    var threads = new ArrayList<Thread>(nbThreads);

    IntStream.range(0, nbThreads).forEach(i -> {
      threads.add(Thread.ofPlatform().daemon().start(()-> {
        var count = 0;
        for (;;) {
          System.out.println("Current counting value : " + count);
          System.out.println("Thread Id : " + Thread.currentThread().threadId());
          try {
            Thread.sleep(1_000);
          } catch (InterruptedException e) {
            return;
          }
          count++;
        }
      }));
    });

    System.out.println("enter a thread id:");
    try (var scanner = new Scanner(System.in)) {
      while (scanner.hasNextInt()) {
        var threadId = scanner.nextInt();
        for (var thread : threads) {
          if (thread.threadId() == threadId) {
            thread.interrupt();
          }
        }
      }
    }
  }
}
