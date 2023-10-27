import java.util.concurrent.ThreadLocalRandom;

public class E8 {
  public static boolean isPrime(long candidate) throws InterruptedException {
    if (candidate <= 1) {
      return false;
    }

    for (var i = 2; i <= Math.sqrt(candidate); i++) {
      if (Thread.interrupted()) {
        throw new InterruptedException();
      }

      if (candidate % i == 0) {
        return false;
      }
    }
    return true;
  }

  public static long findPrime() throws InterruptedException {
    var generator = ThreadLocalRandom.current();
    for(;;) {
      var candidate = generator.nextLong();
      if (isPrime(candidate)) {
        return candidate;
      }
    }
  }

  public static void main(String[] args) {
    var thread = Thread.ofPlatform().start(() -> {
      long nb;
      try {
        nb = findPrime();
        System.out.println("Found a random prime : " + nb);
      } catch (InterruptedException e) {
        System.out.println("Nothing could be found.");
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

