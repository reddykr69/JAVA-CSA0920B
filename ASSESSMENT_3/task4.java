import java.util.concurrent.atomic.AtomicInteger;

public class ScannerStationDemo {

    // Approach 1: Extending Thread
    static class ScannerStationThread extends Thread {
        private final int scriptsToScan;
        private final AtomicInteger totalScanned;

        public ScannerStationThread(String name, int scriptsToScan, AtomicInteger totalScanned) {
            super(name);
            this.scriptsToScan = scriptsToScan;
            this.totalScanned = totalScanned;
        }

        @Override
        public void run() {
            for (int i = 1; i <= scriptsToScan; i++) {
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                totalScanned.incrementAndGet();
            }
            System.out.println(getName() + " (extends Thread) FINISHED.");
        }
    }

    // Approach 2: Implementing Runnable (Preferred)
    static class ScannerStationTask implements Runnable {
        private final String stationName;
        private final int scriptsToScan;
        private final AtomicInteger totalScanned;

        public ScannerStationTask(String stationName, int scriptsToScan, AtomicInteger totalScanned) {
            this.stationName = stationName;
            this.scriptsToScan = scriptsToScan;
            this.totalScanned = totalScanned;
        }

        @Override
        public void run() {
            for (int i = 1; i <= scriptsToScan; i++) {
                try {
                    Thread.sleep(40);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                totalScanned.incrementAndGet();
            }
            System.out.println(stationName + " (Runnable on " + Thread.currentThread().getName() + ") FINISHED.");
        }
    }

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Part 1: Extending Thread ===");
        AtomicInteger total1 = new AtomicInteger(0);
        Thread[] tStations = new Thread[4];
        for (int i = 0; i < 4; i++) {
            tStations[i] = new ScannerStationThread("Station-" + (i + 1), 10, total1);
            tStations[i].start();
        }
        for (Thread t : tStations) t.join();
        System.out.println("Total Scanned (Thread): " + total1.get());

        System.out.println("\n=== Part 2: Implementing Runnable + Life Cycle ===");
        AtomicInteger total2 = new AtomicInteger(0);
        Thread[] rStations = new Thread[4];
        for (int i = 0; i < 4; i++) {
            rStations[i] = new Thread(
                new ScannerStationTask("Station-" + (i + 1), 10, total2),
                "ScannerThread-" + (i + 1)
            );
            System.out.println(rStations[i].getName() + " before start -> state=" + rStations[i].getState());
            rStations[i].start();
            System.out.println(rStations[i].getName() + " after start  -> isAlive=" + rStations[i].isAlive());
        }
        for (Thread t : rStations) {
            t.join();
            System.out.println(t.getName() + " after join   -> state=" + t.getState() + ", isAlive=" + t.isAlive());
        }
        System.out.println("Total Scanned (Runnable): " + total2.get());
    }
}