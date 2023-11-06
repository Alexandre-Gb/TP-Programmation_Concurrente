import java.util.LinkedHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ThePriceIsRight {
  private final int price;
  private final int nbThreads;
  private boolean hasBeenInterrupted;
  private final LinkedHashMap<Thread, Integer> propositions = new LinkedHashMap<>();
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition notAllPropositions = lock.newCondition();

  public ThePriceIsRight(int price, int nbThreads) { // Lock not necessary since no assignation is made on non-final fields
    if (price < 0 || nbThreads < 1) {
      throw new IllegalArgumentException();
    }

    this.price = price;
    this.nbThreads = nbThreads;
  }

  public boolean propose(int proposition) {
    lock.lock();
    try {
      if (proposition < 0) {
        throw new IllegalArgumentException();
      }

      if (hasBeenInterrupted || propositions.containsKey(Thread.currentThread()) || propositions.size() == nbThreads) {
        return false;
      }

      propositions.put(Thread.currentThread(), proposition);

      if (propositions.size() == nbThreads) {
        notAllPropositions.signalAll();
      } else {
        while (!hasBeenInterrupted && propositions.size() != nbThreads) {
          notAllPropositions.await();
        }
      }

      var closestPrice = Integer.MAX_VALUE;
      for (int p : propositions.values()) {
        if (distance(p) < distance(closestPrice)) {
          closestPrice = p;
        }
      }

      return (closestPrice == proposition &&
              propositions.values().stream()
                      .filter(p -> p == proposition)
                      .count() == 1);
    } catch (InterruptedException e) {
      if (nbThreads != 1) {
        hasBeenInterrupted = true;
        propositions.remove(Thread.currentThread());
        notAllPropositions.signalAll();
      }

      return false;
    } finally {
      lock.unlock();
    }
  }

  private int distance(int proposition) {
    return Math.abs(proposition - price);
  }
}
