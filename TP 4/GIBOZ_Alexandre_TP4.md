# TP4 - Signaux
## GIBOZ Alexandre, INFO2 2023-2025
***

[Énoncé](https://igm.univ-mlv.fr/coursconcurrenceinfo2/tds/td04.html)
***

## Exercice 1 - Rendez-vous (come back)

Dans cet exercice, on reprend le dernier exercice de la séance précédente. 
On rappelle qu'on souhaite réaliser une classe RendezVous thread-safe qui offre un méthode set permettant de proposer une valeur et une méthode get qui ''bloque'' jusqu'à ce qu'une valeur ait été proposée et la renvoie lorsque c'est le cas.

1. **Écrire la classe RendezVous sans que votre code ne fasse de l'attente active.**

Classe `RendezVous` :
```java
import java.util.Objects;

public class RendezVous<V> {
  private V value;
  private final Object lock = new Object();

  public void set(V value) {
    synchronized (lock) {
      Objects.requireNonNull(value);
      this.value = value;
      lock.notifyAll();
    }
  }

  public V get() throws InterruptedException {
    synchronized (lock) {
      while (value == null) {
        lock.wait();
      }
      return value;
    }
  }
}
```

2. **Tester votre classe avec le main ci-dessous. 
    Vous devriez observer avec la commande top que votre programme ne consomme quasiment aucun temps processeur.**

Le code fonctionne et renvoi le message au bout de 20 secondes.

3. **Comparer le résultat de la commande top avec le même main mais en utilisant la classe RendezVous de la semaine dernière qui fait de l'attente active.**

La consommation était de 50% du CPU avant la modification (TP précédent), et est d'environ 0.5% (sur un IDE) actuellement.

<br>

## Exercice 2 - File d'attente thread-safe

Dans cet exercice, on veut créer une file d'attente thread-safe.

Dans un premier temps, on cherche a réaliser une file d'attente thread-safe non-bornée que nous appellerons UnboundedSafeQueue<V>. 
Cette classe offrira deux méthodes : 
    - add(V value) qui ajoute un élément à la fin de la file,
    - V take() qui retire le premier élément de la file et le renvoie. Si la file est vide, la méthode doit attendre jusqu'à ce qu'un élément soit ajouté..

1. **Écrire la classe UnboundedSafeQueue<V>.**

Classe `UnboundedSafeQueue` :
```java
public class UnboundedSafeQueue<V> {
  private final ArrayDeque<V> queue = new ArrayDeque<>();
  private final Object lock = new Object();

  public void add(V value) {
    synchronized (lock) {
      queue.addLast(value);
      lock.notifyAll();
    }
  }

  public V take() throws InterruptedException {
    synchronized (lock) {
      while (queue.isEmpty()) {
        lock.wait();
      }
      return queue.removeFirst();
    }
  }
}
```

Remarque: Il est possible d'utiliser l'ArrayDequeue comme un lock
```java
public class UnboundedSafeQueue<V> {
  private final ArrayDeque<V> queue = new ArrayDeque<>();

  public void add(V value) {
    synchronized (queue) {
      queue.addLast(value);
      queue.notifyAll();
    }
  }

  public V take() throws InterruptedException {
    synchronized (queue) {
      while (queue.isEmpty()) {
        queue.wait();
      }
      return queue.removeFirst();
    }
  }
}
```

2. **Écrire un main qui démarre 3 threads qui, toutes les 2 secondes, vont ajouter leur nom (utiliser getName()) dans une UnboundedSafeQueue. 
    Le main effectuera une boucle qui récupère en permanence le premier élément de la UnboundedSafeQueue et l'affiche.**

Méthode `main` :
```java
public static void main(String[] args) throws InterruptedException {
  UnboundedSafeQueue<String> queue = new UnboundedSafeQueue<>();
  var nbThreads = 3;
  Thread[] threads = new Thread[nbThreads];

  IntStream.range(0, nbThreads).forEach(i -> {
    Runnable runnable = () -> {
      try {
        Thread.sleep(2_000);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      queue.add(Thread.currentThread().getName());
    };

    threads[i] = Thread.ofPlatform().start(runnable);
  });

  for (var thread : threads) {
    thread.join();
  }

  for (int i = 0; i < nbThreads; i++) {
    System.out.println(queue.take());
  }
}
```

**Nous voulons maintenant réaliser une file d'attente thread-safe de taille bornée BoundedSafeQueue<V>. 
La capacité maximale est fixée à la création de la file et la taille de la file ne devra jamais dépasser cette capacité. 
La méthode add n'est donc plus adaptée car elle ne prend pas en compte la capacité maximale. 
On la remplace par une méthode put(V value) qui ajoute value s'il y a la place et sinon attend jusqu'à ce qu'une place se libère.**

Class `BoundedSafeQueue` :
```java
public class BoundedSafeQueue<V> {
  private final ArrayDeque<V> queue = new ArrayDeque<>();
  private final int capacity;

  public BoundedSafeQueue(int capacity) {
    if (capacity < 1) throw new IllegalArgumentException("capacity must be > 0");
    this.capacity = capacity;
  }


  public void put(V value) throws InterruptedException {
    synchronized (queue) {
      while (queue.size() >= capacity) {
        queue.wait(); // Full
      }
      queue.addLast(value);
      queue.notifyAll();
    }
  }

  public V take() throws InterruptedException {
    synchronized (queue) {
      while (queue.isEmpty()) {
        queue.wait();
      }
      queue.notify();
      return queue.removeFirst();
    }
  }
}
```

<br>

## Exercice 3 - Maximum Thread-Safe bloquant

Dans cet exercice, on va faire évoluer le programme de maximum thread-safe du TP précédent. 
Le thread main démarre 4 threads, chacun d'eux fait une boucle infinie qui, toutes les secondes :
    - tire un nombre aléatoire entre 0 et MAX_VALUE ;
    - affiche le nom du thread et ce nombre.

Une fois que le thread main a démarré les quatres threads, on veut qu'il affiche le nombre maximum proposé par les différents threads dès qu'un thread à proposé une valeur supérieure à 8_000. 
Une fois que c'est le cas, le thread main affichera le nombre maximum proposé par une thread toutes les secondes.

MAX_VALUE = 10_000 est une constante de la classe BlockingMaximum que vous allez écrire par la suite.

1. **Le thread main et les 4 threads qu'il a démarré doivent communiquer au moyen d'un classe thread-safe BlockingMaximum. 
    Donnez le contrat de cette classe, c'est à dire les méthodes publiques de la classe ainsi que le comportement attendu.**

La classe `BlockingMaximum` possèdera deux méthodes:
    - Optional<Integer> getMax() retournant un `Optional` contenant le maximum des valeurs proposées par les threads.
    Si aucun thread n'a proposé de valeur ou si la valeur proposée est inférieure à 8_000, la méthode retourne un `Optional` vide.
    - void suggest(int value) qui modifie la propriété "currentMax" de l'objet dans le cas ou la valeur proposé est supérieure à 8_000

2. **Écrire le code du main dans une classe Application ainsi que les méthodes de BlockingMaximum.**

On obtient le code suivant:
```java
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class BlockingMaximum {
  public static final int MAX_VALUE = 10_000;
  private Integer currentMax;
  private final Object lock = new Object();

  public Optional<Integer> getMax() throws InterruptedException {
    synchronized (lock) {
      lock.wait();
      return Optional.ofNullable(currentMax);
    }
  }

  public void suggest(int value) {
    synchronized (lock) {
      if (value > 8_000 && (currentMax == null || value > currentMax)) {
        currentMax = value;
        lock.notifyAll();
      }
    }
  }

  public static class Application {
    public static void main(String[] args) {
      int nbThreads = 4;
      var blockingMaximum = new BlockingMaximum();

      IntStream.range(0, nbThreads).forEach(i -> {
        Thread.ofPlatform().start(() -> {
          for(;;) {
            try {
              Thread.sleep(1000);
            } catch (InterruptedException e) {
              throw new AssertionError(e);
            }
            int randint = ThreadLocalRandom.current().nextInt(BlockingMaximum.MAX_VALUE);

            System.out.println(Thread.currentThread().getName() + " a tiré " + randint);
            blockingMaximum.suggest(randint);
          }
        });
      });

      for(;;) {
        Optional<Integer> max; // = Optional.empty() by default
        try {
          max = blockingMaximum.getMax();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
        max.ifPresent(value -> System.out.println("Premier max après 8 000 : " + value));
      }
    }
  }
}
```

## Exercice 4 - Vote

Dans cet exercice, on veut réaliser une classe Vote. Cette classe thread-safe permet d'enregistrer n votes pour des String. 
Quand les n votes ont été enregistrés, on renvoie à chaque votant la String qui a reçu le plus de votes (en cas d'égalité, on renvoie la plus petite dans l'ordre alphabétique ayant reçu le plus de votes). 
Le nombre n de votes attendus est pris en paramètre par le constructeur.

La classe Vote offre une seule méthode vote qui sert à proposer son vote et qui bloque jusqu'à ce que les n votes soient arrivés. 
Ensuite elle renvoie le gagnant. 
Si vote est appelée après que les n votes aient été reçus, la méthode renvoie simplement le gagnant.

Le main ci-dessous donne un exemple d'utilisation de la classe:
```java
public static void main(String[] args) throws InterruptedException {
  var vote = new Vote(4);
  Thread.ofPlatform().start(() -> {
    try {
      Thread.sleep(2_000);
      System.out.println("The winner is " + vote.vote("un"));
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  });
  Thread.ofPlatform().start(() -> {
    try {
      Thread.sleep(1_500);
      System.out.println("The winner is " + vote.vote("zero"));
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  });
  Thread.ofPlatform().start(() -> {
    try {
      Thread.sleep(1_000);
      System.out.println("The winner is " + vote.vote("un"));
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }
  });
  System.out.println("The winner is " + vote.vote("zero"));
}
```

1. **Écrire la classe Vote et vérifier qu'elle passe les tests unitaires de VoteTest.java.**

On obtient la classe suivante:
```java
import java.util.HashMap;
import java.util.Map;

public class Vote {
  private final int maxVotes;
  private int currentVotes = 0;
  private final Map<String, Integer> votes = new HashMap<>();
  private final Object lock = new Object();

  public Vote(int maxVotes) {
    if (maxVotes < 1) throw new IllegalArgumentException("maxVotes must be > 0");
    this.maxVotes = maxVotes;
  }

  public String vote(String candidate) throws InterruptedException {
    synchronized (lock) {
      votes.merge(candidate, 1, Integer::sum);
      currentVotes++;

      if (currentVotes >= maxVotes) {
        lock.notifyAll();
      }

      return computeWinner();
    }
  }

  private String computeWinner() {
    var score = -1;
    String winner = null;
    for (var e : votes.entrySet()) {
      var key = e.getKey();
      var value = e.getValue();
      if (value > score || (value == score && key.compareTo(winner) < 0)) {
        winner = key;
        score = value;
      }
    }
    return winner;
  }


//  private String computeWinner() throws InterruptedException {
//    synchronized (lock) {
//      while (currentVotes < maxVotes) {
//        lock.wait();
//      }
//
//      return votes.entrySet().stream()
//        .max(Map.Entry.comparingByValue())
//        .map(Map.Entry::getKey)
//        .orElse(null);
//    }
//  }

  public static void main(String[] args) throws InterruptedException {
    // ...
  }
}
```