import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class MyThreadSafeClass {
  private int counter;
  private int sum;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public void propose(int value) {
    lock.lock();
    try {
      if (counter == 10) {
        return;
      }
      sum += value;
      counter++;

      if (counter == 10) {
        condition.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  public int sum() throws InterruptedException {
    lock.lock();
    try {
      while (counter < 10) {
        condition.await();
      }
      return sum;
    } finally {
      lock.unlock();
    }
  }

  public static boolean isPrime(long l) {
    if (l <= 1) {
      return false;
    }
    for (long i = 2L; i <= l / 2; i++) {
      if (l % i == 0) {
        return false;
      }
    }
    return true;
  }

  public static void main(String[] args) {
    int nbThreads = 5;
    var myThreadSafeClass = new MyThreadSafeClass();

    IntStream.range(0, nbThreads).forEach(e -> {
      Thread.ofPlatform().daemon().start(() -> {
        var random = new Random();
        for (;;) {
          long nb = 1_000_000_000L + (random.nextLong() % 1_000_000_000L);
          if (isPrime(nb)) {
            myThreadSafeClass.propose((int) nb);
          }
        }
      });
    });
  }
}
