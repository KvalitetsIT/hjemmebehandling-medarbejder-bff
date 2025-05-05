package dk.kvalitetsit.hjemmebehandling.model;


import dk.kvalitetsit.hjemmebehandling.fhir.FhirUtils;
import org.hl7.fhir.r4.model.ResourceType;

public sealed interface QualifiedId permits QualifiedId.PatientId, QualifiedId.CarePlanId, QualifiedId.PersonId, QualifiedId.PlanDefinitionId, QualifiedId.QuestionnaireId, QualifiedId.QuestionnaireResponseId, QualifiedId.PractitionerId, QualifiedId.OrganizationId, QualifiedId.ValueSetId{

    static void validateUnqualifiedId(String unqualified) throws IllegalArgumentException {
        if (!FhirUtils.isPlainId (unqualified)) {
            throw new IllegalArgumentException("Provided id was not a plain id: " + unqualified);
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
            case ValueSet -> new ValueSetId(id);
            default -> throw new IllegalStateException("Unexpected value: " + qualifier);
        };
    }

    static QualifiedId from(ResourceType qualifier, String unqualified) throws IllegalArgumentException {
        return switch (qualifier) {
            case Patient -> new PatientId (unqualified);
            case Person -> new PersonId (unqualified);
            case PlanDefinition -> new PlanDefinitionId (unqualified);
            case Questionnaire -> new QuestionnaireId (unqualified);
            case QuestionnaireResponse -> new QuestionnaireResponseId (unqualified);
            case Practitioner -> new PractitionerId (unqualified);
            case CarePlan -> new CarePlanId (unqualified);
            case ValueSet -> new ValueSetId (unqualified);
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

    String unqualified();

    ResourceType qualifier();

    default String qualified() {
        return qualifier() + "/" + unqualified();
    }


    record PatientId(String unqualified) implements QualifiedId {

        public PatientId {
            validateUnqualifiedId(unqualified);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Patient;
        }
    }

    record CarePlanId(String unqualified) implements QualifiedId {

        public CarePlanId {
            validateUnqualifiedId (unqualified);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.CarePlan;
        }
    }


    record PersonId(String unqualified) implements QualifiedId {

        public PersonId {
            validateUnqualifiedId (unqualified);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Person;
        }
    }


    record PlanDefinitionId(String unqualified) implements QualifiedId {

        public PlanDefinitionId {
            validateUnqualifiedId (unqualified);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.PlanDefinition;
        }
    }


    record QuestionnaireId(String unqualified) implements QualifiedId {

        public QuestionnaireId {
            validateUnqualifiedId (unqualified);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Questionnaire;
        }
    }


    record QuestionnaireResponseId(String unqualified) implements QualifiedId {

        public QuestionnaireResponseId {
            validateUnqualifiedId (unqualified);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.QuestionnaireResponse;
        }
    }

    record PractitionerId(String unqualified) implements QualifiedId {

        public PractitionerId {
            validateUnqualifiedId (unqualified);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Practitioner;
        }
    }


    record OrganizationId(String unqualified) implements QualifiedId {

        public OrganizationId {
            validateUnqualifiedId (unqualified);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.Organization;
        }
    }

    record ValueSetId(String unqualified) implements QualifiedId {

        public ValueSetId {
            validateUnqualifiedId (unqualified);
        }

        @Override
        public ResourceType qualifier() {
            return ResourceType.ValueSet;
        }
    }
}
