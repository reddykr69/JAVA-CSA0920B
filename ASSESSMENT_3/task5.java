public class CinemaSeatBooking {
    private final boolean[] seats;
    private int bookingsMade = 0;
    private final Object seatLock = new Object();

    public CinemaSeatBooking(int totalSeats) {
        this.seats = new boolean[totalSeats];
    }

    // Version 1: Unsynchronized (Race Condition)
    public boolean bookUnsafe(int seatNo) {
        if (!seats[seatNo]) {
            Thread.yield();
            seats[seatNo] = true;
            bookingsMade++;
            return true;
        }
        return false;
    }

    // Version 2: Synchronized Method
    public synchronized boolean bookSyncMethod(int seatNo) {
        if (!seats[seatNo]) {
            seats[seatNo] = true;
            bookingsMade++;
            return true;
        }
        return false;
    }

    // Version 3: Synchronized Block
    public boolean bookSyncBlock(int seatNo) {
        if (seatNo < 0 || seatNo >= seats.length) return false;
        synchronized (seatLock) {
            if (!seats[seatNo]) {
                seats[seatNo] = true;
                bookingsMade++;
                return true;
            }
            return false;
        }
    }

    public int getBookingsMade() {
        return bookingsMade;
    }

    private static void benchmark(String label, int mode) throws InterruptedException {
        int totalSeats = 10_000;
        CinemaSeatBooking cinema = new CinemaSeatBooking(totalSeats);
        Thread[] threads = new Thread[10];
        int perThread = totalSeats / threads.length;

        long start = System.nanoTime();
        for (int t = 0; t < threads.length; t++) {
            final int startSeat = t * perThread;
            threads[t] = new Thread(() -> {
                for (int i = 0; i < perThread; i++) {
                    int seat = (startSeat + i) % totalSeats;
                    if (mode == 1) cinema.bookUnsafe(seat);
                    else if (mode == 2) cinema.bookSyncMethod(seat);
                    else cinema.bookSyncBlock(seat);
                }
            });
            threads[t].start();
        }
        for (Thread t : threads) t.join();
        double ms = (System.nanoTime() - start) / 1_000_000.0;
        System.out.printf("%-22s | Time: %6.2f ms | Bookings: %d%n",
                          label, ms, cinema.getBookingsMade());
    }

    public static void main(String[] args) throws InterruptedException {
        benchmark("Unsynchronized", 1);
        benchmark("Synchronized Method", 2);
        benchmark("Synchronized Block", 3);
    }
}