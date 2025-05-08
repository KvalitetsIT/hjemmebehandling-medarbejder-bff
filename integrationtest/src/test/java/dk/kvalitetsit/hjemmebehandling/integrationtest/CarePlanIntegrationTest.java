package dk.kvalitetsit.hjemmebehandling.integrationtest;

import org.junit.jupiter.api.*;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.api.CarePlanApi;
import org.openapitools.client.model.*;

import java.util.List;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

        CarePlanDto carePlanDto = new CarePlanDto();

        carePlanDto.setPatientDto(new PatientDto());
        Objects.requireNonNull(carePlanDto.getPatientDto()).setCpr("0908060609");

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


        ApiResponse<Void> response = subject.createCarePlanWithHttpInfo(request);


        assertEquals(201, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey("location"));
    }

    @Test
    @Order(2)
    public void createAndGetCarePlan_success() throws Exception {
        String cpr = "4444444444";
        CarePlanDto carePlanDto = new CarePlanDto()
                .patientDto(new PatientDto().cpr(cpr));

        QuestionnaireDto questionnaireDto = new QuestionnaireDto();
        questionnaireDto.setId("Questionnaire/questionnaire-1");

        FrequencyDto frequencyDto = new FrequencyDto()
                .weekdays(List.of(FrequencyDto.WeekdaysEnum.TUE, FrequencyDto.WeekdaysEnum.FRI))
                .timeOfDay("04:00");

        var wrapper = new QuestionnaireWrapperDto()
                .questionnaire(questionnaireDto)
                .frequency(frequencyDto);

        carePlanDto.setQuestionnaires(List.of(wrapper));

        PlanDefinitionDto planDefinitionDto = new PlanDefinitionDto();
        planDefinitionDto.setId("PlanDefinition/plandefinition-1");
        carePlanDto.setPlanDefinitions(List.of(planDefinitionDto));

        CreateCarePlanRequest request = new CreateCarePlanRequest()
                .carePlan(carePlanDto);

        ApiResponse<Void> createResponse = subject.createCarePlanWithHttpInfo(request);

        String id = createResponse.getHeaders().get("Location").getFirst();
        id = id.substring(id.lastIndexOf('/') + 1);
        ApiResponse<CarePlanDto> getResponse = subject.getCarePlanByIdWithHttpInfo(id);

        assertEquals(201, createResponse.getStatusCode());
        assertTrue(createResponse.getHeaders().containsKey("location"));

        assertEquals(200, getResponse.getStatusCode());
        assertEquals("CarePlan/" + id, getResponse.getData().getId());
    }

    @Test
    @Order(3)
    public void getCarePlan_success() throws Exception {
        String carePlanId = "careplan-1";
        ApiResponse<CarePlanDto> response = subject.getCarePlanByIdWithHttpInfo(carePlanId);
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @Order(4)
    public void getCarePlansByCpr_success() throws Exception {
        String cpr = "0101010101";
        boolean onlyActiveCarePlans = false;
        ApiResponse<List<CarePlanDto>> response = subject.searchCarePlansWithHttpInfo(cpr, null, onlyActiveCarePlans, 1, 10);
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @Order(5)
    public void getCarePlansWithUnsatisfiedSchedules_success() throws Exception {

        boolean onlyUnsatisfiedSchedules = true;
        boolean onlyActiveCarePlans = true;
        int pageNumber = 1;
        int pageSize = 10;

        ApiResponse<List<CarePlanDto>> response = subject.searchCarePlansWithHttpInfo(null, onlyUnsatisfiedSchedules, onlyActiveCarePlans, pageNumber, pageSize);

        assertEquals(200, response.getStatusCode());
    }

    @Test
    @Order(6)
    public void patchCarePlan_success() throws Exception {

        String id = "careplan-2";
        UpdateCareplanRequest request = new UpdateCareplanRequest()
                .addPlanDefinitionIdsItem("PlanDefinition/plandefinition-1")
                .patientPrimaryPhone("11223344")
                .patientSecondaryPhone("55667788")
                .primaryRelativeName("Sauron")
                .primaryRelativeAffiliation("Fjende")
                .primaryRelativePrimaryPhone("65412365")
                .primaryRelativeSecondaryPhone("77777777")
                .addQuestionnairesItem(
                        new QuestionnaireFrequencyPairDto()
                                .id("Questionnaire/questionnaire-1")
                                .frequency(new FrequencyDto()
                                        .weekdays(List.of(FrequencyDto.WeekdaysEnum.TUE))
                                        .timeOfDay("11:00")))
                .addQuestionnairesItem(new QuestionnaireFrequencyPairDto()
                        .id("Questionnaire/questionnaire-2")
                        .frequency(new FrequencyDto()
                                .weekdays(List.of(FrequencyDto.WeekdaysEnum.WED))
                                .timeOfDay("11:00")));

        ApiResponse<Void> response = subject.patchCarePlanWithHttpInfo(id, request);

        assertEquals(200, response.getStatusCode());
    }

    @Test
    @Order(7)
    public void resolveAlarm_success() throws Exception {
        String id = "careplan-1";
        String questionnaireId1 = "questionnaire-1";
        ApiResponse<Void> response1 = subject.resolveAlarmWithHttpInfo(id, questionnaireId1);
        assertEquals(200, response1.getStatusCode());
    }

    @Test
    @Order(8)
    public void completeCarePlan_success() throws Exception {
        String id = "careplan-2";
        ApiResponse<Void> response = subject.completeCarePlanWithHttpInfo(id);
        assertEquals(200, response.getStatusCode());
    }

    @Test
    @Order(9)
    public void completeCarePlan_twice_success() throws Exception {
        String id = "careplan-2";

        ApiResponse<Void> firstResponse = subject.completeCarePlanWithHttpInfo(id);
        assertEquals(200, firstResponse.getStatusCode());

        ApiResponse<Void> secondResponse = subject.completeCarePlanWithHttpInfo(id);
        assertEquals(200, secondResponse.getStatusCode());
    }
}
