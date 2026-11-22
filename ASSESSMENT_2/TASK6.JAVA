import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Random;

class TriagePatient {
    final String name;
    final int severity;
    final long arrivalTick;

    TriagePatient(String name, int severity, long arrivalTick) {
        this.name = name;
        this.severity = severity;
        this.arrivalTick = arrivalTick;
    }

    int effectiveSeverity(long currentTick, long agingThreshold) {
        long waited = Math.max(0, currentTick - arrivalTick);
        int boost = (int) (waited / agingThreshold);
        return Math.max(1, severity - boost);
    }

    @Override
    public String toString() {
        return name + "(S" + severity + ",t=" + arrivalTick + ")";
    }
}

public class EmergencyTriageEvaluation {
    private static final int RUNS = 5;

    public static void main(String[] args) {
        System.out.println("=== 1. CORRECTNESS CHECK (8 Patients Hand-Verified Instance) ===");
        Comparator<TriagePatient> comp = Comparator
                .comparingInt((TriagePatient p) -> p.severity)
                .thenComparingLong(p -> p.arrivalTick);
        PriorityQueue<TriagePatient> pq = new PriorityQueue<>(comp);

        String[] names = {"P1_Ravi", "P2_Meena", "P3_John", "P4_Asha", "P5_Kumar", "P6_Lata", "P7_Dev", "P8_Sita"};
        int[] sevs     = {3,         1,          4,         5,         1,          2,         2,        1};
        for (int i = 0; i < names.length; i++) {
            pq.add(new TriagePatient(names[i], sevs[i], i + 1));
        }

        System.out.println("Direct PriorityQueue.toString() (Internal Heap Array Level-Order - NOT Sorted!):");
        System.out.println("  " + pq);
        System.out.println("Actual Treatment Order via repeated poll():");
        StringBuilder sb = new StringBuilder("  ");
        while (!pq.isEmpty()) {
            sb.append(pq.poll()).append(" -> ");
        }
        System.out.println(sb.substring(0, sb.length() - 4));

        System.out.println("\n=== 2. GROWTH INVESTIGATION & ALTERNATIVE CONCEPTS ===");
        int[] sizes = {1_000, 10_000, 100_000};
        for (int n : sizes) {
            Random rng = new Random(42);
            int[] randomSevs = new int[n];
            for (int i = 0; i < n; i++) randomSevs[i] = rng.nextInt(5) + 1;

            long pqAdmit = 0, pqPoll = 0;
            long buckAdmit = 0, buckPoll = 0;
            long llAdmit = 0, llPoll = 0;

            for (int r = 0; r < RUNS; r++) {
                PriorityQueue<TriagePatient> q1 = new PriorityQueue<>(n, comp);
                long t0 = System.nanoTime();
                for (int i = 0; i < n; i++) q1.add(new TriagePatient("P", randomSevs[i], i));
                pqAdmit += (System.nanoTime() - t0);

                t0 = System.nanoTime();
                while (!q1.isEmpty()) q1.poll();
                pqPoll += (System.nanoTime() - t0);

                @SuppressWarnings("unchecked")
                Queue<TriagePatient>[] buckets = new ArrayDeque[6];
                for (int s = 1; s <= 5; s++) buckets[s] = new ArrayDeque<>(n / 5 + 16);
                t0 = System.nanoTime();
                for (int i = 0; i < n; i++) {
                    int s = randomSevs[i];
                    buckets[s].add(new TriagePatient("P", s, i));
                }
                buckAdmit += (System.nanoTime() - t0);

                t0 = System.nanoTime();
                int served = 0;
                while (served < n) {
                    for (int s = 1; s <= 5; s++) {
                        if (!buckets[s].isEmpty()) {
                            buckets[s].poll();
                            served++;
                            break;
                        }
                    }
                }
                buckPoll += (System.nanoTime() - t0);

                if (n <= 10_000) {
                    LinkedList<TriagePatient> sortedLL = new LinkedList<>();
                    t0 = System.nanoTime();
                    for (int i = 0; i < n; i++) {
                        TriagePatient np = new TriagePatient("P", randomSevs[i], i);
                        ListIterator<TriagePatient> it = sortedLL.listIterator();
                        boolean inserted = false;
                        while (it.hasNext()) {
                            if (comp.compare(np, it.next()) < 0) {
                                it.previous();
                                it.add(np);
                                inserted = true;
                                break;
                            }
                        }
                        if (!inserted) sortedLL.addLast(np);
                    }
                    llAdmit += (System.nanoTime() - t0);

                    t0 = System.nanoTime();
                    while (!sortedLL.isEmpty()) sortedLL.pollFirst();
                    llPoll += (System.nanoTime() - t0);
                }
            }

            System.out.printf("N=%,7d | PQ(in/out): %5.2f/%5.2f ms | 5-Bkt: %5.2f/%5.2f ms",
                    n, pqAdmit / (RUNS * 1e6), pqPoll / (RUNS * 1e6), buckAdmit / (RUNS * 1e6), buckPoll / (RUNS * 1e6));
            if (n <= 10_000) {
                System.out.printf(" | LL: %6.2f/%4.2f ms%n",
                        llAdmit / (RUNS * 1e6), llPoll / (RUNS * 1e6));
            } else {
                System.out.println(" | LL: O(N^2) skip");
            }
        }

        System.out.println("\n=== 3. STARVATION & FAIR AGING RULE VERIFICATION ===");
        TriagePatient ashaS5 = new TriagePatient("Asha", 5, 0);
        long threshold = 3;
        for (long tick = 0; tick <= 12; tick += 3) {
            System.out.printf("At tick=%2d: Asha(base S5, arrived t=0) -> Effective Severity = S%d%n",
                    tick, ashaS5.effectiveSeverity(tick, threshold));
        }
    }
}
