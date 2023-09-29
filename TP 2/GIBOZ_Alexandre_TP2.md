# TP2 -  Data race, entrelacement et JIT
## GIBOZ Alexandre, INFO2 2023-2025
***

[Énoncé](https://igm.univ-mlv.fr/coursconcurrenceinfo2/tds/td02.html)
***

## Exercice 0 - When things add up

1. **Récupérez la classe Counter.java et exécutez-la plusieurs fois.**

```java
public class Counter {
  private int value;

  public void addALot() {
    for (var i = 0; i < 100_000; i++) {
      this.value++;
    }
  }

  public static void main(String[] args) throws InterruptedException {
    var counter = new Counter();
    var thread1 = Thread.ofPlatform().start(counter::addALot);
    var thread2 = Thread.ofPlatform().start(counter::addALot);
    thread1.join();
    thread2.join();
    System.out.println(counter.value);
  }
}
```

2. **Essayez d'expliquer ce que vous observez.**

Les champs des objets font partie du tas (mémoire) partagé entre les theads.
Si un thread modifie une variable, les autres threads peuvent voir la modification, mais si un thread est interrompu par
le scheduler au milieu de l'opération d'incrémentation, la valeur de la variable sera faussée lorsqu'il reprendra son opération.

Les opérations, même une simple incrémentation, n'est pas atomique et ne s'effectue pas en un seul temps. Il est possible que
le thread soit dé-schedulé au milieu de l'opération, quand ce dernier récupère la valeur en mémoire, sans avoir eu le temps de la "put" en mémoire après l'incrémentation.

3. **Est-il possible que ce code affiche moins que 100 000 ? Expliquer précisément pourquoi.**

Le thread 1 est schedulé et ne fait que le premier load (de 0) correspondant au premier value++, puis ce dernier est dé-schedulé.
**(en mémoire, value = 0)**
    
Le thread 2 est schedulé et fait les premiers 99_999 passages de boucle, puis est dé-schedulé avant le value++ final.
**(en mémoire, value = 99_999)**

Le thread 1 est schedulé et continue sa première incrémentation, il a récupéré 0 au début et va à présent l'incrémenter et stocker la 
valeur en mémoire.
**(en mémoire, value = 1)**

Le thread 2 est schedulé et effectue le load qui correspond à son opération value++ finale, il récupère 1 puis est dé-schedulé.
**(en mémoire, value = 1)**

Le thread 1 est schedulé et effectue toutes ces opérations jusqu'a la fin.
**(en mémoire, value = 100_000)**

Le thread 2 est schedulé et termine son opération, en incrémentant la valeur qu'il avait récupéré avant d'être dé-schedulé.
La valeur récupérée étant 1, il l'incrémente et la stocke en mémoire.
**(en mémoire, value = 2)**

Dans ce scénario (peu probable), la valeur finale est 2, ce qui est inférieur à 100_000.

<br>

## Exercice 1 - Stop now !

1. **Récupérer la classe StopThreadBug.java. Avant de l'exécuter, essayer de comprendre quel est le comportement espéré. 
    Où se trouve la data-race ?**

On dit au thread de s'arreter depuis une valeur stockée dans le tas partagé. On met la valeur booléenne sur true au bout de
5 secondes afin d'interrompre le premier thread "à distance". Cette méthode d'interruption du thread à distance est relativement sale mais très commun en entreprise.

On appelle "data-race" le fait que plusieurs threads manipulent un même champ d'un même objet.
Certains langages comme Rust empêchent complètement ce genre de comportement, mais Java ne le fait pas
Il est important d'identifier, à chaque fois qu'on se trouve face à un problème, la (les) data-race(s).

Il y'a une data-race sur le champ "stop" de l'objet "StopThreadBug", car plusieurs threads vont manipuler cette valeur.

2. **Exécuter la classe plusieurs fois. Qu'observez-vous ?**

Cela fonctionne comme prévu, le thread s'arrête au bout de 5 secondes et la JVM est fermée.

3. **Modifiez la classe StopThreadBug.java pour supprimer l'affichage dans la boucle du thread. 
    Exécuter la classe à nouveau plusieurs fois. Essayez d'expliquer ce comportement.**

Si l'on commente le print dans la boucle, le thread ne s'arrête plus, et il en va donc de même pour la JVM.

Le problème vient du JIT et de sa méthode d'optimisation du code. Ce dernier optimise le code en prenant en considération qu'un seul thread
va utiliser le code, sans considérer que l'on va modifier une valeur sur le tas avec un autre thread.

Le JIT n'optimise pas la boucle si un print est présent, car 99% du temps et de la puissance est basée a faire de l'affichage, et rien d'autre.
Le JIT considère donc qu'il n'y a rien à optimiser, et le code marche. Si l'on commente le print, le JIT optimise le code et le thread ne s'arrête pas.

4. **Le code avec l'affichage va-t-il toujours finir par arrêter le thread ?**



<br>

## Exercice 2 - Les fourberies du JIT

On obsèrve une data-race sur l'objet "ExempleReordering" et les champs "a" et "b".

1. **Quand on exécute le code (ExempleReordering), quels peuvent être les différents affichages constatés ?**

Les cas triviaux suivants sont possibles:
0 0
1 0
1 2
0 2

Cependant, il est possible que le thread soit dé-schedulé pendant la construction de la chaîne, car 
aucune opération (en particulier une opération de construction d'arguments) n'est atomique.

Le JIT (le processeur également, les deux font des optimisations de code) peuvent également ré-ordonner les valeurs.

2. **Quand on exécute le code (ExampleLongAffectation), quels peuvent être les différents affichages constatés ?**

Un long est codé sur 64 bits, et la valeur -1 est codée sur 64 bits de 1. -1 est donc égal à 0xFFFFFFFFFFFFFFFF.
L'affectation d'une valeur en mémoire n'étant pas atomique (y compris pour un long), il est possible, sur une machine < 64bit
(16/32bits) que seul la première partie du long soit correctement formée. Ce cas la est, sur une machine x64, impossible car le long se forme en une fois.

<br>

## Exercice 3 - When things pile up

1. **Recopiez cette classe dans une nouvelle classe HelloListBug puis modifiez-la pour y ajouter les nombres dans la liste au lieu de les afficher. 
    Faites afficher la taille finale de la liste, une fois tous les threads terminés.**

On obtient le code suivant:
```java
public class HelloListBug  {
  public static void main(String[] args) throws InterruptedException {
    var nbThreads = 4;
    var threads = new Thread[nbThreads];
    var list = new ArrayList<Integer>(5000 * nbThreads);

    IntStream.range(0, nbThreads).forEach(j -> {
      Runnable runnable = () -> {
        for (var i = 0; i < 5000; i++) {
          list.add(i);
        }
      };

      threads[j] = Thread.ofPlatform().start(runnable);
    });

    for (var thread : threads) {
      thread.join();
    }

    System.out.println("list.size() = " + list.size());

    System.out.println("le programme est fini");
  }
}
```

2. **Exécuter le programme plusieurs fois et noter les différents affichages.**

Sur 10 exécutions, on obtient les résultats suivants:
- list.size() = 8407
- list.size() = 9138
- list.size() = 9954
- list.size() = 9420
- list.size() = 8841
- list.size() = 9030
- list.size() = 9378
- list.size() = 8667
- list.size() = 9210
- list.size() = 9567

On constate qu'il y a jamais les 20_000 éléments attendus dans la liste, et que la taille de la liste varie à chaque exécution.

3. **Expliquer comment la taille de la liste peut être plus petite que le nombre total d'appels à la méthode add.**

On remarque dans la méthode `add` de la classe `ArrayList` qu'une propriété "modCount" est incrémentée à chaque fois que l'on ajoute un élément afin
de pouvoir localiser la tête de la liste.

