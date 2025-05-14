package dk.kvalitetsit.hjemmebehandling.repository;

import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.AccessValidationException;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.Collection;
import java.util.List;

public interface QuestionnaireRepository<Questionnaire> extends Repository<Questionnaire, QualifiedId.QuestionnaireId> {


    /**
     * Looks up questionnaires filtered by their status values.
     *
     * @param statusesToInclude Statuses to include.
     * @return List of matching questionnaires.
     * @throws ServiceException If the operation fails.
     */
    List<Questionnaire> fetch(Collection<String> statusesToInclude) throws ServiceException, AccessValidationException;

}
