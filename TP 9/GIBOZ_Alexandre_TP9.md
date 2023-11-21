# TP9 - ExecutorService
## GIBOZ Alexandre, INFO2 2023-2025
***

[Énoncé](https://igm.univ-mlv.fr/coursconcurrenceinfo2/tds/td09.html)
***

Dans ce TP, nous allons reprendre les classes du TP précédent et les réaliser en utilisant différents classes implémentant l'interface ExecutorService.

## Exercice 1

1. **Écrire la classe CheapestPooled en utilisant un ExecutorService. Pour simplifier le code, on suppose que l'API des requêtes ne lève aucune exception.**

```java
public class CheapestPooled {
  private final String item;
  private final int interruptedTime;
  private final int nbThreads;

  public CheapestPooled(String item, int interruptedTime, int nbThreads) {
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
    var answers = new ArrayList<Answer>();

    for (var site : sites) {
      callables.add(() -> new Request(site, item).request(interruptedTime));
    }

    var futures = executorService.invokeAll(callables);
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
    var cheapest = new CheapestPooled("tortank", 4_000, 5);
    var val = cheapest.retrieve();

    val.ifPresentOrElse(
            answer -> System.out.println("Found: " + answer),
            () -> System.out.println("Not found")
    );
  }
}
```

