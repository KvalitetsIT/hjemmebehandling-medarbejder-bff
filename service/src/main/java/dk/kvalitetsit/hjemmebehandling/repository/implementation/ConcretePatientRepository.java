package dk.kvalitetsit.hjemmebehandling.repository.implementation;

import ca.uhn.fhir.rest.gclient.ICriterion;
import ca.uhn.fhir.rest.gclient.StringClientParam;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.PatientRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A concrete implementation of the {@link PatientRepository} interface for handling
 * {@link Patient} entities associated with {@link CarePlan} resources.
 * <p>
 * This repository encapsulates the logic for accessing, storing, and managing CarePlan data,
 * bridging the domain layer and the underlying data sources.
 */
public class ConcretePatientRepository implements PatientRepository<Patient, CarePlan.CarePlanStatus> {

    private final FhirClient client;

    public ConcretePatientRepository(FhirClient client) {
        this.client = client;
    }

    @Override
    public List<Patient> searchPatients(List<String> searchStrings, CarePlan.CarePlanStatus status) throws ServiceException {
        // FHIR has no way of expressing 'search patient with name like %search% OR cpr like %search%'
        // so we have to do that in two seperate queries
        var cprCriterion = CarePlan.PATIENT.hasChainedProperty(new StringClientParam("patient_identifier_cpr").matches().values(searchStrings));
        var nameCriterion = CarePlan.PATIENT.hasChainedProperty(Patient.NAME.matches().values(searchStrings));
        var statusCriterion = CarePlan.STATUS.exactly().code(status.toCode());

        var patientsByCPR = lookupPatients(List.of(cprCriterion, statusCriterion));
        var patientsByName = lookupPatients(List.of(nameCriterion, statusCriterion));
        return Stream.concat(patientsByCPR.stream(), patientsByName.stream())
                .distinct()
                .toList();
    }

    @Override
    public List<Patient> fetchByStatus(CarePlan.CarePlanStatus... status) throws ServiceException {
        List<ICriterion<?>> statusCriteria = Arrays.stream(status)
                .distinct()
                .map(CarePlan.CarePlanStatus::toCode)
                .map(x -> CarePlan.STATUS.exactly().code(x))
                .collect(Collectors.toList());

        return lookupPatients(statusCriteria);
    }

    @Override
    public Optional<Patient> fetch(CPR cpr) {
        throw new NotImplementedException();
    }

    @Override
    public void update(Patient patient) {
        client.updateResource(patient);
    }

    @Override
    public QualifiedId.PatientId save(Patient patient) throws ServiceException {
        return new QualifiedId.PatientId(client.saveResource(patient));
    }

    @Override
    public Optional<Patient> fetch(QualifiedId.PatientId id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<Patient> fetch(List<QualifiedId.PatientId> id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<Patient> fetch() throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<Patient> history(QualifiedId.PatientId id) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<Patient> history(List<QualifiedId.PatientId> patientIds) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    private Optional<Patient> lookupPatient(List<ICriterion<?>> criterion) throws ServiceException {
        var lookupResult = lookupPatients(criterion);

        if (lookupResult.isEmpty()) return Optional.empty();

        if (lookupResult.size() > 1) throw new IllegalStateException(String.format(
                "Could not lookup single resource of class %s!",
                Patient.class
        ));

        return Optional.ofNullable(lookupResult.getFirst());
    }

    private List<Patient> lookupPatients(List<ICriterion<?>> criterion) throws ServiceException {
        return client.fetchByCriteria(Patient.class, criterion);
    }


}

