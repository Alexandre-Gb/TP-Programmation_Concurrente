# TP6 - ReentrantLock
## GIBOZ Alexandre, INFO2 2023-2025
***

[Énoncé](https://igm.univ-mlv.fr/coursconcurrenceinfo2/tds/td06.html)
***

## Exercice 1 - File d'attente thread-safe (come-back)

1. **Ré-écrire (refactor) cette classe pour qu'elle utilise l'API des ReentrantLock.**

Classe `UnboundedSafeQueue`:
```java
public class UnboundedSafeQueue<V> {
  private final ArrayDeque<V> fifo = new ArrayDeque<>();
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public void add(V value) {
    Objects.requireNonNull(value);
    lock.lock();
    try {
      fifo.add(value);
      condition.signal();
    } finally {
      lock.unlock();
    }
  }

  public V take() throws InterruptedException {
    lock.lock();
    try {
      while (fifo.isEmpty()) {
        condition.await();
      }
      return fifo.remove();
    } finally {
      lock.unlock();
    }
  }
}
```

3. **Pourquoi utilise-t-on notifyAll et non pas notify ici ?**

Il est possible de réveiller un mauvais thread. Si on ajoute un élément, il est possible que le thread que l'on réveille
avec notify soit un thread qui souhaite ajouter également, alors qu'on veut réveiller un thread qui attend pour récupérer une valeur.

4 (et 5). **Récrire (refactor) cette classe pour qu'elle utilise l'API des ReentrantLock**

Classe `BoundedQueue`:
```java
public class BoundedSafeQueue<V> {
  private final ArrayDeque<V> fifo = new ArrayDeque<>();
  private final int capacity;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition conditionPut = lock.newCondition();
  private final Condition conditionTake = lock.newCondition();


  public BoundedSafeQueue(int capacity) {
    if (capacity <= 0) {
      throw new IllegalArgumentException();
    }
    this.capacity = capacity;
  }

  public void put(V value) throws InterruptedException {
    Objects.requireNonNull(value);
    lock.lock();
    try {
      while (fifo.size() == capacity) {
        conditionPut.await();
      }
      fifo.add(value);
      conditionTake.signal();
    } finally {
      lock.unlock();
    }
  }

  public V take() throws InterruptedException {
    lock.lock();
    try {
      while (fifo.isEmpty()) {
        conditionTake.await();
      }
      conditionPut.signal();
      return fifo.remove();
    } finally {
      lock.unlock();
    }
  }
}
```

<br>

## Exercice 2 - Recherche de nombres premiers

Dans cet exercice, on veut écrire un programme qui lance 5 threads qui cherchent en boucle des grands nombres premiers. 
Le thread main attend que 10 nombres premiers aient été trouvés, affiche leur somme et s'arrête. 

On souhaite réaliser cette tâche avec les contraintes suivantes : 
-  Vous devez introduire une classe thread-safe MyThreadSafeClass respectant toutes les bonnes pratiques vues en cours et qui va permettre aux threads de communiquer avec le thread main.  
-  Vous ne devez utiliser aucun mécanisme de synchronisation (autre que l'utilisation de MyThreadSafeClass) dans le code de la méthode main. 

1. **Décrire le contrat (c'est à dire quelles sont les méthodes qu'elle doit fournir et ce qu'elles font précisément) d'une classe MyThreadSafeClass permettant de réaliser cette tâche.**

Une méthode propose qui prend un int en paramètre qui fait l'addition de la valeur (première) passée en paramètre avec le champ "sum".
Une méthode sum qui attend que 10 valeurs valides aient été proposées, puis renvoie l'addition des valeurs.

2. **Écrire la classe MyThreadSafeClass, en utlisant l'API des ReentrantLock.**

Classe `MyThreadSafeClass`:
```java
public class MyThreadSafeClass {
  private int counter;
  private int sum;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public void propose(int value) {
    lock.lock();
    try {
      if (counter == 10) {
        return;
      }
      sum += value;
      counter++;

      if (counter == 10) {
        condition.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }

  public int sum() throws InterruptedException {
    lock.lock();
    try {
      while (counter < 10) {
        condition.await();
      }
      return sum;
    } finally {
      lock.unlock();
    }
  }
}
```

3. **Écrire la méthode main d'une classe SumFirstTenPrimes qui réalise la tâche demandée en utilisant MyThreadSafeClass.**

Main:
```java
public static void main(String[] args) {
  int nbThreads = 5;
  var myThreadSafeClass = new MyThreadSafeClass();
  
  IntStream.range(0, nbThreads).forEach(e -> {
    Thread.ofPlatform().daemon().start(() -> {
      var random = new Random();
      for (;;) {
        long nb = 1_000_000_000L + (random.nextLong() % 1_000_000_000L);
        if (isPrime(nb)) {
          myThreadSafeClass.propose((int) nb);
        }
      }
    });
  });
}
```

Les threads sont des daemons afin qu'on ne continue pas de fournir des nombres qui seront pris en considération
au dela de 10.

<br>

## Exercice 3 - Sémaphores

Dans cet exercice, on cherche à écrire une classe thread-safe Semaphore. Un objet de cette classe (un sémaphore) contient un certain nombre de permis d’exécution (le nombre initial est fixé à la construction) et permet d'en acquérir et d'en rendre. 

La classe ne stocke pas réellement des permis (il n'y a pas d'objet "permis") mais simplement le nombre de permis disponibles dans le sémaphore.

Le nombre initial de permis est passé en argument du constructeur de la classe. 
La classe fournit 3 méthodes :
- void release() remet un permis dans le sémaphore (il est possible dépasser le nombre initial de permis).
- boolean tryAcquire() qui retire un permis du sémaphore s'il y en a au moins un de disponible. 
Dans ce cas, la méthode renvoie true. 
S'il n'y a pas de permis disponible, elle renvoie false. 
- void acquire() qui prend un permis s'il y en a au moins un de disponible ou qui bloque jusqu'à ce qu'il y en ait un et le prend.

1. **En utilisant l'API des ReentrantLock, écrire une classe Semaphore thread-safe et qui respecte le contrat ci-dessus.**

Classe `Semaphore`:
```java
public class Semaphore {
  private int nbPermits;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public Semaphore(int nbPermits) {
    lock.lock();
    try {
      this.nbPermits = nbPermits;
    } finally {
      lock.unlock();
    }
  }

  public boolean tryAcquire() {
    lock.lock();
    try {
      if (nbPermits > 1) {
        nbPermits--;
        return true;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  private void acquire() throws InterruptedException {
    lock.lock();
    try {
      while (nbPermits == 0) {
        condition.await();
      }
      nbPermits--;
    } finally {
      lock.unlock();
    }
  }

  public void release() {
    lock.lock();
    try {
      nbPermits++;
      condition.signal();
    } finally {
      lock.unlock();
    }
  }
}
```

2. **Rajouter à votre classe Semaphore une méthode main qui crée un sémaphore contenant 5 permis puis démarre 10 threads. 
    Chaque thread attend de pouvoir prendre un permis. Puis, après l'avoir pris, il attend 1 seconde et rend le permis. 
    Chaque thread affichera un message (avec son numéro) après avoir acquis et après avoir rendu un permis.**

Main:
```java
public static void main(String[] args) {
  var semaphore = new Semaphore(5);
  var nbThreads = 10;
  
  IntStream.range(0, nbThreads).forEach(e -> {
    Thread.ofPlatform().name("Thread " + e).start(() -> {
      try {
        semaphore.acquire();
        System.out.println(Thread.currentThread().getName() + " has acquired its permit.");
        Thread.sleep(1_000);
      } catch (InterruptedException ex) {
        throw new AssertionError(ex);
      }
      System.out.println(Thread.currentThread().getName() + " has released its permit.");
      semaphore.release();
    });
  });
}
```

3. **On veut étendre les fonctionnalités de notre classe en rajoutant la possibilité de savoir combien de threads sont bloqués en attente d'un permis d'exécution. 
    Pour cela, on va rajouter une méthode int waitingForPermits() qui renvoie le nombre de threads qui sont en attente dans acquire.
    Recopier votre classe Semaphore dans une classe SemaphoreClosable et rajouter la méthode waitingForPermits décrite ci-dessus. 
    Modifier le code pour que le main attende 1 seconde après le démarrage des threads et affiche la valeur renvoyée par waitingForPermits.**

Classe `SemaphoreClosable`:
```java
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class SemaphoreClosable {
  private int nbPermits;
  private int nbWaiting;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public SemaphoreClosable(int nbPermits) {
    lock.lock();
    try {
      this.nbPermits = nbPermits;
    } finally {
      lock.unlock();
    }
  }

  public boolean tryAcquire() {
    lock.lock();
    try {
      if (nbPermits > 1) {
        nbPermits--;
        return true;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  public void acquire() throws InterruptedException {
    lock.lock();
    try {
      nbWaiting++;
      while (nbPermits == 0) {
        condition.await();
      }
      nbWaiting--;
      nbPermits--;
    } finally {
      lock.unlock();
    }
  }

  public void release() {
    lock.lock();
    try {
      nbPermits++;
      condition.signal();
    } finally {
      lock.unlock();
    }
  }

  public int waitingForPermits() {
    lock.lock();
    try {
      return nbWaiting;
      // return lock.getHoldCount();
    } finally {
      lock.unlock();
    }
  }

  public static void main(String[] args) {
    var semaphore = new SemaphoreClosable(5);
    var nbThreads = 10;

    IntStream.range(0, nbThreads).forEach(e -> {
      Thread.ofPlatform().name("Thread " + e).start(() -> {
        try {
          semaphore.acquire();
          System.out.println(Thread.currentThread().getName() + " has acquired its permit.");
          Thread.sleep(1_000);
        } catch (InterruptedException ex) {
          throw new AssertionError(ex);
        }
        System.out.println(Thread.currentThread().getName() + " has released its permit.");
        semaphore.release();
      });
    });

    for (int i = 1;; i++) {
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new AssertionError();
      }
      System.out.println(i + " seconds: " + semaphore.waitingForPermits() + " thread(s) currently waiting for a permit.");
    }
  }
}
```

4. **On veut maintenant ajouter la possibilité de fermer le sémaphore. 
    Pour cela on va rajouter une méthode void close(). 
    La fermeture du sémaphore devra avoir le comportement suivant :**
- les nouveaux appels à acquire et tryAcquire lèvent une IllegalStateException,
- les threads bloqués dans acquire lèvent une AsynchronousCloseException,
- les nouveaux appels à release n'ont aucun effet.

**Rajouter la méthode close décrite ci-dessus à votre classe SemaphoreClosable.**

Classe `SemaphoreClosable`:
```java
import java.nio.channels.AsynchronousCloseException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;

public class SemaphoreClosable {
  private int nbPermits;
  private int nbWaiting;
  private boolean isClosed;
  private final ReentrantLock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();

  public SemaphoreClosable(int nbPermits) {
    lock.lock();
    try {
      this.nbPermits = nbPermits;
    } finally {
      lock.unlock();
    }
  }

  public boolean tryAcquire() {
    lock.lock();
    try {
      if (isClosed) {
        throw new IllegalStateException("Semaphore is closed.");
      }
      if (nbPermits > 1) {
        nbPermits--;
        return true;
      }
      return false;
    } finally {
      lock.unlock();
    }
  }

  public void acquire() throws InterruptedException, AsynchronousCloseException {
    lock.lock();
    try {
      if (isClosed) {
        throw new IllegalStateException("Semaphore is closed.");
      }
      nbWaiting++;
      while (nbPermits == 0) {
        condition.await();
        if (isClosed) {
          throw new AsynchronousCloseException();
        }
      }
      nbWaiting--;
      nbPermits--;
    } finally {
      lock.unlock();
    }
  }

  public void release() {
    lock.lock();
    try {
      if (isClosed) {
        return;
      }
      nbPermits++;
      condition.signal();
    } finally {
      lock.unlock();
    }
  }

  public int waitingForPermits() {
    lock.lock();
    try {
      return nbWaiting;
    } finally {
      lock.unlock();
    }
  }

  public void close() {
    lock.lock();
    try {
      if (!isClosed) {
        isClosed = true;
        condition.signalAll();
      }
    } finally {
      lock.unlock();
    }
  }
}
```

5. **On veut modifier le main pour qu'après avoir démarré les threads, il attende 10 secondes et ferme le sémaphore s'il n'y a aucun thread en attente dans un acquire. Quelqu'un propose le code ci-dessous :**
```java
var semaphore = new Semaphore(...);
// ...
if (semaphore.waitingForPermits() == 0){
  semaphore.close();
}
```

**Quel est le problème avec le code proposé ?**
x

**Comment faire évoluer la classe SemaphoreClosable pour pouvoir obtenir cette fonctionnalité ?**
Main:
```java
public static void main(String[] args) {
  var semaphore = new SemaphoreClosable(5);
  var nbThreads = 10;
  
  IntStream.range(0, nbThreads).forEach(e -> {
    Thread.ofPlatform().name("Thread " + e).start(() -> {
      try {
        semaphore.acquire();
        System.out.println(Thread.currentThread().getName() + " has acquired its permit.");
        Thread.sleep(1_000);
      } catch (InterruptedException | AsynchronousCloseException ex) {
        throw new AssertionError(ex);
      }
      System.out.println(Thread.currentThread().getName() + " has released its permit.");
      semaphore.release();
    });
  });
  
  for (int i = 1;; i++) {
    try {
      Thread.sleep(1000);
    } catch (InterruptedException e) {
      throw new AssertionError();
    }
    var waitingForPermits = semaphore.waitingForPermits();
    System.out.println(i + " seconds: " + waitingForPermits + " thread(s) currently waiting for a permit.");
    if (i == 10 && waitingForPermits != 0) {
      semaphore.close();
      break;
    }
  }
}
```