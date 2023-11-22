package fr.uge.concurrence.producteurs;

import fr.uge.concurrence.producteurs.WareHouse.Order;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.IntStream;

/* Everything stops if a missing item is present */
public class DeliverySystemBonus {
  public static void main(String[] args) {
    var nbRetrieve = 1;
    var nbPrepare = 3;
    var nbSend = 10;
    var max = 16;
    var retrieveQueue = new ArrayBlockingQueue<Order>(max);
    var prepareMap = new HashMap<Integer, ArrayBlockingQueue<Order>>();
    for (int i = 0; i < WareHouse.DESTINATIONS; i++) {
      prepareMap.put(i, new ArrayBlockingQueue<>(nbSend));
    }

    var threads = new ArrayList<Thread>(nbRetrieve + nbPrepare + WareHouse.DESTINATIONS);

    IntStream.range(0, nbRetrieve).forEach(i -> {
      threads.add(Thread.ofPlatform().unstarted(() -> {
        for(;;) {
          try {
            var currentOrder = WareHouse.nextOrder();
            retrieveQueue.put(currentOrder);
            System.out.println("new order : " + currentOrder);
          } catch (InterruptedException e) {
            return;
          }
        }
      }));
    });

    IntStream.range(0, nbPrepare).forEach(i -> {
      threads.add(Thread.ofPlatform().unstarted(() -> {
        for(;;) {
          try {
            var currentOrder = retrieveQueue.take();
            var prepare = WareHouse.prepareParcel(currentOrder);
            if (prepare.isPresent()) {
              System.out.println("--> " + currentOrder + " for dispatch " + prepare.get());
              prepareMap.get(prepare.get()).put(currentOrder);
            } else {
              System.out.println("missing item : " + currentOrder + ". Stopping everything.");
              for (var thread : threads) {
                thread.interrupt();
              }
              return; // We also stop the current thread
            }
          } catch (InterruptedException e) {
            return;
          }
        }
      }));
    });

    IntStream.range(0, WareHouse.DESTINATIONS).forEach(i -> {
      threads.add(Thread.ofPlatform().unstarted(() -> {
        var list = new ArrayList<Order>();
        for(;;) {
          if (list.size() == nbSend) {
            WareHouse.checkDelivery(i, list);
            System.out.println("delivery to " + i + " : " + list);
            list.clear();
          }

          try {
            list.add(prepareMap.get(i).take());
          } catch (InterruptedException e) {
            return;
          }
        }
      }));
    });

    for (var thread : threads) {
      thread.start();
    }
  }
}
