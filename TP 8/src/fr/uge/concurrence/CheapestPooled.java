package fr.uge.concurrence;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class CheapestPooled {
  private final int poolSize;
  private final int interruptedTime;
  private final BlockingQueue<String> sitesQueue = new ArrayBlockingQueue<>(Request.ALL_SITES.size());
  private final BlockingQueue<Answer> answersQueue = new ArrayBlockingQueue<>(Request.ALL_SITES.size());

  public CheapestPooled(int poolSize, int interruptedTime) {
    if (poolSize < 0) { throw new IllegalArgumentException(); }
    if (interruptedTime < 0) { throw new IllegalArgumentException(); }

    this.poolSize = poolSize;
    this.interruptedTime = interruptedTime;
    this.sitesQueue.addAll(Request.ALL_SITES);
  }

  public static void main(String[] args) {

  }
}
