import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Heat4JThreadSafe {
  private final HashMap<String, Integer> temperatures = new HashMap<>();
  private final int maxRooms;
  private final Object lock = new Object();

  public Heat4JThreadSafe(int maxRooms) {
    if (maxRooms < 1) { throw new IllegalArgumentException(); }
    this.maxRooms = maxRooms;
  }

  public int retrieveTemperature(String roomName) {
    Objects.requireNonNull(roomName);

    synchronized (lock) {
      if (temperatures.size() == maxRooms) { throw new IllegalStateException("No more space available."); }
      if (temperatures.containsKey(roomName)) { throw new IllegalStateException("The room was already given."); }

      int temperature;
      try {
        temperature = Heat4J.retrieveTemperature(roomName);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      temperatures.put(roomName, temperature);

      if (temperatures.size() == maxRooms) {
        lock.notify();
      }
      return temperature;
    }
  }

  public double retrieveAverageTemperature() throws InterruptedException {
    synchronized (lock) {
      while (temperatures.size() != maxRooms) {
        lock.wait();
      }

      return temperatures.values().stream()
              .mapToInt(Integer::intValue)
              .average()
              .getAsDouble();
    }
  }

  public static class Application {
    public static void main(String[] args) throws InterruptedException {
      var rooms = List.of("bedroom1", "bedroom2", "kitchen", "dining-room", "bathroom", "toilets");
      var heat4J = new Heat4JThreadSafe(rooms.size());

      for (var room : rooms) {
        Thread.ofPlatform().start(() -> {
          var temperature = heat4J.retrieveTemperature(room);
          System.out.println("Temperature in room " + room + " : " + temperature);
        });
      }

      System.out.println("Average temperature: " + heat4J.retrieveAverageTemperature());
    }
  }
}
