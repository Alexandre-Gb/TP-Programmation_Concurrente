import java.util.stream.IntStream;

public class ReusableExchanger<T> {
  private T value1 = null;
  private T value2 = null;
  private ExchangerStatus status = ExchangerStatus.EMPTY;
  private final Object lock = new Object();

  public T exchange(T value) throws InterruptedException {
    synchronized (lock) {
      while (status == ExchangerStatus.FULL) {
        lock.wait();
      }

      if(status == ExchangerStatus.EMPTY) {
        value1 = value;
        status = ExchangerStatus.FIRST_VALUE;

        while(status != ExchangerStatus.FULL) {
          lock.wait();
        }

        status = ExchangerStatus.EMPTY;
        lock.notifyAll();
        return value2;
      } else {
        value2 = value;
        status = ExchangerStatus.FULL;
        lock.notifyAll();
        return value1;
      }
    }
  }

  private enum ExchangerStatus { EMPTY, FIRST_VALUE, FULL }

  public static void main(String[] args) {
    var exchanger = new ReusableExchanger<String>();
    IntStream.range(0, 10).forEach(i -> {
      Thread.ofPlatform().start(() -> {
        try {
          System.out.println("Thread " + i + " received from " + exchanger.exchange("thread " + i));
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      });
    });
  }
}
