package fr.uge.concurrence;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

public class FastestPooled {
  private final String item;
  private final int interruptedTime;
  private final int nbThreads;

  public FastestPooled(String item, int interruptedTime, int nbThreads) {
    Objects.requireNonNull(item);
    if (interruptedTime < 0) { throw new IllegalArgumentException(); }
    if (nbThreads < 1) { throw new IllegalArgumentException(); }

    this.item = item;
    this.interruptedTime = interruptedTime;
    this.nbThreads = nbThreads;
  }

  public Optional<Answer> retrieve() throws InterruptedException {
    var sites = Request.ALL_SITES;
    var executorService = Executors.newFixedThreadPool(nbThreads);
    var callables = new ArrayList<Callable<Optional<Answer>>>();

    for (var site : sites) {
      callables.add(() -> new Request(site, item).request(interruptedTime));
    }

    Optional<Answer> answer;
    try {
      answer = executorService.invokeAny(callables);
    } catch (ExecutionException e) {
      throw new AssertionError(e);
    }

    executorService.shutdown();
    return answer;
  }

  public static void main(String[] args) throws InterruptedException {
    var cheapest = new FastestPooled("tortank", 4_000, 5);
    var val = cheapest.retrieve();

    val.ifPresentOrElse(
            answer -> System.out.println("Found: " + answer),
            () -> System.out.println("Not found")
    );
  }
}
