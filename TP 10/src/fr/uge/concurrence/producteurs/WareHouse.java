package fr.uge.concurrence.producteurs;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

public class WareHouse {
	private static final Random random = new Random();
	public static final int DESTINATIONS = 5;

	public static class Order {
		private final String name;
		private final int location;

		private Order(String name, int location) {
			this.name = name;
			this.location = location;
		}

		private static Order newOrder() {
			String name = random.ints(4, (int) 'a', (int) 'z').mapToObj(x -> Character.toString((char) x))
					.collect(Collectors.joining());
			return new Order(name, random.nextInt(DESTINATIONS));
		}

		public boolean checkLocation(int location) {
			return location == this.location;
		}

		@Override
		public String toString() {
			return name + "#" + location;
		}
	}

	public static Order nextOrder() {
		return Order.newOrder();
	}

	public static Optional<Integer> prepareParcel(Order order) {
		if (random.nextBoolean() && random.nextBoolean()) {
			return Optional.empty();
		}
		try {
			Thread.sleep(300 * random.nextInt(4));
		} catch (InterruptedException e) {
			return Optional.empty();
		}
		return Optional.of(order.location);
	}

	public static void checkDelivery(int location, List<Order> orders) {
		if (orders.size() != 10) {
			throw new IllegalArgumentException("need exactly 10 parcels");
		}
		if (orders.stream().anyMatch(o -> o.location != location)) {
			throw new IllegalArgumentException("wrong location");
		}
	}
}
