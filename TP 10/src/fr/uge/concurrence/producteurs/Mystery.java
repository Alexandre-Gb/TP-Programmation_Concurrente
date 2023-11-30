package fr.uge.concurrence.producteurs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.IntStream;


/*
1. La classe Mystery n'est pas thread-safe. Pourquoi ?
Il y a une data race sur la priorité "index" au travers de la méthode "store".
Cette même propriété est utilisée pour récupérer un élément dans le tableau, ce qui peut causer une exception ArrayIndexOutOfBoundsException.
Par ailleurs, l'opération "Arrays.toString" n'est pas atomique (ni concurrente), ce qui rend la méthode toString non thread-safe également.

2. Donner un scénario d'exécution dans lequel le programme lève une ArrayIndexOutOfBoundsException.
Le premier thread exécute la méthode "store" et incrémente "index" à 1. Il est dé-schedulé juste après avoir passé la condition if, mais avant l'ajout de la valeur.
Index = 1

Deux autres threads exécutent la méthode sans être dé-schedulés.
Index = 3

Un dernier thread exécute la méthode et est dé-schedulé juste après l'incrémentation de index.
Index = 4

Le premier thread est re-schedulé, et ajoute la valeur à l'index 4, ce qui lève l'exception.
 */
public class Mystery {
  private final int[] tab = new int[4];
  private int index; // = 0
  private final ReentrantLock lock = new ReentrantLock();

  public void store(int value) {
    lock.lock();
    try {
      index++;
      if (index >= tab.length) {
        index = 0;
      }
      tab[index] = value;
    } finally {
      lock.unlock();
    }
  }

  @Override
  public String toString() {
    lock.lock();
    try {
      return Arrays.toString(tab);
    } finally {
      lock.unlock();
    }
  }

  public static void main(String[] args) {
    var mystery = new Mystery();

    var nbThreads = 3;
    var nbRounds = 100;

    var executorService = Executors.newFixedThreadPool(nbThreads);
    var callables = new ArrayList<Callable<String>>();

    IntStream.range(0, nbThreads * nbRounds).forEach(i -> callables.add(() -> {
      mystery.store(ThreadLocalRandom.current().nextInt(10));
      return mystery.toString();
    }));

    // List<Future<String>> futures;
    try {
      var futures = executorService.invokeAll(callables);

      for (var future : futures) {
        System.out.println(future.get());
      }
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    } catch (ExecutionException e) {
      System.out.println("oops");
    }

    executorService.shutdown();
  }
}