package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.QuestionnaireResponseApi;
import org.openapitools.client.model.ExaminationStatusDto;
import org.openapitools.client.model.PaginatedListQuestionnaireResponseDto;
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
    public void getQuestionnaireResponsesByCarePlanId_success() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";
        List<String> questionnaireIds = List.of("questionnaire-1");

        // Act
        ApiResponse<PaginatedListQuestionnaireResponseDto> response = subject.getQuestionnaireResponsesByCarePlanIdWithHttpInfo(carePlanId, questionnaireIds, 1, 1);


        assertEquals(3, response.getData().getTotal());
        // Assert
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void getQuestionnaireResponsesByStatus_success() throws Exception {
        // Arrange
        List<ExaminationStatusDto> statuses = List.of(ExaminationStatusDto.NOT_EXAMINED);
        int pageNumber = 1;
        int pageSize = 10;

        // Act
        ApiResponse<List<QuestionnaireResponseDto>> response = subject.getQuestionnaireResponsesByStatusWithHttpInfo(statuses, pageNumber, pageSize);

        // Assert
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void patchQuestionnaireResponse_success() throws Exception {
        // Arrange
        String id = "questionnaireresponse-2";
        PartialUpdateQuestionnaireResponseRequest request = new PartialUpdateQuestionnaireResponseRequest();
        request.setExaminationStatus(ExaminationStatusDto.UNDER_EXAMINATION);

        // Act
        ApiResponse<Void> response = subject.patchQuestionnaireResponseWithHttpInfo(id, request);

        // Assert
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void patchQuestionnaireResponse_twice_success() throws Exception {
        // Arrange
        String id = "questionnaireresponse-3";
        PartialUpdateQuestionnaireResponseRequest firstRequest = new PartialUpdateQuestionnaireResponseRequest();
        firstRequest.setExaminationStatus(ExaminationStatusDto.UNDER_EXAMINATION);

        PartialUpdateQuestionnaireResponseRequest secondRequest = new PartialUpdateQuestionnaireResponseRequest();
        secondRequest.setExaminationStatus(ExaminationStatusDto.EXAMINED);

        // Act / Assert
        ApiResponse<Void> firstResponse = subject.patchQuestionnaireResponseWithHttpInfo(id, firstRequest);
        assertEquals(200, firstResponse.getStatusCode());

        // insert dalay to prevent double creation of practitioner. This is a highly situational, and hopefully, temporary fix.
        // If this test is run before 'patchQuestionnaireResponse_success', the quick succession of first- and secordresponse
        // can cause the user to be created as a Practitioner two times because the time between the two requests is less than
        // cache refresh in hapi-fhir server.
        // There is no unique constraint in the hapi-fhir server on the Practitioners username/sor-code, but the subsequently
        // search fails if more than one is found, which can happen if 'patchQuestionnaireResponse_success' is run after
        // this test case.
        Thread.sleep(1000);

        ApiResponse<Void> secondResponse = subject.patchQuestionnaireResponseWithHttpInfo(id, secondRequest);
        assertEquals(200, secondResponse.getStatusCode());
    }
}
