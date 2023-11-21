package fr.uge.concurrence;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CheapestPooledWithGlobalTimeout {
  private final String item;
  private final int poolSize;
  private final int timeOutMilliPerRequest;
  private final int timeoutMilliGlobal;

  public CheapestPooledWithGlobalTimeout(String item, int poolSize, int timeOutMilliPerRequest, int timeoutMilliGlobal) {
    Objects.requireNonNull(item);
    if (timeOutMilliPerRequest < 0 || timeoutMilliGlobal < 0) { throw new IllegalArgumentException(); }
    if (poolSize < 1) { throw new IllegalArgumentException(); }

    this.item = item;
    this.poolSize = poolSize;
    this.timeOutMilliPerRequest = timeOutMilliPerRequest;
    this.timeoutMilliGlobal = timeoutMilliGlobal;
  }

  public Optional<Answer> retrieve() throws InterruptedException {
    var sites = Request.ALL_SITES;
    var executorService = Executors.newFixedThreadPool(poolSize);
    var callables = new ArrayList<Callable<Optional<Answer>>>();
    var answers = new ArrayList<Answer>();

    for (var site : sites) {
      callables.add(() -> new Request(site, item).request(timeOutMilliPerRequest));
    }

    var futures = executorService.invokeAll(callables, timeoutMilliGlobal, TimeUnit.MILLISECONDS);
    executorService.shutdown();

    try {
      for (var future : futures) {
        var answer = future.get();
        answer.ifPresent(answers::add);
      }
    } catch (ExecutionException e) {
      throw new AssertionError(e);
    }

    return answers.stream().min(Answer::compareTo);
  }

  public static void main(String[] args) throws InterruptedException {
    var cheapest = new CheapestPooledWithGlobalTimeout("tortank", 5, 4_000, 25_000);
    var val = cheapest.retrieve();

    val.ifPresentOrElse(
            answer -> System.out.println("Found: " + answer),
            () -> System.out.println("Not found")
    );
  }
}
