package dk.kvalitetsit.hjemmebehandling.fhir.repository.adaptation;

import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.fhir.repository.CarePlanRepository;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * An adapter whose responsibility is to adapt between FHIR and the domain logic.
 * This primarily covers mapping from business models and calling further into the stack with the expected arguments
 * For now, it implements the CarePlanRepository interface, but this might change in the future
 */
public class CarePlanRepositoryAdaptor implements CarePlanRepository<CarePlanModel, PatientModel> {

    private final CarePlanRepository<CarePlan, Patient> client;
    private final FhirMapper mapper;

    public CarePlanRepositoryAdaptor(CarePlanRepository<CarePlan, Patient> client, FhirMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public void update(CarePlanModel resource) throws ServiceException {
        this.client.update(this.mapper.mapCarePlanModel(resource));
    }

    @Override
    public QualifiedId.CarePlanId save(CarePlanModel resource) throws ServiceException {
        return this.client.save(this.mapper.mapCarePlanModel(resource));
    }

    @Override
    public Optional<CarePlanModel> fetch(QualifiedId.CarePlanId id) throws ServiceException {
        return this.client.fetch(id).map(mapper::mapCarePlan);
    }

    @Override
    public List<CarePlanModel> fetch(List<QualifiedId.CarePlanId> ids) throws ServiceException {
        return this.client.fetch(ids).stream().map(mapper::mapCarePlan).toList();
    }

    @Override
    public List<CarePlanModel> fetch() throws ServiceException {
        return this.client.fetch().stream().map(mapper::mapCarePlan).toList();
    }

    @Override
    public List<CarePlanModel> fetchActiveCarePlansWithQuestionnaire(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException {
        return client.fetchActiveCarePlansWithQuestionnaire(questionnaireId).stream().map(mapper::mapCarePlan).toList();
    }

    @Override
    public List<CarePlanModel> fetchActiveCarePlansByPlanDefinitionId(QualifiedId.PlanDefinitionId plandefinitionId) throws ServiceException {
        return client.fetchActiveCarePlansByPlanDefinitionId(plandefinitionId).stream().map(mapper::mapCarePlan).toList();
    }

    @Override
    public List<CarePlanModel> fetchCarePlansByPatientId(QualifiedId.PatientId patientId, boolean onlyActiveCarePlans) throws ServiceException {
        return client.fetchCarePlansByPatientId(patientId, onlyActiveCarePlans)
                .stream()
                .map(mapper::mapCarePlan)
                .toList();
    }

    @Override
    public List<CarePlanModel> fetch(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        return client.fetch(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied)
                .stream()
                .map(mapper::mapCarePlan)
                .toList();
    }

    @Override
    public List<CarePlanModel> fetch(QualifiedId.PatientId patientId, Instant unsatisfiedToDate, boolean onlyUnSatisfied, boolean onlyActiveCarePlans) throws ServiceException {
        return client.fetch(patientId, unsatisfiedToDate, onlyUnSatisfied, onlyActiveCarePlans)
                .stream()
                .map(mapper::mapCarePlan)
                .toList();
    }

    @Override
    public void update(CarePlanModel carePlanModel, PatientModel patientModel) {
        this.client.update(mapper.mapCarePlanModel(carePlanModel), mapper.mapPatientModel(patientModel));
    }

    @Override
    public QualifiedId.CarePlanId save(CarePlanModel carePlan, PatientModel patient) throws ServiceException {
        return client.save(this.mapper.mapCarePlanModel(carePlan), this.mapper.mapPatientModel(patient));
    }

}
