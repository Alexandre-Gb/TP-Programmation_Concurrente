import java.util.ArrayList;
import java.util.stream.Collectors;

public class ThreadSafeList {
  private final ArrayList<Integer> list = new ArrayList<>();
  private final Object lock = new Object();

  public void add(Integer i) {
    synchronized (lock) {
      list.add(i);
    }
  }

  public int size() {
    synchronized (lock) {
      return list.size();
    }
  }

  @Override
  public String toString() {
    synchronized (lock) {
      return list.toString();

      // Equivalent to the default tostring method of a list
      // return list.stream().map(Object::toString)
      //   .collect(Collectors.joining(", "));
    }
  }
}
