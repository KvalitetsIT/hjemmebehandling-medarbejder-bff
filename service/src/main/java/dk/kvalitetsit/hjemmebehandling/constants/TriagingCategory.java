package dk.kvalitetsit.hjemmebehandling.constants;

public enum TriagingCategory {
    GREEN(3), YELLOW(2), RED(1);

    private final int priority;

    public int getPriority() {
        return priority;
    }

    TriagingCategory(int priority) {
        this.priority = priority;
    }
}
