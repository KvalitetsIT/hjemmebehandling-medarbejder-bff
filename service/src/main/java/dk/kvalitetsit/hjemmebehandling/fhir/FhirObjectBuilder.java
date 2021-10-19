package dk.kvalitetsit.hjemmebehandling.fhir;

import dk.kvalitetsit.hjemmebehandling.service.model.FrequencyModel;
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

            // Map the action to an Activity
            CarePlan.CarePlanActivityComponent activity = new CarePlan.CarePlanActivityComponent();

            CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();

            detail.setInstantiatesUri(List.of(action.getDefinitionUriType()));
            detail.setStatus(CarePlan.CarePlanActivityStatus.NOTSTARTED);
            detail.setScheduled(action.getTiming());

            activity.setDetail(detail);

            carePlan.addActivity(activity);
        }

        return carePlan;
    }

    public void setQuestionnairesForCarePlan(CarePlan carePlan, List<Questionnaire> questionnaires, Map<String, Timing> frequencies) {
        // Clear existing Activity list
        carePlan.getActivity().clear();

        // Map each questionnaire to an Activity
        for(Questionnaire questionnaire : questionnaires) {
            // Map the action to an Activity
            CarePlan.CarePlanActivityComponent activity = new CarePlan.CarePlanActivityComponent();

            CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();

            detail.setInstantiatesUri(List.of(new UriType(questionnaire.getIdElement().toVersionless().getValue())));
            detail.setStatus(CarePlan.CarePlanActivityStatus.NOTSTARTED);

            String questionnaireId = questionnaire.getIdElement().getIdPart();
            detail.setScheduled(frequencies.get(questionnaireId));

            activity.setDetail(detail);

            carePlan.addActivity(activity);
        }
    }
}
