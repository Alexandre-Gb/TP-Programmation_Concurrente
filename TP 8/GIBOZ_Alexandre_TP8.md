# TP8 - Producteurs/Consommateurs
## GIBOZ Alexandre, INFO2 2023-2025
***

[Énoncé](https://igm.univ-mlv.fr/coursconcurrenceinfo2/tds/td08.html)
***

Dans ce TP, on cherche à mettre en application le principe du producteur/consommateur. 
L'idée est de ne jamais faire de synchronisation "à la main", mais plutôt d'apprendre à utiliser judicieusement l'API java.util.concurrent.

<br>

## Exercice 1 - Chaine de décodage

Dans cet exercice, on dispose d'une API simplifiée à l'extrême de décodage de messages reçus depuis internet. Elle fournit 3 méthodes static :
String receive() est une méthode bloquante qui récupère un message codé sur le réseau et le renvoie,
String decode(String codedMsg) est une méthode qui décode un message codé et renvoie le message décodé. Si une erreur survient lors du décodage, cette méthode lève l'exception IllegalArgumentException,
void archive(String decodedMsg) permet d'archiver un message décodé.**

2. **Écrire dans le main d'une classe Codex un programme qui va utiliser 3 threads pour récupérer en boucle les messages codés depuis internet, 2 threads pour les décoder et 1 thread pour archiver les messages décodés. 
     De plus, chaque thread affiche ce qu'il fait. 
     Pour l'instant, on ignore (on ne fait rien dans ce cas) les messages dont le décodage lève une exception.**

Classe `Codex`:
```java
public class Codex {
  private static final int MAX_SIZE = 14;
  public static void main(String[] args) {
    var receiveQueue = new ArrayBlockingQueue<String>(MAX_SIZE);
    var decodedQueue = new ArrayBlockingQueue<String>(MAX_SIZE);
    var nbCollectThreads = 3;
    var nbDecodeThreads = 2;
    var nbArchiveThread = 1;

    IntStream.range(0, nbCollectThreads).forEach((i) -> {
      Thread.ofPlatform().name("Thread-" + i).start(() -> {
        for(;;) {
          try {
            var value = CodeAPI.receive();
            System.out.println(Thread.currentThread().getName() + " collects value: " + value);
            receiveQueue.put(value);
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
        }
      });
    });

    IntStream.range(0, nbDecodeThreads).forEach((i) -> {
      Thread.ofPlatform().name("Thread-" + i).start(() -> {
        for(;;) {
          try {
            var encodedValue = receiveQueue.take();
            System.out.println(Thread.currentThread().getName() + " encodes value: " + encodedValue);
            decodedQueue.put(CodeAPI.decode(encodedValue));
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          } catch (IllegalArgumentException e) {
            System.out.println("Impossible de décoder la valeur: " + e);
          }
        }
      });
    });

    IntStream.range(0, nbArchiveThread).forEach((i) -> {
      Thread.ofPlatform().name("Thread-" + i).start(() -> {
        for(;;) {
          try {
            var toArchive = decodedQueue.take();
            System.out.println(Thread.currentThread().getName() + " archives value: " + toArchive);
            CodeAPI.archive(toArchive);
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
        }
      });
    });
  }
}
```

3. **Copiez votre classe Codex dans une classe CodexWithInterruption et modifier le main pour que le programme s'arrête si l'un des messages codés produit une exception lors de son décodage.**



<br>

## API Request

Le but de cet exercice est d'apprendre à utiliser une API fictive de requêtes Request.java (ce n'est pas un exercice de concurrence). 
Cette API très simplifiée permet de demander le prix d'un objet sur un site de vente en ligne. 
Pour faire cette demande, on crée une Request avec le nom de l'objet et le nom du site. 
La méthode request(int timeoutMilli) effectue la demande et renvoie la réponse sous la forme d'une Answer.java (qui est un objet comparable). 
La méthode bloque en attendant la réponse mais cette attente est bornée à au plus timeoutMilli millisecondes.
```java
Request request = new Request("amazon.fr", "pikachu");
Optional<Answer> answer = request.request(5_000);
if (answer.isPresent()) {
    System.out.println("The price is " + answer.orElseThrow().price());
} else {
    System.out.println("The price could not be retrieved from the site");
}
```

La réponse renvoyée par request() n'est pas nécessairement fructueuse, c'est pourquoi elle renvoie un Optional<Answer>. 
Si une réponse est fournie, on peut récupérer le prix avec answer.price().

La liste (List<String>) de tous les sites connus par l'API Request peut-être obtenue par Request.ALL_SITES.

1. **Compléter la classe CheapestSequential.java pour que la méthode retrieve demande tour à tour le prix de l'item sur tous les sites et renvoie une Answer correspondant au prix le plus faible. 
     Si l'item n'est disponible sur aucun site, la méthode renvoie Optional.empty().**

Méthode `retrieve()`:
```java
public Optional<Answer> retrieve() throws InterruptedException {
  var list = new ArrayList<Optional<Answer>>();
  for(var site : Request.ALL_SITES) {
    var request = new Request(site, item);
    var optional = request.request(timeoutMilliPerRequest);
    list.add(optional);
  }
  
  return list.stream()
        .flatMap(Optional::stream)
        .min(Answer::compareTo);
}
```

<br>

## Exercice 2

Dans cet exercice, on cherche à accélérer la recherche du meilleur prix en utilisant des threads.

Dans une premier temps, on veut écrire une classe Fastest qui lance un thread par site et renvoie la réponse du premier site qui répond. 
Si l'article, n'est présent sur aucun site, la méthode renvoie Optional.empty()
```java
var agregator = new Fastest("tortank", 2_000);
var answer = agregator.retrieve();
System.out.println(answer); // Optional[tortank@... : ...]
```

1. **Écrire la classe Fastest.**

Classe `Fastest`:
```java
public class Fastest {
  private final String item;
  private final int interruptedTime;

  public Fastest(String item, int interruptedTime) {
    Objects.requireNonNull(item);
    if (interruptedTime < 0) { throw new IllegalArgumentException(); }

    this.item = item;
    this.interruptedTime = interruptedTime;
  }

  public Optional<Answer> retrieve() {
    var queue = new SynchronousQueue<Optional<Answer>>();
    var sites = Request.ALL_SITES;
    var threadList = new ArrayList<Thread>();

    IntStream.range(0, sites.size()).forEach(i -> {
      Thread.ofPlatform().start(() -> {
        threadList.add(Thread.currentThread());
        var request = new Request(sites.get(i), item);
        try {
          queue.put(request.request(interruptedTime));
        } catch (InterruptedException e) {
          return;
        }
      });
    });

    try {
      for(int i = 0; i < threadList.size(); i++) {
        var fastestValue = queue.take();
        if(fastestValue.isPresent()) {
          threadList.forEach(Thread::interrupt);
          return fastestValue;
        }
      }
    } catch (InterruptedException e ) {
      return Optional.empty();
    }

    threadList.forEach(Thread::interrupt);
    return Optional.empty();
  }

  public static void main(String[] args) {
    // var fastest = new Fastest("Bonne note en Java Avancé", 4_000);
    var fastest = new Fastest("tortank", 4_000);
    var val = fastest.retrieve();

    val.ifPresentOrElse(
        answer -> System.out.println("Found: " + answer),
        () -> System.out.println("Not found")
    );
  }
}
```

2. **On veut maintenant écrire une classe Cheapest qui lance un thread par site et collecte toutes les réponses pour renvoyer la moins chère. 
     Si l'article, n'est présent sur aucun site, la méthode renvoie Optional.empty()
     Écrire la classe Cheapest.**

Classe `Cheapest`:
```java
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
```

<br>

## Exercice 3

Dans cet exercice, on cherche à pallier le problème du trop grand nombre de threads démarrés simultanément.

L'idée est de démarrer un nombre fixé poolSize de threads, quel que soit le nombre de sites à interroger. 
Dans la suite, nous appellerons ces threads des worker threads. 
L'idée est que les worker threads vont, en boucle, exécuter des requêtes sur les différents sites et communiquer la réponse au thread qui exécute la méthode retrieve. 
Bien sûr, on ne veut pas que deux worker threads effectuent une requête pour le même site.

Pour communiquer entre le thread qui exécute la méthode retrieve et les worker threads, on utilise deux BlockingQueue sitesQueue et answersQueue. 
La file sitesQueue contient initialement tous les sites. 
Les worker threads vont en boucle prendre dans cette file un site, effectuer la Request et mettre la réponse dans la answersQueue.

La classe CheapestPooled prend à sa construction le nombre poolSize de worker threads et la valeur du timeout pour chacune des requêtes. 
Elle implémente une méthode retrieve() qui renvoie le prix le moins élevé, comme la classe Cheapest écrite précédemment.

1. **Quel type de BlockingQueue peut-on utiliser pour sitesQueue et answersQueue ?**

Les queues devant stocker plusieurs éléments, le choix d'une SynchronousQueue n'est pas envisageable. Le meilleur choix semble l'utilisation d'une ArrayBlockingQueue qui aura
pour size le nombre de sites disponibles (`Request.ALL_SITES.size()`).
