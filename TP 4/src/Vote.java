import java.util.HashMap;
import java.util.Map;

public class Vote {
  private final int maxVotes;
  private int currentVotes = 0;
  private final Map<String, Integer> votes = new HashMap<>();
  private final Object lock = new Object();

  public Vote(int maxVotes) {
    if (maxVotes < 1) throw new IllegalArgumentException("maxVotes must be > 0");
    this.maxVotes = maxVotes;
  }

  public String vote(String candidate) throws InterruptedException {
    synchronized (lock) {
      votes.merge(candidate, 1, Integer::sum);
      currentVotes++;

      if (currentVotes >= maxVotes) {
        lock.notifyAll();
      }

      return computeWinner();
    }
  }

  private String computeWinner() {
    var score = -1;
    String winner = null;
    for (var e : votes.entrySet()) {
      var key = e.getKey();
      var value = e.getValue();
      if (value > score || (value == score && key.compareTo(winner) < 0)) {
        winner = key;
        score = value;
      }
    }
    return winner;
  }


//  private String computeWinner() throws InterruptedException {
//    synchronized (lock) {
//      while (currentVotes < maxVotes) {
//        lock.wait();
//      }
//
//      return votes.entrySet().stream()
//        .max(Map.Entry.comparingByValue())
//        .map(Map.Entry::getKey)
//        .orElse(null);
//    }
//  }

  public static void main(String[] args) throws InterruptedException {
    var vote = new Vote(4);
    Thread.ofPlatform().start(() -> {
      try {
        Thread.sleep(2_000);
        System.out.println("The winner is " + vote.vote("un"));
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    });
    Thread.ofPlatform().start(() -> {
      try {
        Thread.sleep(1_500);
        System.out.println("The winner is " + vote.vote("zero"));
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    });
    Thread.ofPlatform().start(() -> {
      try {
        Thread.sleep(1_000);
        System.out.println("The winner is " + vote.vote("un"));
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    });
    System.out.println("The winner is " + vote.vote("zero"));
  }
}
