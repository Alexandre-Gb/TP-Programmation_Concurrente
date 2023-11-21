# TP5 -  Signaux (suite) - Deadlock
## GIBOZ Alexandre, INFO2 2023-2025
***

[Énoncé](https://igm.univ-mlv.fr/coursconcurrenceinfo2/tds/td05.html)
***

## Exercice 1 - Exchanger

On souhaite implanter un Exchanger qui permet d'échanger deux valeurs entre deux threads. 

L'idée est qu'un premier thread va envoyer une valeur à l'Exchanger en utilisant la méthode exchange, celui-ci va bloquer le thread qui a fait appel à la méthode exchange et attendre (on suppose qu'il n'y a que 2 threads et que chacun appelle exchange exactement une fois). 
Lorsque un second thread fait lui aussi un appel à la méthode exchange avec une seconde valeur, l'appel retourne la première valeur envoyée et le premier thread est dé-bloqué de son appel à exchange en retournant la seconde valeur (attention, les valeurs peuvent être nulles).

En fait, la classe Exchanger existe déjà en Java (dans le package java.util.concurrent), et voici un exemple de code l'utilisant.
```java
public class ExchangerExample {
  public static void main(String[] args) throws InterruptedException {
    var exchanger = new Exchanger<String>();
    Thread.ofPlatform().start(() -> {
      try {
        System.out.println("thread 1 " + exchanger.exchange("foo1"));
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    });
    System.out.println("main " + exchanger.exchange(null));
  }
}
```

1. **Quel est l'affichage attendu ?**

On peut obtenir un des deux résultats suivants :
```
thread 1 null
main foo1
```

```
main foo1
thread 1 null
```

On se propose de ré-implanter cette classe. 

2. **Comment faire pour distinguer le premier et le second appel à la méthode exchange ?**

Il suffit d'implémenter un booléen "isSecondCall" qui est initialisé à false. 
Lors du premier appel à la méthode exchange, on le passe à true. Lors du second appel, on le repasse à false.

3. **Écrire le code de la classe Exchanger.**

Classe `MyExchanger`:
```java
public class MyExchanger<T> {
  private T value1 = null;
  private T value2 = null;
  private boolean isSecond; // False
  private boolean secondValueHere; // False
  private final Object lock = new Object();

  public T exchange(T value) throws InterruptedException {
    synchronized (lock) {
      if(!isSecond) {
        value1 = value;
        isSecond = true;

        // Always a while around a wait, or else it can be waked up for the wrong reason, 
        // Nothing else besides the wait inside the loop
        while(!secondValueHere) {
          lock.wait();
        }
        return value2;
      } else {
        value2 = value;
        secondValueHere = true;
        lock.notify();
        return value1;
      }
    }
  }
}
```

## Exercice 2 - Contrat de classe thread-safe

L'exercice suivant est à nouveau un exercice de contrat : on vous demande d'écrire une classe thread-safe sans spécifier quelles sont les méthodes de la classe. 
Pour un problème donné, c'est à vous de décider quelle classe vous allez créer, afin qu'elle soit utilisée par plusieurs threads, pour résoudre ce problème. 
Une telle classe doit être une librairie, relativement simple, avec un rôle bien identifié. 
En particulier, les méthodes de cette classe ne sont pas sensées faire des calculs très longs ni des affichages. 

Dans cet exercice, on utilise une API pour qui permet d'obtenir la température d'une pièce de la maison (on imagine que chaque pièce de la maison a un capteur relié à internet). 
L'API permet d'obtenir la température d'une pièce à partir de son nom (une String). 
Cette requête prend un peu de temps. 
L'API se résume à la classe Heat4J qui ne possède qu'une méthode statique retrieveTemperature(String roomName).
Elle s'utilise tout simplement comme suit : 
```java
var temperature = Heat4J.retrieveTemperature("kitchen");
```

Pour des raisons de simplicité l'API, ici le code fournit ne fait pas vraiment de requête. 
Il attend un peu et tire simplement la température au hasard. 

Le but de l'exercice est de pouvoir modifier le main de Application pour que :

les appels à Heat4J.retrieveTemperature soient tous fait dans des threads différents. Il y aura donc un thread par pièce ;
le thread main fasse l'affichage de la moyenne.
Il va donc falloir faire communiquer les threads qui font les appels à Heat4J.retrieveTemperature avec le thread main. 
Pour cela, vous devez réaliser une classe thread-safe de votre choix qui servira à faire communiquer tous les threads. 
Il est interdit d'utiliser de la synchronisation en dehors de cette classe. Les méthodes de cette classe ne doivent pas faire d'affichage. 
De plus, pour cet exercice, on vous demande de ne pas utiliser de join() dans le main (ça fonctionne pour la première question, mais pas pour la deuxième).

1. **Écrivez votre classe thread-safe et modifiez le main de la classe Application pour qu'il utilise cette classe et les thread indiqués ci-dessus.**

On créé la classe `Heat4JThreadSafe`:
```java
public class Heat4JThreadSafe {
  private final HashMap<String, Integer> temperatures = new HashMap<>();
  private final int maxRooms;
  private final Object lock = new Object();

  public Heat4JThreadSafe(int maxRooms) {
    if (maxRooms < 1) { throw new IllegalArgumentException(); }
    this.maxRooms = maxRooms;
  }

  public int retrieveTemperature(String roomName) {
    Objects.requireNonNull(roomName);

    synchronized (lock) {
      if (temperatures.size() == maxRooms) { throw new IllegalStateException("No more space available."); }
      if (temperatures.containsKey(roomName)) { throw new IllegalStateException("The room was already given."); }

      int temperature;
      try {
        temperature = Heat4J.retrieveTemperature(roomName);
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
      temperatures.put(roomName, temperature);

      if (temperatures.size() == maxRooms) {
        lock.notify();
      }
      return temperature;
    }
  }

  public double retrieveAverageTemperature() throws InterruptedException {
    synchronized (lock) {
      while (temperatures.size() != maxRooms) {
        lock.wait();
      }

      return temperatures.values().stream()
              .mapToInt(Integer::intValue)
              .average()
              .getAsDouble();
    }
  }

  public static class Application {
    public static void main(String[] args) throws InterruptedException {
      var rooms = List.of("bedroom1", "bedroom2", "kitchen", "dining-room", "bathroom", "toilets");
      var heat4J = new Heat4JThreadSafe(rooms.size());

      for (var room : rooms) {
        Thread.ofPlatform().start(() -> {
          var temperature = heat4J.retrieveTemperature(room);
          System.out.println("Temperature in room " + room + " : " + temperature);
        });
      }

      System.out.println("Average temperature: " + heat4J.retrieveAverageTemperature());
    }
  }
}
```