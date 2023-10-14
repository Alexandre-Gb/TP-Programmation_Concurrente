import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class UnboundedSafeQueue<V> {
  private final ArrayDeque<V> fifo = new ArrayDeque<>();
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public void add(V value) {
    Objects.requireNonNull(value);
    lock.lock();
    try {
      fifo.add(value);
      condition.signal();
    } finally {
      lock.unlock();
    }
  }

  public V take() throws InterruptedException {
    lock.lock();
    try {
      while (fifo.isEmpty()) {
        condition.await();
      }
      return fifo.remove();
    } finally {
      lock.unlock();
    }
  }
}