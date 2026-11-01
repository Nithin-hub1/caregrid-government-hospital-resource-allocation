import java.util.PriorityQueue;
import java.util.Comparator;
public class PatientPriorityQueue {
    private PriorityQueue<Patient> queue;
    public PatientPriorityQueue() {
        queue = new PriorityQueue<>(
            new Comparator<Patient>() {

                @Override
                public int compare(Patient p1, Patient p2) {
                    int priority1 = getPriority(p1.getSeverity());
                    int priority2 = getPriority(p2.getSeverity());
                    if (priority1 != priority2) {
                        return priority2 - priority1;
                    }
                    return p1.getArrivalOrder() - p2.getArrivalOrder();
                }
            }
        );
    }
    private int getPriority(String severity) {
        if (severity.equals("CRITICAL")) {
            return 3;
        }
        if (severity.equals("SERIOUS")) {
            return 2;
        }
        return 1;
    }
    public void addPatient(Patient patient) {
        queue.add(patient);
    }
    public Patient getNextPatient() {
        return queue.poll();
    }
    public boolean isEmpty() {
        return queue.isEmpty();
    }
}