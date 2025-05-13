package dk.kvalitetsit.hjemmebehandling.model.audit;

public class AuditModel {
    private Employee employee;
    private Citizen citizen;
    private String message;

    public AuditModel(String employeeRegionsID, String employeeFullName, String citizenCpr, String citizenFullName, String message) {
        this.employee = new Employee(employeeRegionsID, employeeFullName);
        this.citizen = new Citizen(citizenCpr, citizenFullName);
        this.message = message;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public Citizen getCitizen() {
        return citizen;
    }

    public void setCitizen(Citizen citizen) {
        this.citizen = citizen;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    private class Employee {
        private String regionsID;
        private String fullName;

        public Employee(String regionsID, String fullName) {
            this.regionsID = regionsID;
            this.fullName = fullName;
        }

        public String getRegionsID() {
            return regionsID;
        }

        public void setRegionsID(String regionsID) {
            this.regionsID = regionsID;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }

    private class Citizen {
        private String cpr;
        private String fullName;

        public Citizen(String cpr, String fullName) {
            this.cpr = cpr;
            this.fullName = fullName;
        }

        public String getCpr() {
            return cpr;
        }

        public void setCpr(String cpr) {
            this.cpr = cpr;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }
    }
}
