import java.io.IOException;

public class OnlineExamPortal {

    // Base Checked Exception
    static class ExamException extends Exception {
        public ExamException(String message) { super(message); }
        public ExamException(String message, Throwable cause) { super(message, cause); }
    }

    // Specific Checked Exceptions
    static class ExamWindowClosedException extends ExamException {
        public ExamWindowClosedException(String msg) { super(msg); }
    }

    static class SubmissionStorageException extends ExamException {
        public SubmissionStorageException(String msg, Throwable cause) { super(msg, cause); }
    }

    // Unchecked Exception
    static class StudentNotRegisteredException extends RuntimeException {
        public StudentNotRegisteredException(String msg) { super(msg); }
    }

    // Layer 3: Storage Layer
    static class ExamStorage {
        public void saveFile(String fileName, long sizeKb) throws SubmissionStorageException {
            try {
                if (sizeKb > 500) {
                    throw new IOException("Disk quota exceeded (" + sizeKb + " KB > 500 KB)");
                }
                System.out.println("[Storage] Saved: " + fileName);
            } catch (IOException e) {
                throw new SubmissionStorageException("Could not store " + fileName, e);
            }
        }
    }

    // Layer 2: Service Layer
    static class ExamService {
        private final ExamStorage storage = new ExamStorage();

        public void submitAnswer(String studentId, boolean registered,
                                 boolean windowOpen, String file, long sizeKb)
                throws ExamException {
            if (!registered) {
                throw new StudentNotRegisteredException("Student " + studentId + " is not enrolled.");
            }
            if (!windowOpen) {
                throw new ExamWindowClosedException("Submission window has already closed.");
            }
            storage.saveFile(file, sizeKb);
        }
    }

    // Layer 1: Controller Layer
    private final ExamService service = new ExamService();

    public void handleRequest(String studentId, boolean registered,
                              boolean windowOpen, String file, long sizeKb) {
        try {
            service.submitAnswer(studentId, registered, windowOpen, file, sizeKb);
            System.out.println("[UI] Submission accepted for " + studentId);
        } catch (ExamWindowClosedException e) {
            System.out.println("[UI] Rejected: " + e.getMessage());
        } catch (SubmissionStorageException e) {
            System.out.println("[UI] Upload failed: " + e.getMessage());
            System.out.println("[Log] Root Cause: " + e.getCause());
        } catch (StudentNotRegisteredException e) {
            System.out.println("[UI] Access Denied: " + e.getMessage());
        } catch (ExamException e) {
            System.out.println("[UI] General Exam Error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        OnlineExamPortal portal = new OnlineExamPortal();
        portal.handleRequest("S101", true, true, "ans1.pdf", 200);
        portal.handleRequest("S102", false, true, "ans2.pdf", 200);
        portal.handleRequest("S103", true, false, "ans3.pdf", 200);
        portal.handleRequest("S104", true, true, "ans4.pdf", 850);
    }
}