package dk.kvalitetsit.hjemmebehandling.fhir.repository;

import dk.kvalitetsit.hjemmebehandling.model.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.model.QualifiedId;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;

import java.util.List;

public interface QuestionnaireResponseRepository<QuestionnaireResponse> extends Repository<QuestionnaireResponse, QualifiedId.QuestionnaireResponseId> {


    /**
     * Looks up questionnaire responses for a care plan and specific questionnaires.
     *
     * @param carePlanId       The care plan ID.
     * @param questionnaireIds The questionnaire IDs.
     * @return List of matching responses.
     */
    List<QuestionnaireResponse> fetch(QualifiedId.CarePlanId carePlanId, List<QualifiedId.QuestionnaireId> questionnaireIds);


    /**
     * Looks up questionnaire responses filtered by status.
     *
     * @param statuses List of statuses to filter by.
     * @return List of responses.
     * @throws ServiceException If the operation fails.
     */
    List<QuestionnaireResponse> fetchByStatus(List<ExaminationStatus> statuses) throws ServiceException;

    /**
     * Looks up questionnaire responses by status and care plan ID.
     *
     * @param statuses   List of statuses.
     * @param carePlanId The care plan ID.
     * @return List of responses.
     * @throws ServiceException If the operation fails.
     */
    List<QuestionnaireResponse> fetch(List<ExaminationStatus> statuses, QualifiedId.CarePlanId carePlanId) throws ServiceException;

    /**
     * Looks up questionnaire responses by a single status.
     *
     * @param status The status.
     * @return List of responses.
     * @throws ServiceException If the operation fails.
     */
    List<QuestionnaireResponse> fetchByStatus(ExaminationStatus status) throws ServiceException;


}
