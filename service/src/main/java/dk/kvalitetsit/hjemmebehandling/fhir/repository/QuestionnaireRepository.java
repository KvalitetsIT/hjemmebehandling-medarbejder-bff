package dk.kvalitetsit.hjemmebehandling.fhir.repository;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.Collection;
import java.util.List;

public interface QuestionnaireRepository<Questionnaire> extends Repository<Questionnaire> {


    /**
     * Looks up questionnaires filtered by their status values.
     *
     * @param statusesToInclude Statuses to include.
     * @return List of matching questionnaires.
     * @throws ServiceException If the operation fails.
     */
    List<Questionnaire> lookupQuestionnairesByStatus(Collection<String> statusesToInclude) throws ServiceException;

    /**
     * Looks up all versions of questionnaires by their IDs.
     *
     * @param ids List of questionnaire IDs.
     * @return List of questionnaires.
     */
    List<Questionnaire> lookupVersionsOfQuestionnaireById(List<QualifiedId> ids);

}
