import java.util.Objects;

/**
 * Note: this code does several stupid things !
 */
public class StupidRendezVous<V> {
  private V value;

  public void set(V value) {
    Objects.requireNonNull(value);
    this.value = value;
  }

  public V get() throws InterruptedException {
    while (value == null) {
      // Thread.sleep(1); // then comment this line !
    }
    return value;
  }
}