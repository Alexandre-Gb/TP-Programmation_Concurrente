# TP7 - Interuptions et Exceptions
## GIBOZ Alexandre, INFO2 2023-2025
***

[Énoncé](https://igm.univ-mlv.fr/coursconcurrenceinfo2/tds/td07.html)
***

## Exercice 1 - Haut les mains!

1. **Pourquoi n'est il pas possible d’arrêter un thread de façon non coopérative ?**

Un thread ne s'arrête que lorsqu'il est dans une action bloquante et que, dans le cas ou une demande
d'interruption lui a été formulée, il prend en considération le cas.

Le thread, si mal conçu par le programmeur, peut ne jamais s'arrêter.

2. **Rappeler ce qu'est une opération bloquante.**

On définit par action bloquante toute opération qui met le thread en attente d'une ressource externe.
Ce processus est généralement hors de notre contrôle et peut s'avérer chronophage 
(appels systèmes, attendre la réponse d'une BDD/API suite à une requête ...)

3. **À quoi sert la méthode d'instance interrupt() de la classe Thread?**

La méthode interrupt() permet de demander à un thread de s'arrêter. Une valeur booléenne indiquant si une demande a été
formulée sera passée à true sur le thread ciblé. Tant que cette valeur n'est pas considérée, le thread continue de subsister.

4. **Expliquer comment interrompre un thread en train d'effectuer une opération bloquante et le faire sur l'exemple suivant : 
    le thread main attend 5 secondes avant d'interrompre le thread qui dort et ce dernier affiche son nom.**
```java
public static void main(String[] args) {
  Thread.ofPlatform().start(() -> {
    for (var i = 1;; i++) {
      try {
        Thread.sleep(1_000);
        System.out.println("Thread slept " + i + " seconds.");
      } catch (InterruptedException e) {
        // TODO
      }
    }
  });
}
```

On va stocker le thread dans une variable afin de pouvoir lui envoyer une demande d'interruption quand cela s'avère nécessaire.
Dans le cas ou le thread effectue une action bloquante et que la propriété `volatile boolean interrupted;` du thread est set à true, on
capture l'exception `InterruptedException` et on sort de la boucle infinie via une instruction `break`.

Le main va attendre 5 secondes avant d'envoyer une demande d'interruption au thread.

On obtient le code suivant:
```java
public static void main(String[] args) {
  var thread = Thread.ofPlatform().start(() -> {
    for (var i = 1;; i++) {
      try {
        Thread.sleep(1_000);
        System.out.println("Thread slept " + i + " seconds.");
      } catch (InterruptedException e) {
        System.out.println("Thread " + Thread.currentThread().getName() + " interrupted.");
        break;
      }
    }
  });
  
  
  try {
    Thread.sleep(5_000);
  } catch (InterruptedException e) {
    throw new AssertionError(e);
  }
  
  thread.interrupt();
}
```

5. **Expliquer, sur l'exemple suivant, comment utiliser la méthode Thread.interrupted pour arrêter le calcul de findPrime() qui n'est pas une opération bloquante. 
    Modifier le code de findPrime (mais ni sa signature, ni isPrime) pour pouvoir l'interrompre. 
    Dans ce cas, elle renvoie un OptionalLong vide.
    Puis faire en sorte que le main attende 3 secondes avant d'interrompre le thread qui cherche un nombre premier, en affichant "STOP".**
```java
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
  for (;;) {
    var candidate = generator.nextLong();
    if (isPrime(candidate)) {
      return OptionalLong.of(candidate);
    }
  }
}

public static void main(String[] args) throws InterruptedException {
  Thread.ofPlatform().start(() -> {
    System.out.println("Found a random prime : " + findPrime().orElseThrow());
  });
}
```

On obtient le code suivant:
```java
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
```

6. **Expliquer la (trop) subtile différence entre les méthodes Thread.interrupted et thread.isInterrupted de la classe Thread.**

`Thread.interrupted` est une méthode statique qui va renvoyer une valeur booléenne en fonction de si le thread courrant à sa propriété `interrupted` définie sur `true`.
Mais, la fonction va également repasser cette valeur à `false`.

`isInterrupted()`, cependant, est une méthode d'instance qui va uniquement regarder si la propriété est vraie sur le thread, sans en affecter la valeur (comme une méthode peek).

7. **On souhaite maintenant faire en sorte que findPrime s'arrête dès que possible si le thread qui l’utilise est interrompu. 
    Pour cela, modifier le code de findPrime et/ou isPrime sans modifier leur signature.**

On obtient le code suivant:
```java
public class E7 {
  public static boolean isPrime(long candidate) {
    if (candidate <= 1) {
      return false;
    }
    for (var i = 2; i <= Math.sqrt(candidate); i++) {
      if (Thread.currentThread().isInterrupted()) {
        return false;
      }

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
      var nb = findPrime();
      if (nb.isEmpty()) {
        System.out.println("Nothing could be found.");
      } else {
        System.out.println("Found a random prime : " + nb);
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
```

La subtilité ici est que l'on regarde la propriété `interrupted` dans les deux fonctions.
Si on utilise `Thread.interrupted()` dans la méthode `isPrime()`, on va effectivement sortir de la méthode car
la propriété était true, mais, elle sera remise à false par la méthode, ce qui va empêcher la méthode `findPrime` de s'arrêter car la 
condition de sortie (interrupted == true) ne sera plus satisfaite.

Par ailleurs, il faut adapter le code pour correspondre au OptionalLong.empty, on ne veut pas throw d'erreur.

8. **Et si vous pouvez modifier le code des méthodes ET leur signature, que faites-vous ?**

On obtient le code suivant:
```java
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
```

On ne teste l'interruption que dans la méthode `isPrime`, car cette dernière sera propagée.
Il est préférable d'utiliser `Thread.interrupted()` au lieu de `Thread.currentThread().isInterrupted()` car la première va remettre la propriété à false, et évitera aux autres
méthodes de retester la propriété.

<br>

## Exercice 2 - Count And Interrupt (à faire à la maison)

On souhaite avoir 4 threads qui affichent chacun leur numéro et un compteur indéfiniment (chaque thread a son propre compteur). 
Pour éviter de faire chauffer la machine, l'affichage se fera une fois par seconde (en utilisant Thread.sleep()).

De plus, le thread main va lire des entiers sur l'entrée standard et si l'utilisateur entre une valeur correspondant au numéro d'un thread, ce dernier sera arrêté.

Le code pour lire sur l'entrée standard est le suivant :
```java
  System.out.println("enter a thread id:");
  try (var input = new InputStreamReader(System.in);  var reader = new BufferedReader(input)) {
    String line;
    while ((line = reader.readLine()) != null) {
      var threadId = Integer.parseInt(line);
      ...
    }
  }
```

1. **Comment faire pour que le programme se termine si l'on fait un Ctrl-D dans le terminal ?**

On procède ainsi:
```java
public class CountAndInterrupt {
  public static void main(String[] args) {
    var nbThreads = 4;
    var threads = new ArrayList<Thread>(nbThreads);

    IntStream.range(0, nbThreads).forEach(i -> {
      threads.add(Thread.ofPlatform().daemon().start(()-> {
        var count = 0;
        for (;;) {
          System.out.println("Current counting value : " + count);
          System.out.println("Thread Id : " + Thread.currentThread().threadId());
          try {
            Thread.sleep(1_000);
          } catch (InterruptedException e) {
            return;
          }
          count++;
        }
      }));
    });

    System.out.println("enter a thread id:");
    try (var scanner = new Scanner(System.in)) {
      while (scanner.hasNextInt()) {
        var threadId = scanner.nextInt();
        for (var thread : threads) {
          if (thread.threadId() == threadId) {
            thread.interrupt();
          }
        }
      }
    }
  }
}
```

<br>

## Exercice 3 - Le Juste Prix (exam 2017-2018)

1. **Écrire le code de la classe ThePriceIsRight.**

Classe `ThePriceIsRight`:
```java
public class ThePriceIsRight {
  private final int price;
  private final int nbThreads;
  private boolean hasBeenInterrupted;
  private final LinkedHashMap<Thread, Integer> propositions = new LinkedHashMap<>();
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition notAllPropositions = lock.newCondition();

  public ThePriceIsRight(int price, int nbThreads) {
    lock.lock();
    try {
      if (price < 0 || nbThreads < 1) {
        throw new IllegalArgumentException();
      }

      this.price = price;
      this.nbThreads = nbThreads;
    } finally {
      lock.unlock();
    }
  }

  public boolean propose(int proposition) {
    lock.lock();
    try {
      if (proposition < 0) {
        throw new IllegalArgumentException();
      }

      if (hasBeenInterrupted || propositions.containsKey(Thread.currentThread()) || propositions.size() == nbThreads) {
        return false;
      }

      propositions.put(Thread.currentThread(), proposition);
      notAllPropositions.signalAll();

      while (!hasBeenInterrupted && propositions.size() != nbThreads) {
        notAllPropositions.await();
      }

      var closestPrice = Integer.MAX_VALUE;
      for (int p : propositions.values()) {
        if (distance(p) < distance(closestPrice)) {
          closestPrice = p;
        }
      }

      return (closestPrice == proposition &&
              propositions.values().stream()
                      .filter(p -> p == proposition)
                      .count() == 1);
    } catch (InterruptedException e) {
      if (nbThreads != 1) {
        hasBeenInterrupted = true;
        propositions.remove(Thread.currentThread());
        notAllPropositions.signalAll();
      }

      return false;
    } finally {
      lock.unlock();
    }
  }

  private int distance(int proposition) {
    return Math.abs(proposition - price);
  }
}-
```