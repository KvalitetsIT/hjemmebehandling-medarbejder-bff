package dk.kvalitetsit.hjemmebehandling.model;

public record AuditModel(
        Employee employee,
        Citizen citizen,
        String message
) {
    public AuditModel(Employee employee, Citizen citizen, String message) {
        this.employee = employee;
        this.citizen = citizen;
        this.message = message;
    }

    public record Employee(
            String regionsID,
            String fullName
    ) { }

    public record Citizen(
            String cpr,
            String fullName
    ) {
    }
}
