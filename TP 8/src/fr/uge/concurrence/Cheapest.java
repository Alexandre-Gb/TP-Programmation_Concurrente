package fr.uge.concurrence;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.stream.IntStream;

public class Cheapest {
  private final String item;
  private final int interruptedTime;

  public Cheapest(String item, int interruptedTime) {
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

    var list = new ArrayList<Answer>();
    for(int i = 0; i < sites.size(); i++) {
      try {
        queue.take().ifPresent(list::add);
      } catch (InterruptedException e) {
        continue;
      }
    }

    if (list.isEmpty()) {
      threadList.forEach(Thread::interrupt);
      return Optional.empty();
    }

    threadList.forEach(Thread::interrupt);
    return list.stream().min(Answer::compareTo);
  }

  public static void main(String[] args) {
    var cheapest = new Cheapest("tortank", 4_000);
    var val = cheapest.retrieve();

    val.ifPresentOrElse(
        answer -> System.out.println("Found: " + answer),
        () -> System.out.println("Not found")
    );
  }
}
