package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.CarePlanApi;
import org.openapitools.client.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class CarePlanIntegrationTest extends AbstractIntegrationTest {
    private CarePlanApi subject;

    @BeforeEach
    public void setup() {
        subject = new CarePlanApi();

        subject.getApiClient().setBasePath(enhanceBasePath(subject.getApiClient().getBasePath()));
    }

    @Test
    public void createCarePlan_success() throws Exception {
        // Arrange
        CarePlanDto carePlanDto = new CarePlanDto();

        carePlanDto.setPatientDto(new PatientDto());
        carePlanDto.getPatientDto().setCpr("0606060606");

        QuestionnaireDto questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setId("Questionnaire/questionnaire-1");

        FrequencyDto frequencyDto = new FrequencyDto();
        frequencyDto.setWeekdays(List.of(FrequencyDto.WeekdaysEnum.TUE, FrequencyDto.WeekdaysEnum.FRI));
        frequencyDto.setTimeOfDay("04:00");

        QuestionnaireWrapperDto wrapper = new QuestionnaireWrapperDto();
        wrapper.setQuestionnaire(questionnaireDto);
        wrapper.setFrequency(frequencyDto);

        carePlanDto.setQuestionnaires(List.of(wrapper));

        CreateCarePlanRequest request = new CreateCarePlanRequest()
                .carePlan(carePlanDto);

        // Act
        ApiResponse<Void> response = subject.createCarePlanWithHttpInfo(request);

        // Assert
        assertEquals(201, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("location"));
    }

    @Test
    public void getCarePlan_success() throws Exception {
        // Arrange
        String carePlanId = "careplan-1";

        // Act
        ApiResponse<CarePlanDto> response = subject.getCarePlanByIdWithHttpInfo(carePlanId);

        // Assert
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void getCarePlansByCpr_success() throws Exception {
        // Arrange
        String cpr = "0101010101";

        // Act
        ApiResponse<List<CarePlanDto>> response = subject.searchCarePlansWithHttpInfo(cpr, null);

        // Assert
        assertEquals(200, response.getStatusCode());
    }

    @Test
    public void getCarePlansWithUnsatisfiedSchedules_success() throws Exception {
        // Arrange
        boolean onlyUnsatisfiedSchedules = true;

        // Act
        ApiResponse<List<CarePlanDto>> response = subject.searchCarePlansWithHttpInfo(null, onlyUnsatisfiedSchedules);

        // Assert
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @Disabled
    public void patchCarePlan_success() throws Exception {
        // Arrange
        String id = "careplan-1";
        PartialUpdateCareplanRequest request = new PartialUpdateCareplanRequest();
        request.addQuestionnaireIdsItem("questionnaire-1");

        FrequencyDto frequencyDto = new FrequencyDto();
        frequencyDto.setWeekdays(List.of(FrequencyDto.WeekdaysEnum.TUE));
        frequencyDto.setTimeOfDay("04:00");
        request.putQuestionnaireFrequenciesItem("questionnaire-1", frequencyDto);

        // Act
        ApiResponse<Void> response = subject.patchCarePlanWithHttpInfo(id, request);

        // Assert
        assertEquals(200, response.getStatusCode());
    }
}
