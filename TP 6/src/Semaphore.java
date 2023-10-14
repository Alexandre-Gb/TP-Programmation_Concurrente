import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class Semaphore {
  private int nbPermits;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public Semaphore(int nbPermits) {
    lock.lock();
    try {
      this.nbPermits = nbPermits;
    } finally {
      lock.unlock();
    }
  }

  public boolean tryAcquire() {
    lock.lock();
    try {
      if (nbPermits > 1) {
        nbPermits--;
        return true;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  public void acquire() throws InterruptedException {
    lock.lock();
    try {
      while (nbPermits == 0) {
        condition.await();
      }
      nbPermits--;
    } finally {
      lock.unlock();
    }
  }

  public void release() {
    lock.lock();
    try {
      nbPermits++;
      condition.signal();
    } finally {
      lock.unlock();
    }
  }

  public static void main(String[] args) {
    var semaphore = new Semaphore(5);
    var nbThreads = 10;

    IntStream.range(0, nbThreads).forEach(e -> {
      Thread.ofPlatform().name("Thread " + e).start(() -> {
        try {
          semaphore.acquire();
          System.out.println(Thread.currentThread().getName() + " has acquired its permit.");
          Thread.sleep(1_000);
        } catch (InterruptedException ex) {
          throw new AssertionError(ex);
        }
        System.out.println(Thread.currentThread().getName() + " has released its permit.");
        semaphore.release();
      });
    });
  }
}
