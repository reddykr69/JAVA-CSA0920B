import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;

class Medicine {
    final String code;
    final String name;
    final int stock;
    final int insertionSeq;

    Medicine(String code, String name, int stock, int insertionSeq) {
        this.code = code;
        this.name = name;
        this.stock = stock;
        this.insertionSeq = insertionSeq;
    }
}

public class PharmacyInventoryBenchmark {
    private static final int RUNS = 5;
    private static final int LOOKUPS = 100_000;

    private static void testInventorySize(int n) {
        // Generate N unique medicine codes in shuffled insertion order
        List<String> insertionKeys = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            insertionKeys.add(String.format("MED-%06d", i + 1));
        }
        Collections.shuffle(insertionKeys, new Random(42));

        String[] lookupTargets = new String[LOOKUPS];
        Random rng = new Random(101);
        for (int i = 0; i < LOOKUPS; i++) {
            lookupTargets[i] = insertionKeys.get(rng.nextInt(n));
        }

        long hmPut = 0, tmPut = 0, lhmPut = 0;
        long hmGet = 0, tmGet = 0, lhmGet = 0, bsGet = 0;
        long hmCodeRep = 0, tmCodeRep = 0, lhmCodeRep = 0, bsCodeRep = 0;
        long hmInsRep = 0, tmInsRep = 0, lhmInsRep = 0;

        for (int r = 0; r < RUNS; r++) {
            // 1. Populate maps
            Map<String, Medicine> hm = new HashMap<>(n * 2);
            long t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                String k = insertionKeys.get(i);
                hm.put(k, new Medicine(k, "Drug_" + i, (i % 500) + 10, i));
            }
            hmPut += (System.nanoTime() - t0);

            Map<String, Medicine> tm = new TreeMap<>();
            t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                String k = insertionKeys.get(i);
                tm.put(k, new Medicine(k, "Drug_" + i, (i % 500) + 10, i));
            }
            tmPut += (System.nanoTime() - t0);

            Map<String, Medicine> lhm = new LinkedHashMap<>(n * 2);
            t0 = System.nanoTime();
            for (int i = 0; i < n; i++) {
                String k = insertionKeys.get(i);
                lhm.put(k, new Medicine(k, "Drug_" + i, (i % 500) + 10, i));
            }
            lhmPut += (System.nanoTime() - t0);

            // Sorted Array for Alternative Concept
            Medicine[] sortedArr = new Medicine[n];
            for (int i = 0; i < n; i++) {
                String k = insertionKeys.get(i);
                sortedArr[i] = new Medicine(k, "Drug_" + i, (i % 500) + 10, i);
            }
            Arrays.sort(sortedArr, Comparator.comparing(m -> m.code));
            Comparator<Medicine> byCode = Comparator.comparing(m -> m.code);

            // 2. 100,000 Lookups
            long sink = 0;
            t0 = System.nanoTime();
            for (int i = 0; i < LOOKUPS; i++) sink += hm.get(lookupTargets[i]).stock;
            hmGet += (System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (int i = 0; i < LOOKUPS; i++) sink += tm.get(lookupTargets[i]).stock;
            tmGet += (System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (int i = 0; i < LOOKUPS; i++) sink += lhm.get(lookupTargets[i]).stock;
            lhmGet += (System.nanoTime() - t0);

            t0 = System.nanoTime();
            for (int i = 0; i < LOOKUPS; i++) {
                int idx = Arrays.binarySearch(sortedArr, new Medicine(lookupTargets[i], "", 0, 0), byCode);
                sink += sortedArr[idx].stock;
            }
            bsGet += (System.nanoTime() - t0);

            // 3. Report A: Code Order Report
            // HashMap -> requires sorting entries by key
            t0 = System.nanoTime();
            List<Medicine> hmCodeList = new ArrayList<>(hm.values());
            hmCodeList.sort(byCode);
            for (Medicine m : hmCodeList) sink += m.stock;
            hmCodeRep += (System.nanoTime() - t0);

            // TreeMap -> natural iteration is already in code order!
            t0 = System.nanoTime();
            for (Medicine m : tm.values()) sink += m.stock;
            tmCodeRep += (System.nanoTime() - t0);

            // LinkedHashMap -> requires sorting entries by key
            t0 = System.nanoTime();
            List<Medicine> lhmCodeList = new ArrayList<>(lhm.values());
            lhmCodeList.sort(byCode);
            for (Medicine m : lhmCodeList) sink += m.stock;
            lhmCodeRep += (System.nanoTime() - t0);

            // Sorted Array -> natural iteration
            t0 = System.nanoTime();
            for (Medicine m : sortedArr) sink += m.stock;
            bsCodeRep += (System.nanoTime() - t0);

            // 4. Report B: Insertion Order Report
            Comparator<Medicine> byIns = Comparator.comparingInt(m -> m.insertionSeq);
            // HashMap -> requires sorting by insertionSeq
            t0 = System.nanoTime();
            List<Medicine> hmInsList = new ArrayList<>(hm.values());
            hmInsList.sort(byIns);
            for (Medicine m : hmInsList) sink += m.stock;
            hmInsRep += (System.nanoTime() - t0);

            // TreeMap -> requires sorting by insertionSeq
            t0 = System.nanoTime();
            List<Medicine> tmInsList = new ArrayList<>(tm.values());
            tmInsList.sort(byIns);
            for (Medicine m : tmInsList) sink += m.stock;
            tmInsRep += (System.nanoTime() - t0);

            // LinkedHashMap -> natural iteration is already in insertion order!
            t0 = System.nanoTime();
            for (Medicine m : lhm.values()) sink += m.stock;
            lhmInsRep += (System.nanoTime() - t0);

            if (sink == 42) System.out.print("");
        }

        System.out.println("======================================================================");
        System.out.printf("PHARMACY INVENTORY SIZE N = %,d MEDICINES (Averaged over %d runs)%n", n, RUNS);
        System.out.println("======================================================================");
        System.out.printf("%-17s | %-9s | %-11s | %-11s | %-11s%n",
                "Structure", "Put (ms)", "100k Get", "Code Rep", "Ins Rep");
        System.out.printf("%-17s | %9.3f | %11.3f | %11.3f | %11.3f%n",
                "HashMap", hmPut / (RUNS * 1e6), hmGet / (RUNS * 1e6), hmCodeRep / (RUNS * 1e6), hmInsRep / (RUNS * 1e6));
        System.out.printf("%-17s | %9.3f | %11.3f | %11.3f | %11.3f%n",
                "TreeMap", tmPut / (RUNS * 1e6), tmGet / (RUNS * 1e6), tmCodeRep / (RUNS * 1e6), tmInsRep / (RUNS * 1e6));
        System.out.printf("%-17s | %9.3f | %11.3f | %11.3f | %11.3f%n",
                "LinkedHashMap", lhmPut / (RUNS * 1e6), lhmGet / (RUNS * 1e6), lhmCodeRep / (RUNS * 1e6), lhmInsRep / (RUNS * 1e6));
        System.out.printf("%-17s | %9s | %11.3f | %11.3f | %11s%n",
                "Sorted Array (BS)", "N/A", bsGet / (RUNS * 1e6), bsCodeRep / (RUNS * 1e6), "N/A");
    }

    public static void main(String[] args) {
        testInventorySize(1_000);
        testInventorySize(10_000);
        testInventorySize(50_000);
    }
}