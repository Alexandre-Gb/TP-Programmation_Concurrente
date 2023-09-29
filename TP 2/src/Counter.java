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