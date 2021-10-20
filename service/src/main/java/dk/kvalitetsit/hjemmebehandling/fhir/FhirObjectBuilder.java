package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
public class FhirObjectBuilder {
    public CarePlan buildCarePlan(Patient patient, PlanDefinition planDefinition) {
        CarePlan carePlan = new CarePlan();

        carePlan.setTitle(planDefinition.getTitle());

        Reference subjectReference = new Reference(patient.getId());
        carePlan.setSubject(subjectReference);

        Period period = new Period();
        period.setStart(Date.from(Instant.now()));
        carePlan.setPeriod(period);

        // TODO - figure out how to set the author-field (possibly look up Practitioner based on token?).

        // TODO - figure out how to determine and set CareTeam (again, possibly based on the token?)

        // Each action on the PlanDefinition is mapped to an activity on the CarePlan
        for(PlanDefinition.PlanDefinitionActionComponent action : planDefinition.getAction()) {
            // Figure out whether this action denotes a Questionnaire or not.
            if(action.getDefinition() == null || !action.getDefinition().primitiveValue().contains("Questionnaire")) {
                // Not a Questionnaire reference.
                continue;
            }

            carePlan.addActivity(buildActivity(action));
        }

        return carePlan;
    }

    public void setQuestionnairesForCarePlan(CarePlan carePlan, List<Questionnaire> questionnaires, Map<String, Timing> frequencies) {
        // Clear existing Activity list
        carePlan.getActivity().clear();

        // Map each questionnaire to an Activity
        for(Questionnaire questionnaire : questionnaires) {
            carePlan.addActivity(buildActivity(questionnaire, frequencies));
        }
    }

    private CarePlan.CarePlanActivityComponent buildActivity(PlanDefinition.PlanDefinitionActionComponent action) {
        CanonicalType instantiatesCanonical = action.getDefinitionCanonicalType();
        Type timing = action.getTiming();

        return buildActivity(instantiatesCanonical, timing);
    }

    private CarePlan.CarePlanActivityComponent buildActivity(Questionnaire questionnaire, Map<String, Timing> frequencies) {
        CanonicalType instantiatesCanonical = getInstantiatesCanonical(questionnaire);

        Timing timing = getTiming(questionnaire, frequencies);

        return buildActivity(instantiatesCanonical, timing);
    }

    private CanonicalType getInstantiatesCanonical(Questionnaire questionnaire) {
        return new CanonicalType(questionnaire.getIdElement().toVersionless().getValue());
    }

    private Timing getTiming(Questionnaire questionnaire, Map<String, Timing> frequencies) {
        String questionnaireId = questionnaire.getIdElement().getIdPart();
        return frequencies.get(questionnaireId);
    }

    private CarePlan.CarePlanActivityComponent buildActivity(CanonicalType instantiatesCanonical, Type timing) {
        CarePlan.CarePlanActivityComponent activity = new CarePlan.CarePlanActivityComponent();

        activity.setDetail(buildDetail(instantiatesCanonical, timing));

        return activity;
    }

    private CarePlan.CarePlanActivityDetailComponent buildDetail(CanonicalType instantiatesCanonical, Type timing) {
        CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();

        detail.setInstantiatesCanonical(List.of(instantiatesCanonical));
        detail.setStatus(CarePlan.CarePlanActivityStatus.NOTSTARTED);
        detail.setScheduled(timing);

        return detail;
    }
}
