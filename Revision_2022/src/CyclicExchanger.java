import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class CyclicExchanger<T> {
  private final int nbParticipants;
  private int nbWaiting;
  private final T[] values;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  @SuppressWarnings("unchecked")
  public CyclicExchanger(int nbParticipants) {
    if (nbParticipants < 2) {
      throw new IllegalArgumentException();
    }
    this.nbParticipants = nbParticipants;
    this.values = (T[]) new Object[nbParticipants];
  }

  public T exchange(T value) throws InterruptedException {
    lock.lock();
    try {
      if (nbWaiting >= nbParticipants) {
        throw new IllegalStateException();
      }

      int index = nbWaiting;
      values[index] = value;
      nbWaiting++;

      if (nbWaiting == nbParticipants) {
        condition.signalAll();
      } else {
        while (nbWaiting < nbParticipants) {
          condition.await();
        }
      }

      var result = values[(index + 1) % nbParticipants];
      values[(index + 1) % nbParticipants] = null;
      return result;
    } finally {
      lock.unlock();
    }
  }

  public static void main(String[] args) {
    var nbThreads = 5;
    var cyclicExchanger = new CyclicExchanger<Integer>(nbThreads);

    IntStream.range(0, nbThreads).forEach(i -> {
      Thread.ofPlatform().name("Thread " + i).start(() -> {
        try {
          Thread.sleep(i * 1_000L);
          var value = cyclicExchanger.exchange(i);
          System.out.println("Thread " + i + " received " + value);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      });
    });
  }
}
