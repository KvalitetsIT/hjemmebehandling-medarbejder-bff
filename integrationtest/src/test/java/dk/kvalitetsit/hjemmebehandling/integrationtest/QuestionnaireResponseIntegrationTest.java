package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.QuestionnaireResponseApi;
import org.openapitools.client.model.PageDetails;
import org.openapitools.client.model.PartialUpdateQuestionnaireResponseRequest;
import org.openapitools.client.model.QuestionnaireResponseDto;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class QuestionnaireResponseIntegrationTest extends AbstractIntegrationTest {
    private QuestionnaireResponseApi subject;

    @BeforeEach
    public void setup() {
        subject = new QuestionnaireResponseApi();

        subject.getApiClient().setBasePath(enhanceBasePath(subject.getApiClient().getBasePath()));
    }

    @Test
    public void getQuestionnaireResponsesByCpr_success() throws Exception {
        // Arrange
        String cpr = "0101010101";
        List<String> questionnaireIds = List.of("Questionnaire/questionnaire-1");

        // Act
        ApiResponse<List<QuestionnaireResponseDto>> response = subject.getQuestionnaireResponsesByCprWithHttpInfo(cpr, questionnaireIds);

        // Assert
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_success() throws Exception {
        // Arrange
        List<String> statuses = List.of("NOT_EXAMINED");
        PageDetails pageDetails = new PageDetails();
        pageDetails.setPageNumber(1);
        pageDetails.setPageSize(10);;

        // Act
        ApiResponse<List<QuestionnaireResponseDto>> response = subject.getQuestionnaireResponsesByStatusWithHttpInfo(statuses, pageDetails);

        // Assert
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void patchQuestionnaireResponse_success() throws Exception {
        // Arrange
        //String id = "QuestionnaireResponse/questionnaireresponse-2";
        String id = "questionnaireresponse-2";
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        request.setExaminationStatus(PartialUpdateQuestionnaireResponseRequest.ExaminationStatusEnum.UNDER_EXAMINATION);

        // Act
        ApiResponse<Void> response = subject.patchQuestionnaireResponseWithHttpInfo(id, request);

        // Assert
        assertEquals(200, response.getStatusCode());
    }
}
