package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.service.model.Patient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class PatientService {
    private static final Logger logger = LoggerFactory.getLogger(PatientService.class);

    public List<Patient> getPatients(String clinicalIdentifier) {
        Patient p = new Patient();

        p.setCpr("0101010101");
        p.setFamilyName("Ærtegærde Ømø Ååstrup");
        p.setGivenName("Torgot");

        return List.of(p);
    }
}
