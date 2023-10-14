import java.nio.channels.AsynchronousCloseException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class SemaphoreClosable {
  private int nbPermits;
  private int nbWaiting;
  private boolean isClosed;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public SemaphoreClosable(int nbPermits) {
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
      if (isClosed) {
        throw new IllegalStateException("Semaphore is closed.");
      }
      if (nbPermits > 1) {
        nbPermits--;
        return true;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  public void acquire() throws InterruptedException, AsynchronousCloseException {
    lock.lock();
    try {
      if (isClosed) {
        throw new IllegalStateException("Semaphore is closed.");
      }
      nbWaiting++;
      while (nbPermits == 0) {
        condition.await();
        if (isClosed) {
          throw new AsynchronousCloseException();
        }
      }
      nbWaiting--;
      nbPermits--;
    } finally {
      lock.unlock();
    }
  }

  public void release() {
    lock.lock();
    try {
      if (isClosed) {
        return;
      }
      nbPermits++;
      condition.signal();
    } finally {
      lock.unlock();
    }
  }

  public int waitingForPermits() {
    lock.lock();
    try {
      return nbWaiting;
      // return lock.getHoldCount();
    } finally {
      lock.unlock();
    }
  }

  public void close() {
    lock.lock();
    try {
      if (!isClosed) {
        isClosed = true;
        condition.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  public static void main(String[] args) {
    var semaphore = new SemaphoreClosable(5);
    var nbThreads = 10;

    IntStream.range(0, nbThreads).forEach(e -> {
      Thread.ofPlatform().name("Thread " + e).start(() -> {
        try {
          semaphore.acquire();
          System.out.println(Thread.currentThread().getName() + " has acquired its permit.");
          Thread.sleep(1_000);
        } catch (InterruptedException | AsynchronousCloseException ex) {
          throw new AssertionError(ex);
        }
        System.out.println(Thread.currentThread().getName() + " has released its permit.");
        semaphore.release();
      });
    });

    for (int i = 1;; i++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new AssertionError();
      }

      var waitingForPermits = semaphore.waitingForPermits();
      System.out.println(i + " seconds: " + waitingForPermits + " thread(s) currently waiting for a permit.");

      if (i == 10 && waitingForPermits != 0) {
        semaphore.close();
        break;
      }
    }
  }
}
