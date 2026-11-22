import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

public class SemesterResultProcessor {

    static class StudentResult {
        final String studentId;
        final double average;
        final String grade;

        StudentResult(String id, double avg, String grade) {
            this.studentId = id;
            this.average = avg;
            this.grade = grade;
        }
    }

    static class FileOutcome {
        final boolean success;
        final String fileName;
        final StudentResult result;
        final String errorReason;

        FileOutcome(String fileName, StudentResult result) {
            this.success = true;
            this.fileName = fileName;
            this.result = result;
            this.errorReason = null;
        }

        FileOutcome(String fileName, String errorReason) {
            this.success = false;
            this.fileName = fileName;
            this.result = null;
            this.errorReason = errorReason;
        }
    }

    static class ScoreFileTask implements Callable<FileOutcome> {
        private final Path filePath;

        ScoreFileTask(Path filePath) {
            this.filePath = filePath;
        }

        @Override
        public FileOutcome call() {
            String name = filePath.getFileName().toString();
            try {
                List<String> lines = Files.readAllLines(filePath);
                if (lines.isEmpty()) {
                    return new FileOutcome(name, "Empty file");
                }
                String[] parts = lines.get(0).split(",");
                if (parts.length < 4) {
                    return new FileOutcome(name, "Malformed columns");
                }

                String studentId = parts[0].trim();
                double sum = 0;
                for (int i = 1; i <= 3; i++) {
                    double score = Double.parseDouble(parts[i].trim());
                    if (score < 0 || score > 100) {
                        return new FileOutcome(name, "Score out of range: " + score);
                    }
                    sum += score;
                }

                double avg = sum / 3.0;
                String grade = avg >= 90 ? "S"
                             : avg >= 80 ? "A"
                             : avg >= 70 ? "B"
                             : avg >= 50 ? "C" : "F";
                return new FileOutcome(name, new StudentResult(studentId, avg, grade));

            } catch (NoSuchFileException e) {
                return new FileOutcome(name, "Missing file");
            } catch (NumberFormatException e) {
                return new FileOutcome(name, "Corrupt numeric score");
            } catch (Exception e) {
                return new FileOutcome(name, "Unexpected error: " + e.getMessage());
            }
        }
    }

    public static long runBenchmark(List<Path> files, int poolSize) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        ConcurrentHashMap<String, StudentResult> validResults = new ConcurrentHashMap<>();
        ConcurrentLinkedQueue<String> failures = new ConcurrentLinkedQueue<>();

        long start = System.nanoTime();
        List<Future<FileOutcome>> futures = new ArrayList<>(files.size());

        for (Path p : files) {
            futures.add(executor.submit(new ScoreFileTask(p)));
        }

        for (Future<FileOutcome> f : futures) {
            try {
                FileOutcome out = f.get();
                if (out.success) {
                    validResults.put(out.result.studentId, out.result);
                } else {
                    failures.add(out.fileName + " -> " + out.errorReason);
                }
            } catch (ExecutionException e) {
                failures.add("Task execution error: " + e.getCause());
            }
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.MINUTES);
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        System.out.printf("Pool=%d | Time=%4d ms | Valid=%d | Failed=%d | Total=%d%n",
                poolSize, elapsedMs, validResults.size(), failures.size(),
                validResults.size() + failures.size());
        return elapsedMs;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get("score_files");
        Files.createDirectories(dir);
        List<Path> files = new ArrayList<>();
        int totalFiles = 10_000;

        System.out.println("Generating 10,000 test score files (9,800 valid, 100 corrupt, 100 missing)...");
        for (int i = 1; i <= totalFiles; i++) {
            Path p = dir.resolve("student_" + i + ".txt");
            files.add(p);
            if (i % 100 == 0) {
                Files.deleteIfExists(p); // 100 missing files
            } else if (i % 100 == 50) {
                Files.writeString(p, "STU" + i + ",85,CORRUPT,90"); // 100 corrupt files
            } else {
                Files.writeString(p, "STU" + i + ",82,78,91"); // 9,800 valid files
            }
        }

        System.out.println("Running benchmarks:");
        for (int threads : new int[]{1, 2, 4, 8}) {
            runBenchmark(files, threads);
        }
    }
}