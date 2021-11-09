package dk.kvalitetsit.hjemmebehandling.constants;

public enum ExaminationStatus {
    NOT_EXAMINED(2), UNDER_EXAMINATION(1), EXAMINED(3);

    private int priority;

    public int getPriority() {
        return priority;
    }

    ExaminationStatus(int priority) {
        this.priority = priority;
    }
}
