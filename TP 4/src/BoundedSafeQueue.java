import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.stream.IntStream;

public class BoundedSafeQueue<V> {
  private final ArrayDeque<V> queue = new ArrayDeque<>();
  private final int capacity;

  public BoundedSafeQueue(int capacity) {
    if (capacity < 1) throw new IllegalArgumentException("capacity must be > 0");
    this.capacity = capacity;
  }


  public void put(V value) throws InterruptedException {
    synchronized (queue) {
      while (queue.size() >= capacity) {
        queue.wait(); // Full
      }
      queue.addLast(value);
      queue.notifyAll();
    }
  }

  public V take() throws InterruptedException {
    synchronized (queue) {
      while (queue.isEmpty()) {
        queue.wait();
      }
      queue.notify();
      return queue.removeFirst();
    }
  }

  public static void main(String[] args) {
    int nbThreads = 3;
    int capacity = 2;
    var queue = new BoundedSafeQueue<String>(capacity);

    IntStream.range(0, nbThreads).forEach(i -> {
      Thread.ofPlatform().start(() -> {
        try {
          Thread.sleep(2_000);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
        try {
          queue.put(Thread.currentThread().getName());
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      });
    });

    for(;;) {
      try {
        System.out.println("First element in queue: " + queue.take());
      } catch (InterruptedException e) {
        throw new AssertionError();
      }
    }
  }
}
