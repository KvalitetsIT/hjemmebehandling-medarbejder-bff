package dk.kvalitetsit.hjemmebehandling.repository.implementation;

import ca.uhn.fhir.rest.api.SortOrderEnum;
import ca.uhn.fhir.rest.api.SortSpec;
import ca.uhn.fhir.rest.gclient.ICriterion;
import dk.kvalitetsit.hjemmebehandling.fhir.BundleBuilder;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirClient;
import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.constants.SearchParameters;
import dk.kvalitetsit.hjemmebehandling.repository.CarePlanRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.CarePlan;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.PlanDefinition;
import org.hl7.fhir.r4.model.ResourceType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A concrete implementation of the {@link CarePlanRepository} interface for handling
 * {@link CarePlan} entities associated with {@link Patient} resources.
 * <p>
 * This repository encapsulates the logic for accessing, storing, and managing CarePlan data,
 * bridging the domain layer and the underlying data sources.
 */
public class ConcreteCarePlanRepository implements CarePlanRepository<CarePlan, Patient> {

    private final FhirClient client;

    public ConcreteCarePlanRepository(FhirClient client) {
        this.client = client;
    }



    @Override
    public void update(CarePlan carePlan, Patient patient) {
        var bundle = new BundleBuilder().buildUpdateCarePlanBundle(carePlan, patient);
        client.saveInTransaction(bundle, ResourceType.CarePlan);
    }

    @Override
    public QualifiedId.CarePlanId save(CarePlan carePlan, Patient patient) throws ServiceException {
        // Build a transaction bundle.
        var bundle = new BundleBuilder().buildCreateCarePlanBundle(carePlan, patient);
        client.addOrganizationTag(bundle);
        var id = client.saveInTransaction(bundle, ResourceType.CarePlan).map(QualifiedId.CarePlanId::new);
        return id.orElseThrow(() -> new IllegalStateException("Could not locate location-header in response when executing transaction."));
    }

    @Override
    public List<CarePlan> fetch(QualifiedId.PatientId patientId, Instant unsatisfiedToDate, boolean onlyUnSatisfied, boolean onlyActiveCarePlans) throws ServiceException {
        var criteria = client.createCriteria(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied);
        var patientCriterion = CarePlan.PATIENT.hasId(patientId.unqualified());
        criteria.add(patientCriterion);
        var sortSpec = new SortSpec(SearchParameters.CAREPLAN_SATISFIED_UNTIL, SortOrderEnum.ASC);
        return lookupCarePlansByCriteria(criteria, sortSpec);
    }

    @Override
    public List<CarePlan> fetchActiveCarePlansByPlanDefinitionId(QualifiedId.PlanDefinitionId plandefinitionId) throws ServiceException {
        var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
        var plandefinitionCriterion = FhirUtils.buildPlanDefinitionCriterion(plandefinitionId);
        List<ICriterion<?>> criteria = new ArrayList<>(List.of(statusCriterion, plandefinitionCriterion));
        return lookupCarePlansByCriteria(criteria);
    }

    @Override
    public List<CarePlan> fetchCarePlansByPatientId(QualifiedId.PatientId patientId, boolean onlyActiveCarePlans) throws ServiceException {
        var patientCriterion = CarePlan.PATIENT.hasId(patientId.unqualified());
        List<ICriterion<?>> criteria = new ArrayList<>(List.of(patientCriterion));
        if (onlyActiveCarePlans) {
            var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
            criteria.add(statusCriterion);
        }
        return lookupCarePlansByCriteria(criteria);
    }

    @Override
    public List<CarePlan> fetch(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        var criteria = client.createCriteria(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied);
        var sortSpec = new SortSpec(SearchParameters.CAREPLAN_SATISFIED_UNTIL, SortOrderEnum.ASC);
        return lookupCarePlansByCriteria(criteria, sortSpec);
    }

    public List<CarePlan> fetchActiveCarePlansByQuestionnaireId(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException {
        var statusCriterion = CarePlan.STATUS.exactly().code(CarePlan.CarePlanStatus.ACTIVE.toCode());
        var questionnaireCriterion = CarePlan.INSTANTIATES_CANONICAL.hasChainedProperty(PlanDefinition.DEFINITION.hasId(questionnaireId.unqualified()));
        List<ICriterion<?>> criteria = new ArrayList<>(List.of(statusCriterion, questionnaireCriterion));
        return lookupCarePlansByCriteria(criteria);
    }

    @Override
    public void update(CarePlan resource) {
        throw new NotImplementedException();
    }

    @Override
    public QualifiedId.CarePlanId save(CarePlan resource) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public Optional<CarePlan> fetch(QualifiedId.CarePlanId id) throws ServiceException {
        var idCriterion = CarePlan.RES_ID.exactly().code(id.unqualified());
        var result = this.lookupCarePlansByCriteria(List.of(idCriterion));
        if (result.size() > 1) throw new IllegalStateException(String.format(
                "Could not lookup single resource of class %s!",
                CarePlan.class
        ));

        return Optional.of(result.getFirst());
    }

    @Override
    public List<CarePlan> fetch(List<QualifiedId.CarePlanId> id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlan> fetch() {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlan> history(QualifiedId.CarePlanId id) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlan> history(List<QualifiedId.CarePlanId> carePlanIds) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    private List<CarePlan> lookupCarePlansByCriteria(List<ICriterion<?>> criteria) throws ServiceException {
        return client.lookupByCriteria(
                CarePlan.class, criteria,
                List.of(CarePlan.INCLUDE_SUBJECT, CarePlan.INCLUDE_INSTANTIATES_CANONICAL)
        );
    }

    private List<CarePlan> lookupCarePlansByCriteria(List<ICriterion<?>> criteria, SortSpec sortSpec) throws ServiceException {
        // The FhirLookupResult includes the patient- and plandefinition-resources that we need,
        // but due to limitations of the FHIR server, not the questionnaire-resources. Se wo look up those in a separate call.
        return client.lookupByCriteria(
                CarePlan.class, criteria,
                List.of(CarePlan.INCLUDE_SUBJECT, CarePlan.INCLUDE_INSTANTIATES_CANONICAL),
                Optional.of(sortSpec),
                Optional.empty(),
                Optional.empty()
        );
    }


}
