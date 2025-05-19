package dk.kvalitetsit.hjemmebehandling.repository.access;

import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.CarePlanRepository;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class ValidatedCarePlanRepository implements CarePlanRepository<CarePlanModel, PatientModel> {

    private final AccessValidator<CarePlanModel> validator;
    private final CarePlanRepository<CarePlanModel, PatientModel> repository;

    public ValidatedCarePlanRepository(AccessValidator<CarePlanModel> validator, CarePlanRepository<CarePlanModel, PatientModel> repository) {
        this.validator = validator;
        this.repository = repository;
    }

    @Override
    public void update(CarePlanModel carePlanModel, PatientModel patientModel) throws ServiceException {
        repository.update(carePlanModel, patientModel);
    }

    @Override
    public QualifiedId.CarePlanId save(CarePlanModel carePlanModel, PatientModel patientModel) throws ServiceException {
        return repository.save(carePlanModel, patientModel);
    }

    @Override
    public List<CarePlanModel> fetchActiveCarePlansByPlanDefinitionId(QualifiedId.PlanDefinitionId plandefinitionId) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.fetchActiveCarePlansByPlanDefinitionId(plandefinitionId));
    }

    @Override
    public List<CarePlanModel> fetchActiveCarePlansByQuestionnaireId(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.fetchActiveCarePlansByQuestionnaireId(questionnaireId));
    }

    @Override
    public List<CarePlanModel> fetchCarePlansByPatientId(QualifiedId.PatientId patientId, boolean onlyActiveCarePlans) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.fetchCarePlansByPatientId(patientId, onlyActiveCarePlans));
    }

    @Override
    public List<CarePlanModel> fetch(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.fetch(unsatisfiedToDate, onlyActiveCarePlans, onlyUnSatisfied));
    }

    @Override
    public List<CarePlanModel> fetch(QualifiedId.PatientId patientId, Instant unsatisfiedToDate, boolean onlyUnSatisfied, boolean onlyActiveCarePlans) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.fetch(patientId, unsatisfiedToDate, onlyUnSatisfied, onlyActiveCarePlans));
    }

    @Override
    public void update(CarePlanModel resource) throws ServiceException {
        repository.update(resource);
    }

    @Override
    public QualifiedId.CarePlanId save(CarePlanModel resource) throws ServiceException {
        return repository.save(resource);
    }

    @Override
    public Optional<CarePlanModel> fetch(QualifiedId.CarePlanId id) throws ServiceException, AccessValidationException {
        var result = repository.fetch(id);
        if (result.isPresent()) return Optional.of(validator.validateAccess(result.get()));
        return result;
    }

    @Override
    public List<CarePlanModel> fetch(List<QualifiedId.CarePlanId> id) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.fetch(id));
    }

    @Override
    public List<CarePlanModel> fetch() throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.fetch());
    }

    @Override
    public List<CarePlanModel> history(QualifiedId.CarePlanId id) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.history(id));
    }

    @Override
    public List<CarePlanModel> history(List<QualifiedId.CarePlanId> carePlanIds) throws ServiceException, AccessValidationException {
        return validator.validateAccess(repository.history(carePlanIds));
    }
}
