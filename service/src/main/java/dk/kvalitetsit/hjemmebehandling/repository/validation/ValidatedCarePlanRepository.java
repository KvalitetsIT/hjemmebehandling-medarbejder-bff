package dk.kvalitetsit.hjemmebehandling.repository.validation;

import dk.kvalitetsit.hjemmebehandling.model.CPR;
import dk.kvalitetsit.hjemmebehandling.model.CarePlanModel;
import dk.kvalitetsit.hjemmebehandling.model.PatientModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.repository.CarePlanRepository;
import dk.kvalitetsit.hjemmebehandling.service.access.AccessValidator;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public class ValidatedCarePlanRepository implements CarePlanRepository<CarePlanModel, PatientModel> {

    private final AccessValidator validator;
    private final CarePlanRepository<CarePlanModel, PatientModel> repository;

    public ValidatedCarePlanRepository(AccessValidator validator, CarePlanRepository<CarePlanModel, PatientModel> repository) {
        this.validator = validator;
        this.repository = repository;
    }

    @Override
    public void update(CarePlanModel carePlanModel, PatientModel patientModel) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public QualifiedId.CarePlanId save(CarePlanModel carePlanModel, PatientModel patientModel) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> fetchActiveCarePlansByPlanDefinitionId(QualifiedId.PlanDefinitionId plandefinitionId) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> fetchActiveCarePlansByQuestionnaireId(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> fetchCarePlansByPatientId(QualifiedId.PatientId patientId, boolean onlyActiveCarePlans) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> fetch(Instant unsatisfiedToDate, boolean onlyActiveCarePlans, boolean onlyUnSatisfied) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> fetch(QualifiedId.PatientId patientId, Instant unsatisfiedToDate, boolean onlyUnSatisfied, boolean onlyActiveCarePlans) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public void update(CarePlanModel resource) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public QualifiedId.CarePlanId save(CarePlanModel resource) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public Optional<CarePlanModel> fetch(QualifiedId.CarePlanId id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> fetch(List<QualifiedId.CarePlanId> id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<CarePlanModel> fetch() throws ServiceException {
        throw new NotImplementedException();
    }
}
