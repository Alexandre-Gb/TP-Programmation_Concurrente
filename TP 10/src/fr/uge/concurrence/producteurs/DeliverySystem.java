package fr.uge.concurrence.producteurs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.IntStream;

import fr.uge.concurrence.producteurs.WareHouse.Order;

public class DeliverySystem {
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

    IntStream.range(0, nbRetrieve).forEach(i -> {
      Thread.ofPlatform().start(() -> {
        for(;;) {
          try {
            var currentOrder = WareHouse.nextOrder();
            retrieveQueue.put(currentOrder);
            System.out.println("new order : " + currentOrder);
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
        }
      });
    });

    IntStream.range(0, nbPrepare).forEach(i -> {
      Thread.ofPlatform().start(() -> {
        for(;;) {
          try {
            var currentOrder = retrieveQueue.take();
            var prepare = WareHouse.prepareParcel(currentOrder);
            if (prepare.isPresent()) {
              System.out.println("--> " + currentOrder + " for dispatch " + prepare.get());
              prepareMap.get(prepare.get()).put(currentOrder);
            } else {
              System.out.println("missing item : " + currentOrder);
            }
          } catch (InterruptedException e) {
            throw new AssertionError(e);
          }
        }
      });
    });

    IntStream.range(0, WareHouse.DESTINATIONS).forEach(i -> {
      Thread.ofPlatform().start(() -> {
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
            throw new AssertionError(e);
          }
        }
      });
    });
  }
}
