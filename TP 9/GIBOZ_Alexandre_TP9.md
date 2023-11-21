# TP9 - ExecutorService
## GIBOZ Alexandre, INFO2 2023-2025
***

[Énoncé](https://igm.univ-mlv.fr/coursconcurrenceinfo2/tds/td09.html)
***

Dans ce TP, nous allons reprendre les classes du TP précédent et les réaliser en utilisant différents classes implémentant l'interface ExecutorService.

## Exercice 1

1. **Écrire la classe CheapestPooled en utilisant un ExecutorService. Pour simplifier le code, on suppose que l'API des requêtes ne lève aucune exception.**

Classe `CheapestPooled`:
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

2. **On veut écrire une classe CheapestPooledWithGlobalTimeout qui, en plus des paramètres de CheapestPooled, prend un paramètre timeoutMilliGlobal. 
     Donc, à la construction, cette classe prend :
        - item le nom de l'objet demandé,
        - poolSize la taille du pool de worker-threads,
        - timeoutMilliPerRequest le timeout en milli-secondes pour chaque Request,
        - timeoutMilliGlobal le timeout en milli-secondes pour l'appel à retrieve.
   Les Request sont toujours exécutées avec le timeout timeoutMilliPerRequest, mais votre code devra en plus garantir que l'exécution de retrieve ne prend pas plus de timeoutMilliGlobal millisecondes. 
   La réponse renvoyée par retrieve renverra le prix le moins élevé parmi les réponses obtenues.
   Écrire la classe CheapestPooledWithGlobalTimeout en utilisant un ExecutorService**

Classe `CheapestPooledWithGlobalTimeout`:
```java
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
```

3. **En utilisant un ExecutorService, écrire une classe FastestPooled dont la méthode retrieve() lance un nombre fixé de threads qui tentent d'obtenir le prix d'un item et renvoie le premier prix obtenu, s'il existe.**

Classe `FastestPooled`:
```java
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
```