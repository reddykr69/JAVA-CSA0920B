import java.util.StringJoiner;

public class TicketGeneratorBenchmark {
    private static final int RUNS = 5;

    public static String buildWithStringConcat(int tickets, int fieldsPerTicket) {
        String lastTicket = "";
        for (int t = 0; t < tickets; t++) {
            String ticket = "=== INDIAN RAILWAYS E-TICKET ===\n";
            for (int f = 0; f < fieldsPerTicket; f++) {
                ticket = ticket + "Field_" + f + ":VAL_" + (t * 100 + f) + "|";
            }
            lastTicket = ticket;
        }
        return lastTicket;
    }

    public static String buildWithStringBuilder(int tickets, int fieldsPerTicket) {
        String lastTicket = "";
        for (int t = 0; t < tickets; t++) {
            StringBuilder sb = new StringBuilder(fieldsPerTicket * 24 + 40);
            sb.append("=== INDIAN RAILWAYS E-TICKET ===\n");
            for (int f = 0; f < fieldsPerTicket; f++) {
                sb.append("Field_").append(f).append(":VAL_").append(t * 100 + f).append("|");
            }
            lastTicket = sb.toString();
        }
        return lastTicket;
    }

    public static String buildWithStringBuffer(int tickets, int fieldsPerTicket) {
        String lastTicket = "";
        for (int t = 0; t < tickets; t++) {
            StringBuffer sbuf = new StringBuffer(fieldsPerTicket * 24 + 40);
            sbuf.append("=== INDIAN RAILWAYS E-TICKET ===\n");
            for (int f = 0; f < fieldsPerTicket; f++) {
                sbuf.append("Field_").append(f).append(":VAL_").append(t * 100 + f).append("|");
            }
            lastTicket = sbuf.toString();
        }
        return lastTicket;
    }

    public static String buildWithStringJoiner(int tickets, int fieldsPerTicket) {
        String lastTicket = "";
        for (int t = 0; t < tickets; t++) {
            StringJoiner sj = new StringJoiner("|", "=== INDIAN RAILWAYS E-TICKET ===\n", "|");
            for (int f = 0; f < fieldsPerTicket; f++) {
                sj.add("Field_" + f + ":VAL_" + (t * 100 + f));
            }
            lastTicket = sj.toString();
        }
        return lastTicket;
    }

    private static long usedMemoryBytes() {
        Runtime rt = Runtime.getRuntime();
        return rt.totalMemory() - rt.freeMemory();
    }

    private static void runWorkload(String label, int tickets, int fields) {
        System.out.println("------------------------------------------------------------------");
        System.out.printf("WORKLOAD: %s (%d tickets x %d fields)%n", label, tickets, fields);
        System.out.println("------------------------------------------------------------------");

        // Warmup
        buildWithStringConcat(Math.min(tickets, 10), Math.min(fields, 10));
        buildWithStringBuilder(Math.min(tickets, 10), Math.min(fields, 10));
        buildWithStringBuffer(Math.min(tickets, 10), Math.min(fields, 10));
        buildWithStringJoiner(Math.min(tickets, 10), Math.min(fields, 10));

        // 1. String (+)
        System.gc();
        long memBeforeConcat = usedMemoryBytes();
        long totalConcatNs = 0;
        String res1 = "";
        for (int r = 0; r < RUNS; r++) {
            long t0 = System.nanoTime();
            res1 = buildWithStringConcat(tickets, fields);
            totalConcatNs += (System.nanoTime() - t0);
        }
        long memAfterConcat = Math.max(0, usedMemoryBytes() - memBeforeConcat);
        long estObjectsConcat = (long) tickets * fields * 2L;

        // 2. StringBuilder
        System.gc();
        long memBeforeSB = usedMemoryBytes();
        long totalSBNs = 0;
        String res2 = "";
        for (int r = 0; r < RUNS; r++) {
            long t0 = System.nanoTime();
            res2 = buildWithStringBuilder(tickets, fields);
            totalSBNs += (System.nanoTime() - t0);
        }
        long memAfterSB = Math.max(0, usedMemoryBytes() - memBeforeSB);
        long estObjectsSB = (long) tickets * 2L;

        // 3. StringBuffer
        System.gc();
        long memBeforeSBuf = usedMemoryBytes();
        long totalSBufNs = 0;
        String res3 = "";
        for (int r = 0; r < RUNS; r++) {
            long t0 = System.nanoTime();
            res3 = buildWithStringBuffer(tickets, fields);
            totalSBufNs += (System.nanoTime() - t0);
        }
        long memAfterSBuf = Math.max(0, usedMemoryBytes() - memBeforeSBuf);
        long estObjectsSBuf = (long) tickets * 2L;

        // 4. StringJoiner (Alternative Concept)
        long totalSJNs = 0;
        String res4 = "";
        for (int r = 0; r < RUNS; r++) {
            long t0 = System.nanoTime();
            res4 = buildWithStringJoiner(tickets, fields);
            totalSJNs += (System.nanoTime() - t0);
        }

        if (res1.length() != res2.length() || res2.length() != res3.length() || res3.length() != res4.length()) {
            throw new IllegalStateException("Output length mismatch!");
        }

        System.out.printf("%-18s | %-14s | %-20s | %-14s%n",
                "Approach", "Avg Time (ms)", "Est. Temp Objects", "Heap Delta (KB)");
        System.out.printf("%-18s | %14.4f | %20d | %14.1f%n",
                "String (+)", (totalConcatNs / (double) RUNS) / 1_000_000.0, estObjectsConcat, memAfterConcat / 1024.0);
        System.out.printf("%-18s | %14.4f | %20d | %14.1f%n",
                "StringBuilder", (totalSBNs / (double) RUNS) / 1_000_000.0, estObjectsSB, memAfterSB / 1024.0);
        System.out.printf("%-18s | %14.4f | %20d | %14.1f%n",
                "StringBuffer", (totalSBufNs / (double) RUNS) / 1_000_000.0, estObjectsSBuf, memAfterSBuf / 1024.0);
        System.out.printf("%-18s | %14.4f | %20d | %14s%n",
                "StringJoiner (Alt)", (totalSJNs / (double) RUNS) / 1_000_000.0, (long) tickets * (fields + 2), "N/A");
    }

    public static void main(String[] args) {
        System.out.println("Q1: RAILWAY E-TICKET GENERATION BENCHMARK (Averaged over " + RUNS + " runs)");
        runWorkload("W1: Single Short Ticket", 1, 6);
        runWorkload("W2: High Ticket Volume", 2000, 50);
        runWorkload("W3: High Field Density", 500, 200);
    }
}