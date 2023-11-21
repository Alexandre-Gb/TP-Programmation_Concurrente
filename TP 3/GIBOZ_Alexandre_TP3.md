# TP3 - Synchronized
## GIBOZ Alexandre, INFO2 2023-2025
***

[Énoncé](https://igm.univ-mlv.fr/coursconcurrenceinfo2/tds/td03.html)
***

## Exercice 1 - When things pile up (come back)

Commencer par mettre l'instruction d'ajout dans un bloc synchronisé, afin de constater qu'a priori, on ne perd plus de valeurs à cause de l'entrelacement des instructions. 

```java
public class HelloListBug {
  public static void main(String[] args) throws InterruptedException {
    var nbThreads = 4;
    var threads = new Thread[nbThreads];
    var list = new ArrayList<Integer>(5_000 * nbThreads);
    var lock = new Object();

    IntStream.range(0, nbThreads).forEach(j -> {
      Runnable runnable = () -> {
        for (var i = 0; i < 5_000; i++) {
          synchronized (lock) {
            list.add(i);
          }
        }
      };

      threads[j] = Thread.ofPlatform().start(runnable);
    });

    for (Thread thread : threads) {
      thread.join();
    }

    System.out.println("taille de la liste:" + list.size());
  }
}
```

1. **Rappeler quelles doivent être les propriétés de l'objet qui sert de lock.**

C'est un objet, il ne doit pas être null ou primitif.
L'objet ne doit pas utiliser d'interning (String, Integer, etc.).
On souhaite limiter l'accès à l'objet au maximum, donc il doit être privé et final (encapsulation).
L'objet doit servir de référence commune.

2. **Réaliser une classe ThreadSafeList qui implémente une liste thread-safe permettant d'ajouter d'un élément et de récupérer la taille de la liste. 
    Utiliser cette classe pour écrire une classe HelloListFixedBetter.**

Classe `ThreadSafeList`:
```java
public class ThreadSafeList {
  private final ArrayList<Integer> list = new ArrayList<>();
  private final Object lock = new Object();
  
  public void add(Integer i) {
    synchronized (lock) {
      list.add(i);
    }
  }

  public int size() {
    synchronized (lock) {
      return list.size();
    }
  }
}
```

Classe `HelloListFixedBetter`:
```java
import java.util.stream.IntStream;

public class HelloListFixedBetter {
  public static void main(String[] args) throws InterruptedException {
    var nbThreads = 4;
    var threads = new Thread[nbThreads];
    var fixedList = new ThreadSafeList();

    IntStream.range(0, nbThreads).forEach(j -> {
      Runnable runnable = () -> {
        for (var i = 0; i < 5_000; i++) {
          fixedList.add(i);
        }
      };

      threads[j] = Thread.ofPlatform().start(runnable);
    });

    for (Thread thread : threads) {
      thread.join();
    }

    System.out.println("taille de la liste:" + fixedList.size());
  }
}
```

3. **Au lieu d'afficher la taille de la liste, on veut afficher le contenu de la liste. 
    Modifier la classe ThreadSafeList pour permettre cette évolution.**

On modifie la classe `ThreadSafeList` pour override la méthode `toString`:
```java
  @Override
  public String toString() {
    synchronized (lock) {
      return list.toString();

      // Equivalent to the default tostring method of a list
      // return list.stream().map(Object::toString)
      //   .collect(Collectors.joining(", "));
    }
  }
```

L'affichage n'étant pas atomique (la size peut être bougée entre temps, des valeurs ajoutées...), on doit aussi utiliser le lock.

<br>

## Exercice 2 - Tableau d'honneur

1. **Expliquer pourquoi la classe HonorBoard n'est pas thread-safe.
   Si vous ne voyez pas, faites un grep "Mickey Duck" sur la sortie du programme et donner un scénario pouvant mener à cet affichage.**

La data-race a lieu sur les propriétés `firstName` et `lastName` de la classe `Student`.
La classe n'utilise aucun lock, et les propriétés sont modifiées dans la méthode `set` de la classe `HonorBoard`.
Les différentes opérations (modification de propriétés de l'objet, toString) ne sont pas atomiques, et peuvent être dé-schedulées en plein milieu de l'opération, causant
un comportement incohérent. 

Par exemple, "Mickey Duck" (ou d'autres combinaisons incohérents) peut être affiché dans le cas ou un thread est dé-schedulé entre 
l'affichage de la première propriété et la seconde, et qu'un autre reprend son toString qui a été interrompu. Le scénario est peu probable et donc difficile
à observer, nécessitant un grand nombre d'itérations avant de pouvoir constater le problème.

2. **Modifier le code de la classe HonorBoard pour la rendre thread-safe.**

On modifie la classe pour obtenir le résultat suivant:
```java
public class HonorBoard {
  private String firstName;
  private String lastName;
  private final Object lock = new Object();
  
  public void set(String firstName, String lastName) {
    synchronized(lock) {
      this.firstName = firstName;
      this.lastName = lastName;
    }
  }
  
  @Override
  public String toString() {
    synchronized(lock) {
      return firstName + ' ' + lastName;
    }
  }

// ...
}
```

3. **Maintenant que votre classe est thread-safe, peut-on remplacer la ligne :**
```java
System.out.println(board);
```

Par la ligne:
```java
System.out.println(board.firstName() + ' ' + board.lastName());
```

**Avec les deux accesseurs définis comme d'habitude (et à condition d'utiliser des bloc synchronized) ?**

Non, bien que la classe soit convenablement conçue (lock dans toString et dans les accesseurs), elle est incorrectement utilisée.
Là où la première ligne prend le lock et construit correctement la chaîne, la deuxième ligne prend le lock, mais ne construit pas la chaîne atomiquement.
Il est important de conserver le lock pendant la totalité d'une opération.

<br>

## Exercice 3 - Maximum Thread-safe

Dans cet exercice, on souhaite écrire une classe thread-safe qui permet à plusieurs threads de proposer des valeurs entières simultanément et de savoir à tout moment quelle est la valeur maximale proposée.
L'objectif final est d'écrire un programme qui démarre 4 threads qui proposent 10 valeurs entières en boucle, pendant qu'un autre thread demande, à intervalles de temps réguliers, quelle est le maximum courant des valeurs proposées par les threads.

1. **Pourquoi a-t-on besoin d'une classe thread-safe pour réaliser ce programme ? Quelles doivent être les méthodes fournies par cette classe ?**

Une classe thread-safe est une classe qui empêche d'obtenir un état incohérent de la classe, et ce 
même bien qu'elle soit utilisée par plusieurs threads en parallèle.

Il est nécessaire que la classe soit thread-safe car elle est effectivement utilisée par plusieurs threads, et que
pendant qu'un est occupé à ajouter des valeurs, l'autre y accède au même moment, ce qui peut causer des incohérences.

Les méthodes (thread-safe) de la classe peuvent être:
- public Optional<Integer> max() : retourne la valeur maximale actuelle si elle existe, ou un Optional vide sinon.
- public void propose(int newValue) : remplace la valeur maximale actuelle par newValue si newValue est supérieur à la valeur maximale actuelle

3. **Écrire le code de la classe thread-safe CurrentMaximum afin que le code du main fonctionne.**

On obtient la classe suivante :
```java
import java.util.Optional;

public class CurrentMaximum {
  public static int MAX_VALUE = 10_000;
  private Integer value;
  private final Object lock = new Object();

  public Optional<Integer> max() {
    synchronized (lock) {
      // return value == null ? Optional.empty() : Optional.of(value);
      return Optional.ofNullable(value);
    }
  }

  public void propose(int newValue) {
    synchronized (lock) {
      if (value == null || newValue > value) {
        value = newValue;
      }
    }
  }

  public static class Main {
    public static void main(String[] args) {
      var nbThreads = 4;
      var nbLoops = 10;
      var threadList = new ArrayList<Thread>(nbThreads);
      var currentMaximum = new CurrentMaximum();

      IntStream.range(0, nbThreads).forEach(i -> {
        threadList.add(Thread.ofPlatform().name("Thread-" + i).start(() -> {
          for (int j = 0; j < nbLoops; j++) {
            try {
              Thread.sleep(1_000);
            } catch (InterruptedException e) {
              throw new AssertionError(e);
            }

            var newValue = ThreadLocalRandom.current().nextInt(MAX_VALUE);
            System.out.println(Thread.currentThread().getName() + " propose " + newValue);
            currentMaximum.propose(newValue);
          }
        }));
      });

      Optional<Integer> max;
      for (int i = 0; i < nbLoops; i++) {
        try {
          Thread.sleep(1_000);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }

        max = currentMaximum.max();
        max.ifPresentOrElse(
                val -> System.out.println("Max courant : " + val),
                () -> System.out.println("Pas de max courant pour le moment.")
        );
      }

      for (var thread : threadList) {
        try {
          thread.join();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }

      max = currentMaximum.max();
      max.ifPresentOrElse(
              val -> System.out.println("Max final : " + val),
              () -> System.out.println("Pas de max final.")
      );
    }
  }
}
```

Exceptionnellement, on peut utiliser un Integer comme propriété, car on ne peut pas savoir la véritable valeur
max dans le cas d'un int si la valeur max n'a jamais excédé 0 (la gestion de valeur par défaut est impossible à faire proprement avec un int primitif).

4. **On souhaite maintenant pouvoir afficher le thread qui a proposé le maximum en même temps que sa valeur. Modifier le code en conséquence.**

On modifie le code :
```java
public class CurrentMaximum {
  public static int MAX_VALUE = 10_000;
  private Integer value;
  private Thread maxThread;
  private final Object lock = new Object();

  public Optional<Integer> max() {
    synchronized (lock) {
      return Optional.ofNullable(value);
    }
  }

  public void propose(int newValue) {
    synchronized (lock) {
      if (value == null || newValue > value) {
        value = newValue;
        maxThread = Thread.currentThread();
      }
    }
  }

  public Optional<Thread> maxThread() {
    synchronized (lock) {
      return Optional.ofNullable(maxThread);
    }
  }

  public static class Main {
    public static void main(String[] args) {
      var nbThreads = 4;
      var nbLoops = 10;
      var threadList = new ArrayList<Thread>(nbThreads);
      var currentMaximum = new CurrentMaximum();

      IntStream.range(0, nbThreads).forEach(i -> {
        threadList.add(Thread.ofPlatform().name("Thread-" + i).start(() -> {
          for (int j = 0; j < nbLoops; j++) {
            try {
              Thread.sleep(1_000);
            } catch (InterruptedException e) {
              throw new AssertionError(e);
            }

            var newValue = ThreadLocalRandom.current().nextInt(MAX_VALUE);
            System.out.println(Thread.currentThread().getName() + " propose " + newValue);
            currentMaximum.propose(newValue);
          }
        }));
      });

      Optional<Integer> max;
      Optional<Thread> maxThread;
      for (int i = 0; i < nbLoops; i++) {
        try {
          Thread.sleep(1_000);
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }

        max = currentMaximum.max();
        maxThread = currentMaximum.maxThread();
        if (max.isPresent() && maxThread.isPresent()) {
          System.out.println("Max courant : " + max.get() + " proposé par " + maxThread.get().getName());
        } else {
          System.out.println("Pas de max courant pour le moment.");
        }
      }

      for (var thread : threadList) {
        try {
          thread.join();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }

      max = currentMaximum.max();
      maxThread = currentMaximum.maxThread();
      if (max.isPresent() && maxThread.isPresent()) {
        System.out.println("Max final : " + max.get() + " proposé par " + maxThread.get().getName());
      } else {
        System.out.println("Pas de max final.");
      }
    }
  }
}
```

<br>

## Exercice 4 - Méthode bloquante ?

Dans cet exercice, on souhaite écrire une classe RendezVous thread-safe qui permet de réaliser le passage d'une valeur entre des threads. 
C'est une première tentative qui ne fonctionnera pas de réaliser une méthode bloquante. 
Nous verrons au prochain cours comment résoudre ce problème de façon satisfaisante.

Dans le programme FindPrime.java, plusieurs threads sont démarrés et tirent des grands nombres au hasard jusqu'à en trouver un qui soit premier. 
Le principe est que le main attend jusqu'à ce que l'un des threads ait trouvé un nombre premier. 
Pour réaliser le passage de la valeur entre les threads qui cherchent des nombres premiers et le main, nous allons écrire une classe RendezVous.

La classe RendezVous est sensée être une classe thread-safe qui offre un méthode set permettant de proposer une valeur et une méthode get qui ''bloque'' jusqu'à ce qu'une valeur ait été proposée et la renvoie lorsque c'est le cas.

1. **Que se passe-t-il lorsqu'on exécute ce code ?**

Lorsque l'on exécute le programme, au bout d'une quinzaine de secondes, un message "I found a large prime number : " suivi d'un nombre premier est affiché.
```
I found a large prime number : 16729414019
```

Le code va lancer 4 threads daemon qui vont chercher un nombre premier en additionnant à une variable "nb" la valeur de la constante "BIG_LONG"
et une valeur pseudo-aléatoire comprise entre 0 et BIG_LONG. 

2. **Commenter l'instruction Thread.sleep(1) dans la méthode get puis ré-exécuter le code. Que se passe-t-il ? Expliquer où est le bug ?**

Les quatres threads trouvent une valeur première, on le sait grâce à l'apparition des messages dans le terminal, cependant, rien d'autre ne se produit 
et le programme ne se termine pas.

Le JIT est la pour optimiser le code, mais ne se focalise pas sur l'aspect de concurrence pendant le processus d'optimisation.
Il est possible que le JIT ait vu rapidement (au bout de quelques centaines de tours) que la valeur "value" n'allait jamais changer, et a
donc altéré le code.

3. **Écrire une classe thread-safe RendezVous sur le même principe que la classe StupidRendezVous mais qui fonctionne correctement, que l'instruction Thread.sleep(1) soit commentée ou non.**

Classe `RendezVous`:
```java
import java.util.Objects;

public class RendezVous<V> {
  private V value;
  private final Object lock = new Object();

  public void set(V value) {
    synchronized (lock) {
      Objects.requireNonNull(value);
      this.value = value;
    }
  }

  public V get() throws InterruptedException {
    while (value == null) {
      Thread.sleep(1);
    }
    synchronized (lock) {
      return value;
    }
  }
}
```

4. **Regarder l'utilisation du CPU par votre programme avec la commande top. Votre code fait de l'attente active ce qui n'est pas une solution acceptable, mais vous n'avez pas les outils pour corriger cela pour l'instant. 
    Nous verrons au prochain cours comment réaliser une méthode bloquante sans faire de l'attente active.**

On constate effectivement une très forte utilisation du CPU par le programme (supérieure à 50% dans mon cas).

