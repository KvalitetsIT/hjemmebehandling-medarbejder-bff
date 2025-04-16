package dk.kvalitetsit.hjemmebehandling.fhir.client;

import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirLookupResult;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirMapper;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class CarePlanClientAdaptor implements CarePlanClient<CarePlanModel, PatientModel> {

    private final CarePlanClient<CarePlan, Patient> client;
    private final FhirMapper mapper;

    public CarePlanClientAdaptor(CarePlanClient<CarePlan, Patient> client, FhirMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    @Override
    public void update(CarePlanModel resource) {
        this.client.update(this.mapper.mapCarePlanModel(resource));
    }

    @Override
    public String save(CarePlanModel resource) throws ServiceException {
        return this.client.save(this.mapper.mapCarePlanModel(resource));
    }

    @Override
    public Optional<CarePlanModel> fetch(String id) throws ServiceException {
        return this.client.fetch(id).map(mapper::mapCarePlan);
    }

    @Override
    public List<CarePlanModel> fetch(String... id) {
        return this.client.fetch(id).stream().map(mapper::mapCarePlan).toList();
    }

    @Override
    public List<CarePlanModel> fetch() {
        return this.client.fetch().stream().map(mapper::mapCarePlan).toList();
    }

    @Override
    public List<CarePlanModel> fetchActiveCarePlansWithQuestionnaire(String questionnaireId) throws ServiceException {
        return client.fetchActiveCarePlansWithQuestionnaire(questionnaireId).stream().map(mapper::mapCarePlan).toList();
    }

    @Override
    public List<CarePlanModel> fetchActiveCarePlansWithPlanDefinition(String plandefinitionId) throws ServiceException {
        return client.fetchActiveCarePlansWithPlanDefinition(plandefinitionId).stream().map(mapper::mapCarePlan).toList();
    }

    @Override
    public List<CarePlanModel> fetchCarePlansByPatientId(String patientId, boolean onlyActiveCarePlans) throws ServiceException {
        return client.fetchCarePlansByPatientId(patientId, onlyActiveCarePlans)
                .stream()
                .map(mapper::mapCarePlan)
                .toList();
    }

    @Override
    public List<CarePlanModel> fetchCarePlans(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        return client.fetchCarePlans(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied)
                .stream()
                .map(mapper::mapCarePlan)
                .toList();
    }

    @Override
    public List<CarePlanModel> lookupCarePlans(String cpr, Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        return client.lookupCarePlans(cpr, unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied)
                .stream()
                .map(mapper::mapCarePlan)
                .toList();
    }

    @Override
    public FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria) throws ServiceException {
        return null;
    }

    @Override
    public FhirLookupResult lookupCarePlansByCriteria(List<ICriterion<?>> criteria, Optional<SortSpec> sortSpec) throws ServiceException {
        return null;
    }

    @Override
    public void update(CarePlanModel carePlanModel, PatientModel patientModel) {
        this.client.update(mapper.mapCarePlanModel(carePlanModel), mapper.mapPatientModel(patientModel));
    }

    @Override
    public String save(CarePlanModel carePlan, PatientModel patient) throws ServiceException {
        return client.save(this.mapper.mapCarePlanModel(carePlan), this.mapper.mapPatientModel(patient));
    }




}
