package dk.kvalitetsit.hjemmebehandling.model;

import dk.kvalitetsit.hjemmebehandling.api.PatientDto;

public class CarePlanModel {
    private String id;
    private String title;
    private String status;
    private PatientModel patient;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public PatientModel getPatient() {
        return patient;
    }

    public void setPatient(PatientModel patient) {
        this.patient = patient;
    }
}
