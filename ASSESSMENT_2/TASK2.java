import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

class Song {
    final int id;
    final String title;
    Song(int id, String title) {
        this.id = id;
        this.title = title;
    }
}

public class PlaylistBenchmark {
    private static final int RUNS = 5;
    private static final int MUTATION_OPS = 500;
    private static final int RANDOM_READS = 1000;

    private static void benchmarkSize(int n) {
        long alAddEnd = 0, llAddEnd = 0;
        long alInsFront = 0, llInsFront = 0;
        long alInsMid = 0, llInsMid = 0;
        long alRemFront = 0, llRemFront = 0;
        long alGetK = 0, llGetK = 0;
        long alIter = 0, llIter = 0;

        for (int r = 0; r < RUNS; r++) {
            // 1. Add at end (N elements)
            List<Song> al = new ArrayList<>();
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) al.add(new Song(i, "Track_" + i));
            alAddEnd += (System.nanoTime() - t0);

            List<Song> ll = new LinkedList<>();
            t0 = System.nanoTime();
            for (int i = 0; i < n; i++) ll.add(new Song(i, "Track_" + i));
            llAddEnd += (System.nanoTime() - t0);

            // 2. Insert at front (MUTATION_OPS times)
            t0 = System.nanoTime();
            for (int i = 0; i < MUTATION_OPS; i++) al.add(0, new Song(-i, "Front_" + i));
            alInsFront += (System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (int i = 0; i < MUTATION_OPS; i++) ll.add(0, new Song(-i, "Front_" + i));
            llInsFront += (System.nanoTime() - t0);

            // 3. Insert in middle (MUTATION_OPS times)
            t0 = System.nanoTime();
            for (int i = 0; i < MUTATION_OPS; i++) al.add(al.size() / 2, new Song(i, "Mid_" + i));
            alInsMid += (System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (int i = 0; i < MUTATION_OPS; i++) ll.add(ll.size() / 2, new Song(i, "Mid_" + i));
            llInsMid += (System.nanoTime() - t0);

            // 4. Remove from front (MUTATION_OPS times)
            t0 = System.nanoTime();
            for (int i = 0; i < MUTATION_OPS; i++) al.remove(0);
            alRemFront += (System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (int i = 0; i < MUTATION_OPS; i++) ll.remove(0);
            llRemFront += (System.nanoTime() - t0);

            // 5. Random access get(k) (RANDOM_READS times)
            long sink = 0;
            t0 = System.nanoTime();
            for (int i = 0; i < RANDOM_READS; i++) {
                int idx = (i * 997) % al.size();
                sink += al.get(idx).id;
            }
            alGetK += (System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (int i = 0; i < RANDOM_READS; i++) {
                int idx = (i * 997) % ll.size();
                sink += ll.get(idx).id;
            }
            llGetK += (System.nanoTime() - t0);

            // 6. Iterate through whole list
            t0 = System.nanoTime();
            for (Song s : al) sink += s.id;
            alIter += (System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (Song s : ll) sink += s.id;
            llIter += (System.nanoTime() - t0);

            if (sink == 42) System.out.print("");
        }

        System.out.println("==================================================================");
        System.out.printf("PLAYLIST SIZE N = %,d (Averaged over %d runs)%n", n, RUNS);
        System.out.println("==================================================================");
        System.out.printf("%-28s | %-15s | %-15s%n", "Operation", "ArrayList (ms)", "LinkedList (ms)");
        System.out.printf("%-28s | %15.4f | %15.4f%n", "Add at End (N items)", alAddEnd / (RUNS * 1e6), llAddEnd / (RUNS * 1e6));
        System.out.printf("%-28s | %15.4f | %15.4f%n", "Insert at Front (500 ops)", alInsFront / (RUNS * 1e6), llInsFront / (RUNS * 1e6));
        System.out.printf("%-28s | %15.4f | %15.4f%n", "Insert at Middle (500 ops)", alInsMid / (RUNS * 1e6), llInsMid / (RUNS * 1e6));
        System.out.printf("%-28s | %15.4f | %15.4f%n", "Remove at Front (500 ops)", alRemFront / (RUNS * 1e6), llRemFront / (RUNS * 1e6));
        System.out.printf("%-28s | %15.4f | %15.4f%n", "Access k-th (1,000 queries)", alGetK / (RUNS * 1e6), llGetK / (RUNS * 1e6));
        System.out.printf("%-28s | %15.4f | %15.4f%n", "Iterate Full List (N items)", alIter / (RUNS * 1e6), llIter / (RUNS * 1e6));
    }

    private static void benchmarkUpNextQueue(int ops) {
        long alTime = 0, llTime = 0, adTime = 0;
        for (int r = 0; r < RUNS; r++) {
            List<Song> al = new ArrayList<>();
            long t0 = System.nanoTime();
            for (int i = 0; i < ops; i++) {
                if (i % 2 == 0) al.add(0, new Song(i, "Urgent_" + i));
                else al.add(new Song(i, "Normal_" + i));
            }
            for (int i = 0; i < ops / 2; i++) al.remove(0);
            alTime += (System.nanoTime() - t0);

            Deque<Song> ll = new LinkedList<>();
            t0 = System.nanoTime();
            for (int i = 0; i < ops; i++) {
                if (i % 2 == 0) ll.addFirst(new Song(i, "Urgent_" + i));
                else ll.addLast(new Song(i, "Normal_" + i));
            }
            for (int i = 0; i < ops / 2; i++) ll.pollFirst();
            llTime += (System.nanoTime() - t0);

            Deque<Song> ad = new ArrayDeque<>();
            t0 = System.nanoTime();
            for (int i = 0; i < ops; i++) {
                if (i % 2 == 0) ad.addFirst(new Song(i, "Urgent_" + i));
                else ad.addLast(new Song(i, "Normal_" + i));
            }
            for (int i = 0; i < ops / 2; i++) ad.pollFirst();
            adTime += (System.nanoTime() - t0);
        }
        System.out.println("==================================================================");
        System.out.printf("CHANGED CONDITION: 'UP NEXT' QUEUE (%,d addFirst/addLast + %,d pollFirst)%n", ops, ops / 2);
        System.out.println("==================================================================");
        System.out.printf("ArrayList  : %.4f ms%n", alTime / (RUNS * 1e6));
        System.out.printf("LinkedList : %.4f ms%n", llTime / (RUNS * 1e6));
        System.out.printf("ArrayDeque : %.4f ms%n", adTime / (RUNS * 1e6));
    }

    public static void main(String[] args) {
        benchmarkSize(2_000);
        benchmarkSize(10_000);
        benchmarkSize(50_000);
        benchmarkUpNextQueue(10_000);
    }
}
