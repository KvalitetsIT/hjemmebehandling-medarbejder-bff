package dk.kvalitetsit.hjemmebehandling.constants;

public enum TriagingCategory {
    RED(1), YELLOW(2), GREEN(3);

    private int priority;

    public int getPriority() {
        return priority;
    }

    TriagingCategory(int priority) {
        this.priority = priority;
    }
}
