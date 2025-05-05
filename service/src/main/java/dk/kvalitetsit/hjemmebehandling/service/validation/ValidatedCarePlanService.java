package dk.kvalitetsit.hjemmebehandling.service.validation;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.CarePlanService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.apache.commons.lang3.NotImplementedException;
import org.hl7.fhir.r4.model.TimeType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ValidatedCarePlanService implements CarePlanService {

    private final CarePlanService service;
    private final AccessValidatingService accessValidatingService;

    public ValidatedCarePlanService(CarePlanService service, AccessValidatingService accessValidatingService) {
        this.service = service;
        this.accessValidatingService = accessValidatingService;
    }

    @Override
    public QualifiedId.CarePlanId createCarePlan(CarePlanModel carePlan) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public CarePlanModel completeCarePlan(QualifiedId.CarePlanId carePlanId) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> getCarePlansWithFilters(boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> getCarePlansWithFilters(CPR cpr, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> getCarePlansWithFilters(boolean onlyActiveCarePlans, boolean onlyUnSatisfied, Pagination pagination) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> getCarePlansWithFilters(CPR cpr, boolean onlyActiveCarePlans, boolean onlyUnSatisfied, Pagination pagination) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public Optional<CarePlanModel> getCarePlanById(QualifiedId.CarePlanId carePlanId) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public CarePlanModel resolveAlarm(QualifiedId.CarePlanId carePlanId, QualifiedId.QuestionnaireId questionnaireId) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public CarePlanModel updateCarePlan(QualifiedId.CarePlanId carePlanId, List<QualifiedId.PlanDefinitionId> planDefinitionIds, List<QualifiedId.QuestionnaireId> questionnaireIds, Map<String, FrequencyModel> frequencies, PatientDetails patientDetails) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<QuestionnaireModel> getUnresolvedQuestionnaires(QualifiedId.CarePlanId carePlanId) throws AccessValidationException, ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public TimeType getDefaultDeadlineTime() throws ServiceException {
        throw new NotImplementedException();
    }
}
