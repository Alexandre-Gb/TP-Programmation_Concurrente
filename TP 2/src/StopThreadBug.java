public class StopThreadBug implements Runnable {
  private boolean stop = false;

  public void stop() {
    stop = true;
  }

  @Override
  public void run() {
    while (!stop) {
      System.out.println("Up");
    }
    System.out.print("Done");
  }

  public static void main(String[] args) throws InterruptedException {
    var stopThreadBug = new StopThreadBug();
    Thread.ofPlatform().start(stopThreadBug);
    Thread.sleep(1_000);
    System.out.println("Trying to tell the thread to stop");
    stopThreadBug.stop();
  }
}