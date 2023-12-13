package dk.kvalitetsit.hjemmebehandling.api.dto;

import dk.kvalitetsit.hjemmebehandling.api.dto.questionnaire.question.BaseQuestionDto;
import dk.kvalitetsit.hjemmebehandling.constants.ExaminationStatus;
import dk.kvalitetsit.hjemmebehandling.constants.TriagingCategory;
import dk.kvalitetsit.hjemmebehandling.mapping.ToModel;
import dk.kvalitetsit.hjemmebehandling.model.QuestionnaireResponseModel;

import java.time.Instant;
import java.util.List;

public class QuestionnaireResponseDto extends BaseDto implements ToModel<QuestionnaireResponseModel> {
    private String questionnaireId;
    private String carePlanId;
    private String questionnaireName;
    private List<BaseQuestionDto<?>> questions;
    private Instant answered;
    private ExaminationStatus examinationStatus;
    private TriagingCategory triagingCategory;
    private PatientDto patient;
    private String planDefinitionTitle;

    public String getQuestionnaireId() {
        return questionnaireId;
    }

    public void setQuestionnaireId(String questionnaireId) {
        this.questionnaireId = questionnaireId;
    }

    public String getCarePlanId() {
        return carePlanId;
    }

    public void setCarePlanId(String carePlanId) {
        this.carePlanId = carePlanId;
    }

    public String getQuestionnaireName() {
        return questionnaireName;
    }

    public void setQuestionnaireName(String questionnaireName) {
        this.questionnaireName = questionnaireName;
    }

    public List<BaseQuestionDto<?>> getQuestions() {
        return questions;
    }

    public void setQuestions(List<BaseQuestionDto<?>> questions) {
        this.questions = questions;
    }

    public Instant getAnswered() {
        return answered;
    }

    public void setAnswered(Instant answered) {
        this.answered = answered;
    }

    public ExaminationStatus getExaminationStatus() {
        return examinationStatus;
    }

    public void setExaminationStatus(ExaminationStatus examinationStatus) {
        this.examinationStatus = examinationStatus;
    }

    public TriagingCategory getTriagingCategory() {
        return triagingCategory;
    }

    public void setTriagingCategory(TriagingCategory triagingCategory) {
        this.triagingCategory = triagingCategory;
    }

    public PatientDto getPatient() {
        return patient;
    }

    public void setPatient(PatientDto patient) {
        this.patient = patient;
    }

    public String getPlanDefinitionTitle() {
        return planDefinitionTitle;
    }

    public void setPlanDefinitionTitle(String planDefinitionTitle) {
        this.planDefinitionTitle = planDefinitionTitle;
    }

    @Override
    public QuestionnaireResponseModel toModel() {
        return null;
    }
}
