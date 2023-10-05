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

  public String firstName() {
    synchronized (lock) {
      return firstName;
    }
  }
  public String lastName() {
    synchronized (lock) {
      return lastName;
    }
  }
  
  @Override
  public String toString() {
    synchronized(lock) {
      return firstName + ' ' + lastName;
    }
  }
  
  public static void main(String[] args) {
    var board = new HonorBoard();
    Thread.ofPlatform().start(() -> {
      for(;;) {
        board.set("Mickey", "Mouse");
      }
    });
    
    Thread.ofPlatform().start(() -> {
      for(;;) {
        board.set("Donald", "Duck");
      }
    });
    
    Thread.ofPlatform().start(() -> {
      for(;;) {
        System.out.println(board);
        // System.out.println(board.firstName() + ' ' + board.lastName());
      }
    });
  }
}
