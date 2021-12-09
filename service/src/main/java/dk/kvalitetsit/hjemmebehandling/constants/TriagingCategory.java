package dk.kvalitetsit.hjemmebehandling.constants;

public enum TriagingCategory {
    GREEN(1), YELLOW(2), RED(3);

    private int priority;

    public int getPriority() {
        return priority;
    }

    TriagingCategory(int priority) {
        this.priority = priority;
    }
}
