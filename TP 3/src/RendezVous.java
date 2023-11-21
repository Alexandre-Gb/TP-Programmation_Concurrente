import java.util.Objects;

public class RendezVous<V> {
  private V value;
  private final Object lock = new Object();

  public void set(V value) {
    synchronized (lock) {
      Objects.requireNonNull(value);
      this.value = value;
    }
  }

  public V get() {
    for(;;) {
      synchronized (lock) {
        if (value != null) {
          return value;
        }
      }
    }
  }
}
