package fr.uge.concurrence;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.IntStream;

public class Fastest {
  private final String item;
  private final int interruptedTime;

  public Fastest(String item, int interruptedTime) {
    Objects.requireNonNull(item);
    if (interruptedTime < 0) { throw new IllegalArgumentException(); }

    this.item = item;
    this.interruptedTime = interruptedTime;
  }

  public Optional<Answer> retrieve() {
    var queue = new SynchronousQueue<Optional<Answer>>();
    var sites = Request.ALL_SITES;
    var threadList = new ArrayList<Thread>();

    IntStream.range(0, sites.size()).forEach(i -> Thread.ofPlatform().start(() -> {
      threadList.add(Thread.currentThread());
      var request = new Request(sites.get(i), item);
      try {
        queue.put(request.request(interruptedTime));
      } catch (InterruptedException e) {
        return;
      }
    }));

    try {
      for(int i = 0; i < threadList.size(); i++) {
        var fastestValue = queue.take();
        if(fastestValue.isPresent()) {
          threadList.forEach(Thread::interrupt);
          return fastestValue;
        }
      }
    } catch (InterruptedException e ) {
      return Optional.empty();
    }

    threadList.forEach(Thread::interrupt);
    return Optional.empty();
  }

  public static void main(String[] args) {
//    var fastest = new Fastest("Bonne note en Java Avancé", 4_000);
    var fastest = new Fastest("tortank", 4_000);
    var val = fastest.retrieve();

    val.ifPresentOrElse(
        answer -> System.out.println("Found: " + answer),
        () -> System.out.println("Not found")
    );
  }
}
