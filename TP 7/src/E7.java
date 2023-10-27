import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;

public class E7 {
  public static boolean isPrime(long candidate) {
    if (candidate <= 1) {
      return false;
    }
    for (var i = 2; i <= Math.sqrt(candidate); i++) {
      if (Thread.currentThread().isInterrupted()) {
        return false;
      }

      if (candidate % i == 0) {
        return false;
      }
    }
    return true;
  }

  public static OptionalLong findPrime() {
    var generator = ThreadLocalRandom.current();
    while (!Thread.interrupted()) {
      var candidate = generator.nextLong();
      if (isPrime(candidate)) {
        return OptionalLong.of(candidate);
      }
    }

    return OptionalLong.empty();
  }

  public static void main(String[] args) {
    var thread = Thread.ofPlatform().start(() -> {
      var nb = findPrime();
      if (nb.isEmpty()) {
        System.out.println("Nothing could be found.");
      } else {
        System.out.println("Found a random prime : " + nb.orElseThrow());
      }
    });

    try {
      Thread.sleep(3_000);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    thread.interrupt();
  }
}

