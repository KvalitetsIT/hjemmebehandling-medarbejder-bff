package dk.kvalitetsit.hjemmebehandling.model;


import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import org.hl7.fhir.r4.model.ResourceType;

public sealed interface QualifiedId permits
        QualifiedId.PatientId,
        QualifiedId.CarePlanId,
        QualifiedId.PersonId,
        QualifiedId.PlanDefinitionId,
        QualifiedId.QuestionnaireId,
        QualifiedId.QuestionnaireResponseId,
        QualifiedId.PractitionerId {

    static void validateUnqualifiedId(String unqualifiedId) throws IllegalArgumentException {
        if (!FhirUtils.isPlainId(unqualifiedId)) {
            throw new IllegalArgumentException("Provided id was not a plain id: " + unqualifiedId);
        }
    }

    static QualifiedId from(String qualifiedId) throws IllegalArgumentException {
        var qualifier = extractQualifier(qualifiedId);
        var id = extractUnqualifiedId(qualifiedId);

        return switch (qualifier) {
            case Patient -> new PatientId(id);
            case Person -> new PersonId(id);
            case PlanDefinition -> new PlanDefinitionId(id);
            case Questionnaire -> new QuestionnaireId(id);
            case QuestionnaireResponse -> new QuestionnaireResponseId(id);
            case Practitioner -> new PractitionerId(id);
            case CarePlan -> new CarePlanId(id);
            default -> throw new IllegalStateException("Unexpected value: " + qualifier);
        };

    }

    private static String extractUnqualifiedId(String qualifiedId) {
        var parts = qualifiedId.split("/");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Cannot unqualify id: " + qualifiedId + "! Illegal format");
        }
        if (!FhirUtils.isPlainId(parts[1])) {
            throw new IllegalArgumentException("Cannot unqualify id: " + qualifiedId + "! Illegal id");
        }
        return parts[1];
    }

    private static ResourceType extractQualifier(String qualifiedId) {
        var parts = qualifiedId.split("/");
        return Enum.valueOf(ResourceType.class, parts[0]);
    }

    String id();

    ResourceType qualifier();

    default String qualifiedId() {
        return qualifier() + "/" + id();
    }

    default String unQualifiedId() {
        return qualifier() + "/" + id();
    }

    record PatientId(String id) implements QualifiedId {

        public PatientId {
            validateUnqualifiedId(id);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Patient;
        }
    }

    record CarePlanId(String id) implements QualifiedId {

        public CarePlanId {
            validateUnqualifiedId(id);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Patient;
        }
    }


    record PersonId(String id) implements QualifiedId {

        public PersonId {
            validateUnqualifiedId(id);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Patient;
        }
    }


    record PlanDefinitionId(String id) implements QualifiedId {

        public PlanDefinitionId {
            validateUnqualifiedId(id);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Patient;
        }
    }


    record QuestionnaireId(String id) implements QualifiedId {

        public QuestionnaireId {
            validateUnqualifiedId(id);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Patient;
        }
    }


    record QuestionnaireResponseId(String id) implements QualifiedId {

        public QuestionnaireResponseId {
            validateUnqualifiedId(id);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Patient;
        }
    }

    record PractitionerId(String id) implements QualifiedId {

        public PractitionerId {
            validateUnqualifiedId(id);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Patient;
        }
    }


}
