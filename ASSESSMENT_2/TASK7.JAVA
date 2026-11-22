import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

class RankedTeam {
    final String name;
    final int points;
    final int goalDiff;

    RankedTeam(String name, int points, int goalDiff) {
        this.name = name;
        this.points = points;
        this.goalDiff = goalDiff;
    }

    @Override
    public String toString() {
        return name + "(" + points + "," + goalDiff + ")";
    }
}

// ORIGINAL SOLUTION FROM Q5:
// Every call to ranking() allocates a new ArrayList(N) and runs a full O(N log N) TimSort.
class OriginalLeaderboard<T> {
    private final List<T> items = new ArrayList<>();
    private final Comparator<? super T> order;

    OriginalLeaderboard(Comparator<? super T> order) {
        this.order = order;
    }

    void addOrUpdate(T item) {
        items.add(item);
    }

    List<T> ranking() {
        List<T> copy = new ArrayList<>(items);
        copy.sort(order);
        return copy;
    }
}

// REDESIGNED SOLUTION FOR Q7:
// Uses a TreeSet<T> (Red-Black Tree) + HashMap<String, T> key index + lazy cached snapshot.
// - Updates remove old entry in O(log N) and insert new entry in O(log N) without duplicates.
// - Repeated ranking() calls return an unmodifiable cached snapshot in O(1) time and 0 extra allocations
//   when no mutation has occurred, or O(N) linear tree traversal when dirty (completely eliminating O(N log N) sorting).
class RedesignedLeaderboard {
    private final Comparator<RankedTeam> order;
    private final TreeSet<RankedTeam> sortedSet;
    private final Map<String, RankedTeam> teamIndex = new HashMap<>();
    private List<RankedTeam> cachedRanking = null;

    RedesignedLeaderboard(Comparator<RankedTeam> order) {
        this.order = order;
        this.sortedSet = new TreeSet<>(order);
    }

    void addOrUpdate(RankedTeam updatedTeam) {
        RankedTeam existing = teamIndex.put(updatedTeam.name, updatedTeam);
        if (existing != null) {
            sortedSet.remove(existing); // O(log N)
        }
        sortedSet.add(updatedTeam);     // O(log N)
        cachedRanking = null;           // Invalidate snapshot cache
    }

    List<RankedTeam> ranking() {
        if (cachedRanking == null) {
            cachedRanking = Collections.unmodifiableList(new ArrayList<>(sortedSet));
        }
        return cachedRanking;
    }

    List<RankedTeam> topK(int k) {
        List<RankedTeam> top = new ArrayList<>(k);
        int count = 0;
        for (RankedTeam t : sortedSet) {
            if (count++ >= k) break;
            top.add(t);
        }
        return top;
    }
}

public class DesignChallengeQ7 {
    private static final int RUNS = 5;
    private static long comparisonCount = 0;

    public static void main(String[] args) {
        Comparator<RankedTeam> countedComp = (a, b) -> {
            comparisonCount++;
            int c1 = Integer.compare(b.points, a.points);
            if (c1 != 0) return c1;
            int c2 = Integer.compare(b.goalDiff, a.goalDiff);
            if (c2 != 0) return c2;
            return a.name.compareTo(b.name);
        };

        System.out.println("=== Q7 DAY-4 DESIGN CHALLENGE: ORIGINAL vs. REDESIGNED LEADERBOARD ===");
        System.out.println("Workload: N teams loaded, followed by 500 live score updates interleaved with 2,000 ranking() reads");
        System.out.println("------------------------------------------------------------------------");
        System.out.printf("%-8s | %-10s | %-10s | %-13s | %-11s | %-7s%n",
                "Size (N)", "Orig (ms)", "Redes (ms)", "Orig Comps", "Redes Comps", "Speedup");
        System.out.println("------------------------------------------------------------------------");

        int[] sizes = {1_000, 5_000, 20_000};
        for (int n : sizes) {
            long origTotalNs = 0;
            long origComps = 0;
            long redTotalNs = 0;
            long redComps = 0;

            for (int r = 0; r < RUNS; r++) {
                // 1. Measure Original Solution
                comparisonCount = 0;
                OriginalLeaderboard<RankedTeam> orig = new OriginalLeaderboard<>(countedComp);
                for (int i = 0; i < n; i++) {
                    orig.addOrUpdate(new RankedTeam("Team_" + i, i % 80, (i % 50) - 25));
                }
                long t0 = System.nanoTime();
                long sink = 0;
                for (int step = 0; step < 2000; step++) {
                    if (step % 4 == 0) {
                        orig.addOrUpdate(new RankedTeam("Team_" + (step % n), (step * 7) % 100, step % 30));
                    }
                    List<RankedTeam> rank = orig.ranking();
                    sink += rank.get(0).points;
                }
                origTotalNs += (System.nanoTime() - t0);
                origComps += comparisonCount;

                // 2. Measure Redesigned Solution
                comparisonCount = 0;
                RedesignedLeaderboard red = new RedesignedLeaderboard(countedComp);
                for (int i = 0; i < n; i++) {
                    red.addOrUpdate(new RankedTeam("Team_" + i, i % 80, (i % 50) - 25));
                }
                t0 = System.nanoTime();
                for (int step = 0; step < 2000; step++) {
                    if (step % 4 == 0) {
                        red.addOrUpdate(new RankedTeam("Team_" + (step % n), (step * 7) % 100, step % 30));
                    }
                    List<RankedTeam> rank = red.ranking();
                    sink += rank.get(0).points;
                }
                redTotalNs += (System.nanoTime() - t0);
                redComps += comparisonCount;

                if (sink == 42) System.out.print("");
            }

            double origMs = origTotalNs / (RUNS * 1e6);
            double redMs = redTotalNs / (RUNS * 1e6);
            long avgOrigComps = origComps / RUNS;
            long avgRedComps = redComps / RUNS;
            double speedup = origMs / redMs;

            System.out.printf("%,8d | %10.2f | %10.2f | %,13d | %,11d | %6.1fx%n",
                    n, origMs, redMs, avgOrigComps, avgRedComps, speedup);
        }
    }
}
