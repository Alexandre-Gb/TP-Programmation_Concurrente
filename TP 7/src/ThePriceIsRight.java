import java.util.LinkedHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThePriceIsRight {
  private final int price;
  private final int nbThreads;
  private final LinkedHashMap<Thread, Integer> propositions = new LinkedHashMap<>();
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition notAllPropositions = lock.newCondition();

  public ThePriceIsRight(int price, int nbThreads) {
    if (price < 0 || nbThreads < 1) {
      throw new IllegalArgumentException();
    }

    this.price = price;
    this.nbThreads = nbThreads;
  }

  public boolean propose(int proposition) throws InterruptedException {
    lock.lock();
    try {
      propositions.put(Thread.currentThread(), proposition);
      notAllPropositions.signalAll();
      while (propositions.size() != nbThreads) {
        notAllPropositions.await();
      }

      int closest = Integer.MAX_VALUE;
      for (int p : propositions.values()) {
        if (Math.abs(p - price) < Math.abs(closest - price)) {
          closest = p;
        }
      }

      return closest == proposition;
    } finally {
      lock.unlock();
    }
  }

  public static void main(String[] args) {

  }
}
