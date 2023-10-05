import java.util.Optional;

public class CurrentMaximum {
  public static int MAX_VALUE = 10_000;
  private Integer value;
  private final Object lock = new Object();

  public Optional<Integer> max() {
    synchronized (lock) {
      // return value == null ? Optional.empty() : Optional.of(value);
      return Optional.ofNullable(value);
    }
  }

  public void propose(int newValue) {
    synchronized (lock) {
      if (this.value == null || newValue > this.value) {
        this.value = newValue;
      }
    }
  }
}
