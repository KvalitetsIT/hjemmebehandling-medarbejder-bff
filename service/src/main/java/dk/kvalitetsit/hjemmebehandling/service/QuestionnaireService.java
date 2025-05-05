package dk.kvalitetsit.hjemmebehandling.service;

import dk.kvalitetsit.hjemmebehandling.model.PlanDefinitionModel;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.model.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireModel;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface QuestionnaireService {
    Optional<QuestionnaireModel> getQuestionnaireById(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException, AccessValidationException;

    List<QuestionnaireModel> getQuestionnaires(Collection<String> statusesToInclude) throws ServiceException;

    QualifiedId.QuestionnaireId createQuestionnaire(QuestionnaireModel questionnaire) throws ServiceException;

    // TODO: Reduce number of parameters
    void updateQuestionnaire(
            QualifiedId.QuestionnaireId questionnaireId,
            String updatedTitle,
            String updatedDescription,
            String updatedStatus,
            List<QuestionModel> updatedQuestions,
            QuestionModel updatedCallToAction
    ) throws ServiceException, AccessValidationException;

    void retireQuestionnaire(QualifiedId.QuestionnaireId id) throws ServiceException;

    List<PlanDefinitionModel> getPlanDefinitionsThatIncludes(QualifiedId.QuestionnaireId questionnaireId) throws ServiceException;
}
