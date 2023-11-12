package fr.uge.concurrence;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.IntStream;

public class Codex {
  private static final int MAX_SIZE = 14;
  public static void main(String[] args) {
    var receiveQueue = new ArrayBlockingQueue<String>(MAX_SIZE);
    var decodedQueue = new ArrayBlockingQueue<String>(MAX_SIZE);
    var nbCollectThreads = 3;
    var nbDecodeThreads = 2;
    var nbArchiveThread = 1;

    IntStream.range(0, nbCollectThreads).forEach(i -> Thread.ofPlatform().name("Thread-" + i).start(() -> {
      for(;;) {
        try {
          var value = CodeAPI.receive();
          System.out.println(Thread.currentThread().getName() + " collects value: " + value);
          receiveQueue.put(value);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }));

    IntStream.range(0, nbDecodeThreads).forEach(i -> Thread.ofPlatform().name("Thread-" + i).start(() -> {
      for(;;) {
        try {
          var encodedValue = receiveQueue.take();
          System.out.println(Thread.currentThread().getName() + " encodes value: " + encodedValue);
          decodedQueue.put(CodeAPI.decode(encodedValue));
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        } catch (IllegalArgumentException e) {
          System.out.println("Impossible de décoder la valeur: " + e);
        }
      }
    }));

    IntStream.range(0, nbArchiveThread).forEach(i -> Thread.ofPlatform().name("Thread-" + i).start(() -> {
      for(;;) {
        try {
          var toArchive = decodedQueue.take();
          System.out.println(Thread.currentThread().getName() + " archives value: " + toArchive);
          CodeAPI.archive(toArchive);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }
    }));
  }
}
