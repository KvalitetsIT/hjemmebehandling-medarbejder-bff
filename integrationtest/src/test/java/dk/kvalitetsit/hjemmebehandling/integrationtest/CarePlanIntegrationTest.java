package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.CarePlanApi;
import org.openapitools.client.model.*;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CarePlanIntegrationTest extends AbstractIntegrationTest {
    private CarePlanApi subject;

    @BeforeEach
    public void setup() {
        subject = new CarePlanApi();

        subject.getApiClient().setBasePath(enhanceBasePath(subject.getApiClient().getBasePath()));
    }

    @Test
    @Order(1)
    public void createCarePlan_success() throws Exception {
        // Arrange
        CarePlanDto carePlanDto = new CarePlanDto();

        carePlanDto.setPatientDto(new PatientDto());
        carePlanDto.getPatientDto().setCpr("0908060609");

        QuestionnaireDto questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setId("Questionnaire/questionnaire-1");

        FrequencyDto frequencyDto = new FrequencyDto();
        frequencyDto.setWeekdays(List.of(FrequencyDto.WeekdaysEnum.TUE, FrequencyDto.WeekdaysEnum.FRI));
        frequencyDto.setTimeOfDay("04:00");

        QuestionnaireWrapperDto wrapper = new QuestionnaireWrapperDto();
        wrapper.setQuestionnaire(questionnaireDto);
        wrapper.setFrequency(frequencyDto);

        carePlanDto.setQuestionnaires(List.of(wrapper));

        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();
        planDefinitionDto.setId("PlanDefinition/plandefinition-1");
        carePlanDto.setPlanDefinitions(List.of(planDefinitionDto));

        CreateCarePlanRequest request = new CreateCarePlanRequest()
                .carePlan(carePlanDto);

        // Act
        ApiResponse<Void> response = subject.createCarePlanWithHttpInfo(request);

        // Assert
        assertEquals(201, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("location"));
    }
//
//    @Test
//    @Order(2)
//    public void createAndGetCarePlan_success() throws Exception {
//        // Arrange
//        String cpr = "4444444444";
//
//        CarePlanDto carePlanDto = new CarePlanDto();
//
//        carePlanDto.setPatientDto(new PatientDto());
//        carePlanDto.getPatientDto().setCpr(cpr);
//
//        QuestionnaireDto questionnaireDto = new QuestionnaireDto();
//        questionnaireDto.setId("Questionnaire/questionnaire-1");
//
//        FrequencyDto frequencyDto = new FrequencyDto();
//        frequencyDto.setWeekdays(List.of(FrequencyDto.WeekdaysEnum.TUE, FrequencyDto.WeekdaysEnum.FRI));
//        frequencyDto.setTimeOfDay("04:00");
//
//        QuestionnaireWrapperDto wrapper = new QuestionnaireWrapperDto();
//        wrapper.setQuestionnaire(questionnaireDto);
//        wrapper.setFrequency(frequencyDto);
//
//        carePlanDto.setQuestionnaires(List.of(wrapper));
//
//        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();
//        planDefinitionDto.setId("PlanDefinition/plandefinition-1");
//        carePlanDto.setPlanDefinitions(List.of(planDefinitionDto));
//
//        CreateCarePlanRequest request = new CreateCarePlanRequest()
//                .carePlan(carePlanDto);
//
//        // Act
//        ApiResponse<Void> createResponse = subject.createCarePlanWithHttpInfo(request);
//
//        //Thread.sleep(200);
//
//        String id = createResponse.getHeaders().get("Location").get(0);
//        id = id.substring(id.lastIndexOf('/') + 1);
//        ApiResponse<CarePlanDto> getResponse = subject.getCarePlanByIdWithHttpInfo(id);
//
//        // Assert
//        assertEquals(201, createResponse.getStatusCode());
//        assertTrue(createResponse.getHeaders().containsKey("location"));
//
//        assertEquals(200, getResponse.getStatusCode());
//        assertEquals("CarePlan/" + id, getResponse.getData().getId());
//    }
//
//    @Test
//    @Order(3)
//    public void getCarePlan_success() throws Exception {
//        // Arrange
//        String carePlanId = "careplan-1";
//
//        // Act
//        ApiResponse<CarePlanDto> response = subject.getCarePlanByIdWithHttpInfo(carePlanId);
//
//        // Assert
//        assertEquals(200, response.getStatusCode());
//    }
//
//    @Test
//    @Order(4)
//    public void getCarePlansByCpr_success() throws Exception {
//        // Arrange
//        String cpr = "0101010101";
//        boolean onlyActiveCarePlans = false;
//
//        // Act
//        ApiResponse<List<CarePlanDto>> response = subject.searchCarePlansWithHttpInfo(cpr, null, onlyActiveCarePlans, 1, 10);
//
//        // Assert
//        assertEquals(200, response.getStatusCode());
//    }
//
//    @Test
//    @Order(5)
//    public void getCarePlansWithUnsatisfiedSchedules_success() throws Exception {
//        // Arrange
//        boolean onlyUnsatisfiedSchedules = true;
//        boolean onlyActiveCarePlans = true;
//        int pageNumber = 1;
//        int pageSize = 10;
//
//        // Act
//        ApiResponse<List<CarePlanDto>> response = subject.searchCarePlansWithHttpInfo(null, onlyUnsatisfiedSchedules, onlyActiveCarePlans, pageNumber, pageSize);
//
//        // Assert
//        assertEquals(200, response.getStatusCode());
//    }
//
//    @Test
//    @Order(6)
//    public void patchCarePlan_success() throws Exception {
//        // Arrange
//        String id = "careplan-2";
//        UpdateCareplanRequest request = new UpdateCareplanRequest();
//
//        request.addPlanDefinitionIdsItem("PlanDefinition/plandefinition-1");
//
//        FrequencyDto frequencyDto1 = new FrequencyDto();
//        frequencyDto1.setWeekdays(List.of(FrequencyDto.WeekdaysEnum.TUE));
//        frequencyDto1.setTimeOfDay("11:00");
//        request.addQuestionnairesItem(new QuestionnaireFrequencyPairDto()
//                .id("Questionnaire/questionnaire-1")
//                .frequency(frequencyDto1));
//
//        FrequencyDto frequencyDto2 = new FrequencyDto();
//        frequencyDto2.setWeekdays(List.of(FrequencyDto.WeekdaysEnum.WED));
//        frequencyDto2.setTimeOfDay("11:00");
//        request.addQuestionnairesItem(new QuestionnaireFrequencyPairDto()
//                .id("Questionnaire/questionnaire-2")
//                .frequency(frequencyDto2));
//
//        request.setPatientPrimaryPhone("11223344");
//        request.setPatientSecondaryPhone("55667788");
//        request.setPrimaryRelativeName("Sauron");
//        request.setPrimaryRelativeAffiliation("Fjende");
//
//        request.setPrimaryRelativePrimaryPhone("65412365");
//        request.setPrimaryRelativeSecondaryPhone("77777777");
//
//
//        // Act
//        ApiResponse<Void> response = subject.patchCarePlanWithHttpInfo(id, request);
//
//        // Assert
//        assertEquals(200, response.getStatusCode());
//    }
//
//    @Test
//    @Order(7)
//    public void resolveAlarm_success() throws Exception {
//        // Arrange
//        String id = "careplan-1";
//        String questionnaireId1 = "questionnaire-1";
//
//        // Act
//        ApiResponse<Void> response1 = subject.resolveAlarmWithHttpInfo(id, questionnaireId1);
//
//        // Assert
//        assertEquals(200, response1.getStatusCode());
//    }
//
//    @Test
//    @Order(8)
//    public void completeCarePlan_success() throws Exception {
//        // Arrange
//        String id = "careplan-2";
//
//        // Act
//        ApiResponse<Void> response = subject.completeCarePlanWithHttpInfo(id);
//
//        // Assert
//        assertEquals(200, response.getStatusCode());
//    }
//
//    @Test
//    @Order(9)
//    public void completeCarePlan_twice_success() throws Exception {
//        // Arrange
//        String id = "careplan-2";
//
//        // Act / Assert
//        ApiResponse<Void> firstResponse = subject.completeCarePlanWithHttpInfo(id);
//        assertEquals(200, firstResponse.getStatusCode());
//
//        ApiResponse<Void> secondResponse = subject.completeCarePlanWithHttpInfo(id);
//        assertEquals(200, secondResponse.getStatusCode());
//    }
}
