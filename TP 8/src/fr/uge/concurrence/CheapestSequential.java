package fr.uge.concurrence;

import java.util.ArrayList;
import java.util.Objects;
import java.util.Optional;

public class CheapestSequential {

    private final String item;
    private final int timeoutMilliPerRequest;

    public CheapestSequential(String item, int timeoutMilliPerRequest) {
        Objects.requireNonNull(item);
        if (timeoutMilliPerRequest <= 0) { throw new IllegalArgumentException(); }

        this.item = item;
        this.timeoutMilliPerRequest = timeoutMilliPerRequest;
    }

    /**
     * @return the cheapest price for item if it is sold
     */
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

    public static void main(String[] args) throws InterruptedException {
        var agregator = new CheapestSequential("pikachu", 2_000);
        var answer = agregator.retrieve();
        System.out.println(answer); // Optional[pikachu@darty.fr : 214]
    }
}
