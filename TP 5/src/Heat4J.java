import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class Heat4J {

	/**
	 * Return the temperature in Celcius of the sensor located in roomName. This
	 * method is thread-safe.
	 *
	 * @param roomName name of the room equipped with a sensor
	 */
	public static int retrieveTemperature(String roomName) throws InterruptedException {
		Thread.sleep(Math.abs(ThreadLocalRandom.current().nextInt() % 1000));
		return 10 + (ThreadLocalRandom.current().nextInt() % 20);
	}

	public static class Application {
		public static void main(String[] args) throws InterruptedException {
			var rooms = List.of("bedroom1", "bedroom2", "kitchen", "dining-room", "bathroom", "toilets");

			var temperatures = new ArrayList<Integer>();

			for (var room : rooms) {
				var temperature = Heat4J.retrieveTemperature(room);
				System.out.println("Temperature in room " + room + " : " + temperature);
				temperatures.add(temperature);
			}

			System.out.println(temperatures.stream().mapToInt(Integer::intValue).average().getAsDouble());
		}
	}
}