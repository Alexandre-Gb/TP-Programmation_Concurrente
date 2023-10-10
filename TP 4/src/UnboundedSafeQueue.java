import java.util.ArrayDeque;
import java.util.stream.IntStream;

public class UnboundedSafeQueue<V> {
  private final ArrayDeque<V> queue = new ArrayDeque<>();

  public void add(V value) {
    synchronized (queue) {
      queue.addLast(value);
      queue.notifyAll();
    }
  }

  public V take() throws InterruptedException {
    synchronized (queue) {
      while (queue.isEmpty()) {
        queue.wait();
      }
      return queue.removeFirst();
    }
  }

  public static void main(String[] args) throws InterruptedException {
    UnboundedSafeQueue<String> queue = new UnboundedSafeQueue<>();
    var nbThreads = 3;
    Thread[] threads = new Thread[nbThreads];

    IntStream.range(0, nbThreads).forEach(i -> {
      Runnable runnable = () -> {
        try {
          Thread.sleep(2_000);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
        queue.add(Thread.currentThread().getName());
      };

      threads[i] = Thread.ofPlatform().start(runnable);
    });

    for (var thread : threads) {
      thread.join();
    }

    for (int i = 0; i < nbThreads; i++) {
      System.out.println(queue.take());
    }
  }
}
