import java.util.Objects;

public class RendezVous<V> {
  private V value;
  private final Object lock = new Object();

  public void set(V value) {
    synchronized (lock) {
      Objects.requireNonNull(value);
      this.value = value;
      lock.notifyAll();
    }
  }

  public V get() throws InterruptedException {
    synchronized (lock) {
      while (value == null) {
        lock.wait();
      }
      return value;
    }
  }

  public static void main(String[] args) throws InterruptedException {
    var rdv = new RendezVous<String>();
    Thread.ofPlatform().start(() -> {
      try {
        Thread.sleep(10_000);
        rdv.set("Message");
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    });
    System.out.println(rdv.get());
  }
}
