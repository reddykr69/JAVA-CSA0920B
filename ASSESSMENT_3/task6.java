public class HostelWalletTransfer {

    static class WalletAccount {
        final int id;
        double balance;

        WalletAccount(int id, double balance) {
            this.id = id;
            this.balance = balance;
        }
    }

    // DEADLOCKING VERSION: Locks 'from' then 'to'
    public static void transferUnsafe(WalletAccount from, WalletAccount to, double amt) {
        synchronized (from) {
            try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            synchronized (to) {
                if (from.balance >= amt) {
                    from.balance -= amt;
                    to.balance += amt;
                }
            }
        }
    }

    // DEADLOCK-FREE VERSION: Consistent Lock Ordering by Account ID
    public static void transferSafe(WalletAccount from, WalletAccount to, double amt) {
        WalletAccount first = from.id < to.id ? from : to;
        WalletAccount second = from.id < to.id ? to : from;

        synchronized (first) {
            synchronized (second) {
                if (from.balance >= amt) {
                    from.balance -= amt;
                    to.balance += amt;
                }
            }
        }
    }

    public static void main(String[] args) throws InterruptedException {
        WalletAccount a = new WalletAccount(101, 1000.0);
        WalletAccount b = new WalletAccount(102, 1000.0);

        // Pass "deadlock" as command-line arg to reproduce deadlock; default runs safe version
        boolean triggerDeadlock = args.length > 0 && args[0].equalsIgnoreCase("deadlock");

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                if (triggerDeadlock) transferUnsafe(a, b, 1);
                else transferSafe(a, b, 1);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                if (triggerDeadlock) transferUnsafe(b, a, 1);
                else transferSafe(b, a, 1);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        System.out.println("Transfers Complete! Account A=" + a.balance + ", Account B=" + b.balance);
    }
}