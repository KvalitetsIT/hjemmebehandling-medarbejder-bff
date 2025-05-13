package dk.kvalitetsit.hjemmebehandling.service.access;

import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.model.constants.Status;
import dk.kvalitetsit.hjemmebehandling.service.QuestionnaireService;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.apache.commons.lang3.NotImplementedException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public class ValidatedQuestionnaireService implements QuestionnaireService {

    private final AccessValidator accessValidator;
    private final QuestionnaireService service;

    public ValidatedQuestionnaireService(AccessValidator accessValidator, QuestionnaireService service) {
        this.accessValidator = accessValidator;
        this.service = service;
    }

    @Override
    public Optional<QuestionnaireModel> getQuestionnaireById(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public List<QuestionnaireModel> getQuestionnaires(Collection<String> statusesToInclude) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public QualifiedId.QuestionnaireId createQuestionnaire(QuestionnaireModel questionnaire) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public void updateQuestionnaire(QualifiedId.QuestionnaireId questionnaireId,
                                    String updatedTitle,
                                    String updatedDescription,
                                    Status updatedStatus,
                                    List<QuestionModel> updatedQuestions,
                                    QuestionModel updatedCallToAction
    ) throws ServiceException, AccessValidationException {
        throw new NotImplementedException();
    }

    @Override
    public void retireQuestionnaire(QualifiedId.QuestionnaireId id) throws ServiceException {
        throw new NotImplementedException();
    }

    @Override
    public List<PlanDefinitionModel> getPlanDefinitionsThatIncludes(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException {
        throw new NotImplementedException();
    }
}
