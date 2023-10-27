import java.util.OptionalLong;
import java.util.concurrent.ThreadLocalRandom;

public class E5 {
  public static boolean isPrime(long candidate) {
    if (candidate <= 1) {
      return false;
    }
    for (var i = 2; i <= Math.sqrt(candidate); i++) {
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

  public static void main(String[] args) throws InterruptedException {
    var thread = Thread.ofPlatform().start(() -> {
      System.out.println("Found a random prime : " + findPrime().orElseThrow());
    });

    try {
      Thread.sleep(3_000);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

    System.out.println("STOP");
    thread.interrupt();
  }
}
