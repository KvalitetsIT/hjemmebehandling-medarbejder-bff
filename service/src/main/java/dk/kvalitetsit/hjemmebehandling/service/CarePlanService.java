package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import dk.kvalitetsit.hjemmebehandling.types.Pagination;
import org.hl7.fhir.r4.model.TimeType;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface CarePlanService {
    QualifiedId.CarePlanId createCarePlan(CarePlanModel carePlan) throws ServiceException, AccessValidationException;

    CarePlanModel completeCarePlan(QualifiedId.CarePlanId carePlanId) throws ServiceException;

    List<CarePlanModel> getCarePlansWithFilters(boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException;

    List<CarePlanModel> getCarePlansWithFilters(CPR cpr, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException;

    List<CarePlanModel> getCarePlansWithFilters(boolean onlyActiveCarePlans, boolean onlyUnSatisfied, Pagination pagination) throws ServiceException;

    List<CarePlanModel> getCarePlansWithFilters(CPR cpr, boolean onlyActiveCarePlans, boolean onlyUnSatisfied, Pagination pagination) throws ServiceException;

    Optional<CarePlanModel> getCarePlanById(QualifiedId.CarePlanId carePlanId) throws ServiceException, AccessValidationException;

    CarePlanModel resolveAlarm(QualifiedId.CarePlanId carePlanId, QualifiedId.QuestionnaireId questionnaireId) throws ServiceException, AccessValidationException;

    CarePlanModel updateCarePlan(QualifiedId.CarePlanId carePlanId,
                                 List<QualifiedId.PlanDefinitionId> planDefinitionIds,
                                 List<QualifiedId.QuestionnaireId> questionnaireIds,
                                 Map<String, FrequencyModel> frequencies,
                                 PatientDetails patientDetails) throws ServiceException, AccessValidationException;

    List<QuestionnaireModel> getUnresolvedQuestionnaires(QualifiedId.CarePlanId carePlanId) throws AccessValidationException, ServiceException;

    TimeType getDefaultDeadlineTime() throws ServiceException;
}
