import java.util.ArrayDeque;
import java.util.Objects;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class BoundedSafeQueue<V> {
  private final ArrayDeque<V> fifo = new ArrayDeque<>();
  private final int capacity;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition conditionPut = lock.newCondition();
  private final Condition conditionTake = lock.newCondition();


  public BoundedSafeQueue(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException();
    }
    this.capacity = capacity;
  }

  public void put(V value) throws InterruptedException {
    Objects.requireNonNull(value);
    lock.lock();
    try {
      while (fifo.size() == capacity) {
        conditionPut.await();
      }
      fifo.add(value);
      conditionTake.signal();
    } finally {
      lock.unlock();
    }
  }

  public V take() throws InterruptedException {
    lock.lock();
    try {
      while (fifo.isEmpty()) {
        conditionTake.await();
      }
      conditionPut.signal();
      return fifo.remove();
    } finally {
      lock.unlock();
    }
  }
}