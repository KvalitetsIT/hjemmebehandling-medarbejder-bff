package dk.kvalitetsit.hjemmebehandling.fhir;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import dk.kvalitetsit.hjemmebehandling.api.question.Option;
import dk.kvalitetsit.hjemmebehandling.constants.EnableWhenOperator;
import dk.kvalitetsit.hjemmebehandling.constants.PlanDefinitionStatus;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionnaireStatus;
import dk.kvalitetsit.hjemmebehandling.model.*;
import dk.kvalitetsit.hjemmebehandling.service.exception.ServiceException;
import org.hl7.fhir.r4.model.*;

import org.hl7.fhir.r4.model.Enumeration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import dk.kvalitetsit.hjemmebehandling.constants.AnswerType;
import dk.kvalitetsit.hjemmebehandling.constants.CarePlanStatus;
import dk.kvalitetsit.hjemmebehandling.constants.QuestionType;
import dk.kvalitetsit.hjemmebehandling.constants.Systems;
import dk.kvalitetsit.hjemmebehandling.model.answer.AnswerModel;
import dk.kvalitetsit.hjemmebehandling.model.question.QuestionModel;
import dk.kvalitetsit.hjemmebehandling.types.Weekday;
import dk.kvalitetsit.hjemmebehandling.util.DateProvider;

@Component
public class FhirMapper {
    @Autowired
    private DateProvider dateProvider;

    public CarePlan mapCarePlanModel(CarePlanModel carePlanModel) {
        CarePlan carePlan = new CarePlan();

        mapBaseAttributesToFhir(carePlan, carePlanModel);

        carePlan.setTitle(carePlanModel.getTitle());
        carePlan.setStatus(Enum.valueOf(CarePlan.CarePlanStatus.class, carePlanModel.getStatus().toString()));
        carePlan.setCreated(Date.from(carePlanModel.getCreated()));
        if(carePlanModel.getStartDate() != null) {
            carePlan.setPeriod(new Period());
            carePlan.getPeriod().setStart(Date.from(carePlanModel.getStartDate()));
        }
        carePlan.addExtension(ExtensionMapper.mapCarePlanSatisfiedUntil(carePlanModel.getSatisfiedUntil()));

        // Set the subject
        if(carePlanModel.getPatient().getId() != null) {
            carePlan.setSubject(new Reference(carePlanModel.getPatient().getId().toString()));
        }

        // Map questionnaires to activities
        if(carePlanModel.getQuestionnaires() != null) {
            carePlan.setActivity(carePlanModel.getQuestionnaires()
                    .stream()
                    .map(this::buildCarePlanActivity)
                    .collect(Collectors.toList()));
        }

        if(carePlanModel.getPlanDefinitions() != null) {
            // Add references to planDefinitions
            carePlan.setInstantiatesCanonical(carePlanModel.getPlanDefinitions()
                    .stream()
                    .map(pd -> new CanonicalType(pd.getId().toString()))
                    .collect(Collectors.toList()));
        }

        return carePlan;
    }

    public CarePlanModel mapCarePlan(CarePlan carePlan, FhirLookupResult lookupResult, String organisationId)  {
        CarePlanModel carePlanModel = new CarePlanModel();

        mapBaseAttributesToModel(carePlanModel, carePlan);

        carePlanModel.setTitle(carePlan.getTitle());
        carePlanModel.setStatus(Enum.valueOf(CarePlanStatus.class, carePlan.getStatus().toString()));
        carePlanModel.setCreated(carePlan.getCreated().toInstant());
        carePlanModel.setStartDate(carePlan.getPeriod().getStart().toInstant());
        if(carePlan.getPeriod().getEnd() != null) {
            carePlanModel.setEndDate(carePlan.getPeriod().getEnd().toInstant());
        }


        String patientId = carePlan.getSubject().getReference();
        Patient patient = lookupResult.getPatient(patientId).orElseThrow(() -> new IllegalStateException(String.format("Could not look up Patient for CarePlan %s!", carePlanModel.getId())));
        carePlanModel.setPatient(mapPatient(patient, organisationId));

        carePlanModel.setPlanDefinitions(new ArrayList<>());
        for(var ic : carePlan.getInstantiatesCanonical()) {
            var planDefinition = lookupResult
                .getPlanDefinition(ic.getValue())
                .orElseThrow(() -> new IllegalStateException(String.format("Could not look up PlanDefinition for CarePlan %s!", carePlanModel.getId())));
            carePlanModel.getPlanDefinitions().add(mapPlanDefinition(planDefinition, lookupResult));
        }

        carePlanModel.setQuestionnaires(new ArrayList<>());
        for(var activity : carePlan.getActivity()) {
            String questionnaireId = activity.getDetail().getInstantiatesCanonical().get(0).getValue();
            var questionnaire = lookupResult
                    .getQuestionnaire(questionnaireId)
                    .orElseThrow(() -> new IllegalStateException(String.format("Could not look up Questionnaire for CarePlan %s!", carePlanModel.getId())));

            var questionnaireModel = mapQuestionnaire(questionnaire);
            var frequencyModel = mapTiming(activity.getDetail().getScheduledTiming());

            var wrapper = new QuestionnaireWrapperModel();
            wrapper.setQuestionnaire(questionnaireModel);
            wrapper.setFrequency(frequencyModel);
            wrapper.setSatisfiedUntil(ExtensionMapper.extractActivitySatisfiedUntil(activity.getDetail().getExtension()));

            // get thresholds from questionnaire
            List<ThresholdModel> questionnaireThresholds = ExtensionMapper.extractThresholds(
                questionnaire.getItem().stream()
                    .flatMap(q -> q.getExtensionsByUrl(Systems.THRESHOLD).stream())
                    .collect(Collectors.toList())
            );
            wrapper.getThresholds().addAll(questionnaireThresholds);

            // find thresholds from plandefinition
            Optional<List<ThresholdModel>> thresholds = carePlanModel.getPlanDefinitions().stream()
                .flatMap(p -> p.getQuestionnaires().stream())
                .filter(q -> q.getQuestionnaire().getId().equals(questionnaireModel.getId()))
                .findFirst()
                .map(QuestionnaireWrapperModel::getThresholds);
            thresholds.ifPresent(thresholdModels -> wrapper.getThresholds().addAll(thresholdModels));

            carePlanModel.getQuestionnaires().add(wrapper);
        }


        carePlanModel.setSatisfiedUntil(ExtensionMapper.extractCarePlanSatisfiedUntil(carePlan.getExtension()));

        String organizationId = ExtensionMapper.extractOrganizationId(carePlan.getExtension());
        Organization organization = lookupResult.getOrganization(organizationId)
                .orElseThrow(() -> new IllegalStateException(String.format("Organization with id %s was not present when trying to map careplan %s!", organizationId, carePlan.getId())));
        carePlanModel.setDepartmentName(organization.getName());

        return carePlanModel;
    }

    public Timing mapFrequencyModel(FrequencyModel frequencyModel) {
        Timing timing = new Timing();

        Timing.TimingRepeatComponent repeat = new Timing.TimingRepeatComponent();

        EnumFactory<Timing.DayOfWeek> factory = new Timing.DayOfWeekEnumFactory();
        repeat.setDayOfWeek(frequencyModel.getWeekdays().stream().map(w -> new Enumeration<>(factory, w.toString().toLowerCase())).collect(Collectors.toList()));
        repeat.setTimeOfDay(List.of(new TimeType(frequencyModel.getTimeOfDay().toString())));
        timing.setRepeat(repeat);

        return timing;
    }

    public Patient mapPatientModel(PatientModel patientModel) {
        Patient patient = new Patient();

        // Id may be null, in case we are creating the patient.
        if(patientModel.getId() != null) {
            patient.setId(patientModel.getId().toString());
        }

        var name = buildName(patientModel.getGivenName(), patientModel.getFamilyName());
        patient.addName(name);

        patient.getIdentifier().add(makeCprIdentifier(patientModel.getCpr()));
        
        patient.addExtension(ExtensionMapper.mapCustomUserId(patientModel.getCustomUserId()));
        patient.addExtension(ExtensionMapper.mapCustomUserName(patientModel.getCustomUserName()));

        if(patientModel.getContactDetails() != null) {
            var contactDetails = patientModel.getContactDetails();

            var address = buildAddress(contactDetails);
            patient.addAddress(address);

            if(contactDetails.getPrimaryPhone() != null) {
                var primaryContactPoint = buildContactPoint(contactDetails.getPrimaryPhone(), 1);
                patient.addTelecom(primaryContactPoint);
            }

            if(contactDetails.getSecondaryPhone() != null) {
                var secondaryContactPoint = buildContactPoint(contactDetails.getSecondaryPhone(), 2);
                patient.addTelecom(secondaryContactPoint);
            }
        }

        if(patientModel.getPrimaryContact().getName() != null) {
            var contact = new Patient.ContactComponent();

            var contactName = buildName(patientModel.getPrimaryContact().getName());
            contact.setName(contactName);
            var organisation = new Reference();
            organisation.setReference(patientModel.getPrimaryContact().getOrganisation());
            contact.setOrganization(organisation);

            if(patientModel.getPrimaryContact().getAffiliation() != null) {

                var codeableConcept = new CodeableConcept();
                codeableConcept.setText(patientModel.getPrimaryContact().getAffiliation());
                contact.setRelationship(List.of(codeableConcept));
            }

            if(patientModel.getPrimaryContact().getContactDetails() != null) {
                var primaryRelativeContactDetails = patientModel.getPrimaryContact().getContactDetails();
                if(primaryRelativeContactDetails.getPrimaryPhone() != null) {
                    var relativePrimaryContactPoint = buildContactPoint(primaryRelativeContactDetails.getPrimaryPhone(), 1);
                    contact.addTelecom(relativePrimaryContactPoint);
                }

                if(primaryRelativeContactDetails.getSecondaryPhone() != null) {
                    var relativeSecondaryContactPoint = buildContactPoint(primaryRelativeContactDetails.getSecondaryPhone(), 2);
                    contact.addTelecom(relativeSecondaryContactPoint);
                }
            }

            patient.addContact(contact);
        }

        return patient;
    }

    public PatientModel mapPatient(Patient patient, String organizationId) {
        PatientModel patientModel = new PatientModel();

        patientModel.setId(extractId(patient));
        patientModel.setCustomUserId(ExtensionMapper.extractCustomUserId(patient.getExtension()));
        patientModel.setCustomUserName(ExtensionMapper.extractCustomUserName(patient.getExtension()));
        patientModel.setGivenName(extractGivenNames(patient));
        patientModel.setFamilyName(extractFamilyName(patient));
        patientModel.setCpr(extractCpr(patient));
        patientModel.setContactDetails(extractPatientContactDetails(patient));

        if(patient.getContact() != null && !patient.getContact().isEmpty()) {

            if(organizationId == null) throw new IllegalStateException("Mapping contact is only possible while the organization id is known");

            var optionalContact = patient.getContact().stream()
                    .filter(c -> c.getOrganization().getReference().equals(organizationId))
                    .findFirst();

            if (optionalContact.isPresent()){
                var contact = optionalContact.get();
                patientModel.getPrimaryContact().setName(contact.getName().getText());
                patientModel.getPrimaryContact().setOrganisation(contact.getOrganization().getReference());
                patientModel.getPrimaryContact().setAffiliation( contact.getRelationshipFirstRep().getText() );

                // Extract phone numbers
                if(contact.getTelecom() != null && !contact.getTelecom().isEmpty()) {
                    var primaryRelativeContactDetails = new ContactDetailsModel();

                    for(var telecom : contact.getTelecom()) {
                        if(telecom.getRank() == 1) {
                            primaryRelativeContactDetails.setPrimaryPhone(telecom.getValue());
                        }
                        if(telecom.getRank() == 2) {
                            primaryRelativeContactDetails.setSecondaryPhone(telecom.getValue());
                        }
                    }

                    patientModel.getPrimaryContact().setContactDetails(primaryRelativeContactDetails);
                }

            }
        }

        return patientModel;
    }

    public PlanDefinitionModel mapPlanDefinition(PlanDefinition planDefinition, FhirLookupResult lookupResult) {
        PlanDefinitionModel planDefinitionModel = new PlanDefinitionModel();

        mapBaseAttributesToModel(planDefinitionModel, planDefinition);

        planDefinitionModel.setName(planDefinition.getName());
        planDefinitionModel.setTitle(planDefinition.getTitle());
        planDefinitionModel.setStatus(Enum.valueOf(PlanDefinitionStatus.class, planDefinition.getStatus().toString()));
        if(planDefinition.getDate() != null)
            planDefinitionModel.setCreated(planDefinition.getDate().toInstant());

        if(planDefinition.getMeta().getLastUpdated() != null)
            planDefinitionModel.setLastUpdated(planDefinition.getMeta().getLastUpdated().toInstant());

        // Map actions to questionnaires, along with their thresholds
        planDefinitionModel.setQuestionnaires(planDefinition.getAction().stream().map(a -> mapPlanDefinitionAction(a, lookupResult)).collect(Collectors.toList()));

        return planDefinitionModel;
    }

    public Questionnaire mapQuestionnaireModel(QuestionnaireModel questionnaireModel) {
        Questionnaire questionnaire = new Questionnaire();

        mapBaseAttributesToFhir(questionnaire, questionnaireModel);

        questionnaire.setTitle(questionnaireModel.getTitle());
        questionnaire.setStatus( mapQuestionnaireStatus(questionnaireModel.getStatus()) );
        questionnaire.getItem().addAll(questionnaireModel.getQuestions().stream()
            .map(this::mapQuestionnaireItem)
            .collect(Collectors.toList()));
        if (questionnaireModel.getCallToAction() != null) {
            questionnaire.getItem().add(mapQuestionnaireCallToActions(questionnaireModel.getCallToAction()));
        }
        questionnaire.setVersion(questionnaireModel.getVersion());

        return questionnaire;
    }



    public QuestionnaireModel mapQuestionnaire(Questionnaire questionnaire) {
        QuestionnaireModel questionnaireModel = new QuestionnaireModel();

        mapBaseAttributesToModel(questionnaireModel, questionnaire);
        questionnaireModel.setLastUpdated(questionnaire.getMeta().getLastUpdated());
        questionnaireModel.setTitle(questionnaire.getTitle());
        questionnaireModel.setStatus( mapQuestionnaireStatus(questionnaire.getStatus()) );
        questionnaireModel.setQuestions(questionnaire.getItem().stream()
            .filter(q -> !q.getLinkId().equals(Systems.CALL_TO_ACTION_LINK_ID)) // filter out call-to-action's
            .map(this::mapQuestionnaireItem).collect(Collectors.toList()));
        questionnaireModel.setCallToAction(questionnaire.getItem().stream()
                .filter(q -> q.getLinkId().equals(Systems.CALL_TO_ACTION_LINK_ID)) // process call-to-action's
                .findFirst()
                .map(this::mapQuestionnaireItem)
                .orElse(null));
        questionnaireModel.setVersion(questionnaire.getVersion());
        return questionnaireModel;
    }

    private Enumerations.PublicationStatus mapQuestionnaireStatus(QuestionnaireStatus status) {
        switch (status) {
            case ACTIVE:
                return Enumerations.PublicationStatus.ACTIVE;
            case DRAFT:
                return Enumerations.PublicationStatus.DRAFT;
            case RETIRED:
                return Enumerations.PublicationStatus.RETIRED;
            default:
                throw new IllegalArgumentException(String.format("Don't know how to map QuestionnaireStatus %s", status.toString()));
        }
    }

    private QuestionnaireStatus mapQuestionnaireStatus(Enumerations.PublicationStatus status) {
        switch (status) {
            case ACTIVE:
                return QuestionnaireStatus.ACTIVE;
            case DRAFT:
                return QuestionnaireStatus.DRAFT;
            case RETIRED:
                return QuestionnaireStatus.RETIRED;
            default:
                throw new IllegalArgumentException(String.format("Don't know how to map Questionnaire.status %s", status.toString()));
        }
    }

    public QuestionnaireResponse mapQuestionnaireResponseModel(QuestionnaireResponseModel questionnaireResponseModel) {
        QuestionnaireResponse questionnaireResponse = new QuestionnaireResponse();

        mapBaseAttributesToFhir(questionnaireResponse, questionnaireResponseModel);

        questionnaireResponse.setQuestionnaire(questionnaireResponseModel.getQuestionnaireId().toString());
        for(var questionAnswerPair : questionnaireResponseModel.getQuestionAnswerPairs()) {
            questionnaireResponse.getItem().add(getQuestionnaireResponseItem(questionAnswerPair.getAnswer()));
        }
        questionnaireResponse.setBasedOn(List.of(new Reference(questionnaireResponseModel.getCarePlanId().toString())));
        questionnaireResponse.setAuthor(new Reference(questionnaireResponseModel.getAuthorId().toString()));
        questionnaireResponse.setSource(new Reference(questionnaireResponseModel.getSourceId().toString()));
        questionnaireResponse.setAuthored(Date.from(questionnaireResponseModel.getAnswered()));
        questionnaireResponse.getExtension().add(ExtensionMapper.mapExaminationStatus(questionnaireResponseModel.getExaminationStatus()));
        if (questionnaireResponseModel.getExaminationAuthor() != null) {
            questionnaireResponse.getExtension().add(ExtensionMapper.mapExaminationAuthor(questionnaireResponseModel.getExaminationAuthor()));
        }
        questionnaireResponse.getExtension().add(ExtensionMapper.mapTriagingCategory(questionnaireResponseModel.getTriagingCategory()));
        questionnaireResponse.setSubject(new Reference(questionnaireResponseModel.getPatient().getId().toString()));

        return questionnaireResponse;
    }


    public QuestionnaireResponseModel mapQuestionnaireResponse(QuestionnaireResponse questionnaireResponse, FhirLookupResult lookupResult, String organisationId) {
        QuestionnaireResponseModel questionnaireResponseModel = constructQuestionnaireResponse(questionnaireResponse, lookupResult, organisationId);

        Questionnaire questionnaire = lookupResult.getQuestionnaire(questionnaireResponse.getQuestionnaire())
                .orElseThrow(() -> new IllegalStateException(String.format("No Questionnaire found with id %s!", questionnaireResponse.getQuestionnaire())));

        // Populate questionAnswerMap
        List<QuestionAnswerPairModel> answers = new ArrayList<>();

        for(var item : questionnaireResponse.getItem()) {
            QuestionModel question;
            try {
                question = getQuestion(questionnaire, item.getLinkId());
            }   catch (IllegalStateException e) {
                // Corresponding question could not be found in the current/newest questionnaire
                // ignore
                // Or use the overloaded version which runs thought historical versions as well
                // and returns deprecated questions
                question = null;
            }
            AnswerModel answer = getAnswer(item);
            answers.add( new QuestionAnswerPairModel(question, answer));
        }

        questionnaireResponseModel.setQuestionAnswerPairs(answers);

        return questionnaireResponseModel;
    }
    public QuestionnaireResponseModel mapQuestionnaireResponse(
            QuestionnaireResponse questionnaireResponse,
            FhirLookupResult lookupResult,
            List<Questionnaire> historicalQuestionnaires,
            String organisationId
    ) {
        if (historicalQuestionnaires == null) return mapQuestionnaireResponse(questionnaireResponse, lookupResult, organisationId);

        QuestionnaireResponseModel questionnaireResponseModel = constructQuestionnaireResponse(questionnaireResponse, lookupResult, organisationId);

        // Populate questionAnswerMap
        List<QuestionAnswerPairModel> answers = new ArrayList<>();

        //Look through all the given questionnaires
        for(var item : questionnaireResponse.getItem()) {
            QuestionModel question = null;
            boolean deprecated = false;
            int i = 0;


            for (Questionnaire q : historicalQuestionnaires) {
                if (i > 0) deprecated = true;
                boolean hasNext = i < historicalQuestionnaires.size() - 1;
                try {
                    question = getQuestion(q, item.getLinkId());
                    question.setDeprecated(deprecated);
                    break;
                } catch (IllegalStateException e) {
                    if (!hasNext)
                        throw new IllegalStateException("Corresponding question could not be found in the given questionnaires");
                }
                i++;
            }

            /*for (Questionnaire q : historicalQuestionnaires) {
                if (i > 0) deprecated = true;
                boolean hasNext = i < historicalQuestionnaires.size()-1;
                try {
                    question = getQuestion(q, item.getLinkId());
                    question.setDeprecated(deprecated);
                    break;
                }catch (IllegalStateException e) {
                    if (!hasNext) throw new IllegalStateException("Corresponding question could not be found in the given questionnaires");
                }
                i++;
            }*/

            AnswerModel answer = getAnswer(item);
            answers.add(new QuestionAnswerPairModel(question, answer));
        }
        questionnaireResponseModel.setQuestionAnswerPairs(answers);
        return questionnaireResponseModel;
    }



    private QuestionnaireResponseModel constructQuestionnaireResponse(QuestionnaireResponse questionnaireResponse, FhirLookupResult lookupResult, String organisationId) {
        QuestionnaireResponseModel questionnaireResponseModel = new QuestionnaireResponseModel();

        mapBaseAttributesToModel(questionnaireResponseModel, questionnaireResponse);

        String questionnaireId = questionnaireResponse.getQuestionnaire();

        Questionnaire questionnaire = lookupResult.getQuestionnaire(questionnaireId)
                .orElseThrow(() -> new IllegalStateException(String.format("No Questionnaire found with id %s!", questionnaireId)));
        questionnaireResponseModel.setQuestionnaireName(questionnaire.getTitle());
        questionnaireResponseModel.setQuestionnaireId(extractId(questionnaire));


        if(questionnaireResponse.getBasedOn() == null || questionnaireResponse.getBasedOn().size() != 1) {
            throw new IllegalStateException(String.format("Error mapping QuestionnaireResponse %s: Expected exactly one BasedOn-attribute!", questionnaireResponseModel.getId().toString()));
        }
        questionnaireResponseModel.setCarePlanId(new QualifiedId(questionnaireResponse.getBasedOn().get(0).getReference()));

        if(questionnaireResponse.getAuthor() == null) {
            throw new IllegalStateException(String.format("Error mapping QuestionnaireResponse %s: No Author-attribute present!!", questionnaireResponseModel.getId().toString()));
        }
        questionnaireResponseModel.setAuthorId(new QualifiedId(questionnaireResponse.getAuthor().getReference()));

        if(questionnaireResponse.getSource() == null) {
            throw new IllegalStateException(String.format("Error mapping QuestionnaireResponse %s: No Source-attribute present!!", questionnaireResponseModel.getId().toString()));
        }
        questionnaireResponseModel.setSourceId(new QualifiedId(questionnaireResponse.getSource().getReference()));

        questionnaireResponseModel.setAnswered(questionnaireResponse.getAuthored().toInstant());
        questionnaireResponseModel.setExaminationStatus(ExtensionMapper.extractExaminationStatus(questionnaireResponse.getExtension()));
        questionnaireResponseModel.setTriagingCategory(ExtensionMapper.extractTriagingCategoory(questionnaireResponse.getExtension()));

        String practitionerId = ExtensionMapper.tryExtractExaminationAuthorPractitionerId(questionnaireResponse.getExtension());
        Optional<Practitioner> practitioner = lookupResult.getPractitioner(practitionerId);
        practitioner.ifPresent(value -> questionnaireResponseModel.setExaminationAuthor(mapPractitioner(value)));


        String patientId = questionnaireResponse.getSubject().getReference();
        Patient patient = lookupResult.getPatient(patientId)
                .orElseThrow(() -> new IllegalStateException(String.format("No Patient found with id %s!", patientId)));
        questionnaireResponseModel.setPatient(mapPatient(patient, organisationId));

        String carePlanId = questionnaireResponse.getBasedOnFirstRep().getReference();
        CarePlan carePlan = lookupResult.getCarePlan(carePlanId)
                .orElseThrow(() -> new IllegalStateException(String.format("No CarePlan found with id %s!", carePlanId)));

        // loop careplanens plandefinitions for at finde den der har referencen til questionnaire
        for (CanonicalType canonicalType : carePlan.getInstantiatesCanonical()) {
            String planDefinitionId = canonicalType.getValue();
            PlanDefinition planDefinition = lookupResult.getPlanDefinition(planDefinitionId)
                    .orElseThrow(() -> new IllegalStateException(String.format("No PlanDefinition found with id %s!", planDefinitionId)));

            boolean found = false;
            for (PlanDefinition.PlanDefinitionActionComponent planDefinitionActionComponent : planDefinition.getAction()) {
                if (planDefinitionActionComponent.getDefinitionCanonicalType().equals(questionnaireId)) {
                    found = true;
                    break;
                }
            }

            if (found) {
                questionnaireResponseModel.setPlanDefinitionTitle(planDefinition.getTitle());
                break;
            }
        }

        return questionnaireResponseModel;
    }

    public FrequencyModel mapTiming(Timing timing) {
        FrequencyModel frequencyModel = new FrequencyModel();

        if(timing.getRepeat() != null) {
            Timing.TimingRepeatComponent repeat = timing.getRepeat();
            frequencyModel.setWeekdays(repeat.getDayOfWeek().stream().map(d -> Enum.valueOf(Weekday.class, d.getValue().toString())).collect(Collectors.toList()));
            if(!repeat.getTimeOfDay().isEmpty())
                frequencyModel.setTimeOfDay(LocalTime.parse(repeat.getTimeOfDay().get(0).getValue()));
        }

        return frequencyModel;
    }

    private void mapBaseAttributesToModel(BaseModel target, DomainResource source) {
        target.setId(extractId(source));
        target.setOrganizationId(ExtensionMapper.extractOrganizationId(source.getExtension()));
    }

    private void mapBaseAttributesToFhir(DomainResource target, BaseModel source) {
        // We may be creating the resource, and in that case, it is perfectly ok for it not to have id and organization id.
        if(source.getId() != null) {
            target.setId(source.getId().toString());
        }
        if(source.getOrganizationId() != null) {
            target.addExtension(ExtensionMapper.mapOrganizationId(source.getOrganizationId()));
        }
    }

    private QualifiedId extractId(DomainResource resource) {
        String unqualifiedVersionless = resource.getIdElement().toUnqualifiedVersionless().getValue();
        if(FhirUtils.isPlainId(unqualifiedVersionless)) {
            return new QualifiedId(unqualifiedVersionless, resource.getResourceType());
        }
        else if (FhirUtils.isQualifiedId(unqualifiedVersionless, resource.getResourceType())) {
            return new QualifiedId(unqualifiedVersionless);
        }
        else {
            throw new IllegalArgumentException(String.format("Illegal id for resource of type %s: %s!", resource.getResourceType(), unqualifiedVersionless));
        }
    }

    private Identifier makeCprIdentifier(String cpr) {
        Identifier identifier = new Identifier();

        identifier.setSystem(Systems.CPR);
        identifier.setValue(cpr);

        return identifier;
    }

    public String extractCpr(Patient patient) {
        return patient.getIdentifier().get(0).getValue();
    }

    private HumanName buildName(String givenName, String familyName) {
        return buildName(givenName, familyName, null);
    }

    private HumanName buildName(String text) {
        return buildName(null, null, text);
    }

    private HumanName buildName(String givenName, String familyName, String text) {
        var name = new HumanName();

        name.addGiven(givenName);
        name.setFamily(familyName);
        name.setText(text);

        return name;
    }

    private Address buildAddress(ContactDetailsModel contactDetailsModel) {
        var address = new Address();

        address.addLine(contactDetailsModel.getStreet());
        address.setPostalCode(contactDetailsModel.getPostalCode());
        address.setCity(contactDetailsModel.getCity());

        return address;
    }

    private ContactPoint buildContactPoint(String phone, int rank) {
        var contactPoint = new ContactPoint();

        contactPoint.setSystem(ContactPoint.ContactPointSystem.PHONE);
        contactPoint.setValue(phone);
        contactPoint.setRank(rank);

        return contactPoint;
    }

    private String extractFamilyName(Patient patient) {
        if(patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }
        return patient.getName().get(0).getFamily();
    }

    private String extractGivenNames(Patient patient) {
        if(patient.getName() == null || patient.getName().isEmpty()) {
            return null;
        }
        return patient.getName().get(0).getGivenAsSingleString();
    }

    private ContactDetailsModel extractPatientContactDetails(Patient patient) {
        ContactDetailsModel contactDetails = new ContactDetailsModel();

        var lines = patient.getAddressFirstRep().getLine();
        if(lines != null && !lines.isEmpty()) {
            contactDetails.setStreet(String.join(", ", lines.stream().map(l -> l.getValue()).collect(Collectors.toList())));
        }
        contactDetails.setCity(patient.getAddressFirstRep().getCity());
        contactDetails.setPostalCode(patient.getAddressFirstRep().getPostalCode());
        contactDetails.setPrimaryPhone(extractPrimaryPhone(patient.getTelecom()));
        contactDetails.setSecondaryPhone(extractSecondaryPhone(patient.getTelecom()));
        contactDetails.setCountry(extractCountry(patient));

        return contactDetails;
    }

    private String extractCountry(Patient patient) {
        var country = patient.getAddressFirstRep().getCountry();
        if(country == null || country.isEmpty()) {
            return null;
        }
        return country;
    }

    private String extractPrimaryPhone(List<ContactPoint> contactPoints) {
        return extractPhone(contactPoints, 1);
    }

    private String extractSecondaryPhone(List<ContactPoint> contactPoints) {
        return extractPhone(contactPoints, 2);
    }

    private String extractPhone(List<ContactPoint> contactPoints, int rank) {
        if(contactPoints == null || contactPoints.isEmpty()) {
            return null;
        }
        for(ContactPoint cp : contactPoints) {
            if(cp.getSystem().equals(ContactPoint.ContactPointSystem.PHONE) && cp.getRank() == rank) {
                return cp.getValue();
            }
        }
        return null;
    }

    private QuestionModel getQuestion(Questionnaire questionnaire, String linkId) {
        var item = getQuestionnaireItem(questionnaire, linkId);
        if(item == null) {
            throw new IllegalStateException(String.format("Malformed QuestionnaireResponse: Question for linkId %s not found in Questionnaire %s!", linkId, questionnaire.getId()));
        }

        return mapQuestionnaireItem(item);
    }

    private Questionnaire.QuestionnaireItemComponent getQuestionnaireItem(Questionnaire questionnaire, String linkId) {
        for(var item : questionnaire.getItem()) {
            if(item != null && item.getLinkId() != null && item.getLinkId().equals(linkId)) {
                return item;
            }
        }
        return null;
    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionnaireItem(QuestionModel question) {
        Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent();

        item.setLinkId(question.getLinkId());
        item.setText(question.getText());
        if (question.getAbbreviation() != null) {
            item.addExtension(ExtensionMapper.mapQuestionAbbreviation(question.getAbbreviation()));
        }
        if (question.getThresholds() != null) {
            item.getExtension().addAll(ExtensionMapper.mapThresholds(question.getThresholds()));
        }
        if (question.getHelperText() != null) {
            item.addItem(mapQuestionHelperText(question.getHelperText()));
        }
        item.setRequired(question.isRequired());
        if (question.getOptions() != null) {
            item.setAnswerOption( mapAnswerOptions(question.getOptions()) );
        }
        item.setType( mapQuestionType(question.getQuestionType()) );
        if (question.getEnableWhens() != null) {
            item.setEnableWhen( mapEnableWhens(question.getEnableWhens()) );
        }

        if (question.getMeasurementType() != null ) {
            item.getCodeFirstRep()
                    .setCode(question.getMeasurementType().getCode())
                    .setDisplay(question.getMeasurementType().getDisplay())
                    .setSystem(question.getMeasurementType().getSystem());
        }

        if (item.getType() == Questionnaire.QuestionnaireItemType.GROUP) {
            question.getSubQuestions().forEach(questionModel -> {
                item.addItem( this.mapQuestionnaireItem(questionModel) );
            });
        }
        return item;
    }

    private QuestionModel mapQuestionnaireItem(Questionnaire.QuestionnaireItemComponent item) {
        QuestionModel question = new QuestionModel();

        question.setLinkId(item.getLinkId());
        question.setText(item.getText());
        question.setAbbreviation(ExtensionMapper.extractQuestionAbbreviation(item.getExtension()));
        question.setHelperText( mapQuestionnaireItemHelperText(item.getItem()));
        question.setRequired(item.getRequired());
        if(item.getAnswerOption() != null) {
            // TODO: The mapping below has to be changed from excluding the "comment" and the "triage"
            question.setOptions( mapAnswerOptionComponents(item.getAnswerOption()) );
        }
        question.setQuestionType( mapQuestionType(item.getType()) );
        if (item.hasEnableWhen()) {
            question.setEnableWhens( mapEnableWhenComponents(item.getEnableWhen()) );
        }
        if (item.hasCode()) {
            question.setMeasurementType(mapCodingConcept(item.getCodeFirstRep().getSystem(), item.getCodeFirstRep().getCode(), item.getCodeFirstRep().getDisplay()));
        }

        if (item.getType() == Questionnaire.QuestionnaireItemType.GROUP) {
            question.setSubQuestions( mapQuestionnaireItemGroupQuestions(item.getItem()) );
        }
        question.setThresholds(ExtensionMapper.extractThresholds(item.getExtensionsByUrl(Systems.THRESHOLD)));

        return question;
    }

    private List<QuestionModel> mapQuestionnaireItemGroupQuestions(List<Questionnaire.QuestionnaireItemComponent> item) {
        return item.stream()
                .filter(i -> i.getType() != Questionnaire.QuestionnaireItemType.DISPLAY)
                .map(this::mapQuestionnaireItem)
                .collect(Collectors.toList());
    }

    private String mapQuestionnaireItemHelperText(List<Questionnaire.QuestionnaireItemComponent> item) {
        return item.stream()
            .filter(i -> i.getType().equals(Questionnaire.QuestionnaireItemType.DISPLAY))
            .map(i -> i.getText())
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null)
            ;

    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionHelperText(String text) {
        Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent();
        item.setLinkId(IdType.newRandomUuid().getValueAsString())
            .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
            .setText(text);

        return item;
    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionnaireCallToActions(QuestionModel callToAction) {
        Questionnaire.QuestionnaireItemComponent item = mapQuestionnaireItem(callToAction);
        item.setType(Questionnaire.QuestionnaireItemType.DISPLAY);
        item.setLinkId(Systems.CALL_TO_ACTION_LINK_ID);

        return item;
    }

    private Questionnaire.QuestionnaireItemComponent mapQuestionnaireCallToAction(QuestionModel question) {
        Questionnaire.QuestionnaireItemComponent item = new Questionnaire.QuestionnaireItemComponent();

        item.setLinkId(question.getLinkId());
        item.setText(question.getText());
        item.setRequired(question.isRequired());
//        if (question.getOptions() != null) {
//             TODO: Fix the line below - it has to include comment and triage
//            item.setAnswerOption( mapAnswerOptions(question.getOptions().stream().map(Option::getOption).collect(Collectors.toList())) );
//        }
        item.setType( mapQuestionType(question.getQuestionType()) );
        if (question.getEnableWhens() != null) {
            item.setEnableWhen( mapEnableWhens(question.getEnableWhens()) );
        }
        return item;
    }

    private List<Questionnaire.QuestionnaireItemEnableWhenComponent> mapEnableWhens(List<QuestionModel.EnableWhen> enableWhens) {
        return enableWhens
            .stream()
            .map(ew -> mapEnableWhen(ew))
            .collect(Collectors.toList());
    }

    private Questionnaire.QuestionnaireItemEnableWhenComponent mapEnableWhen(QuestionModel.EnableWhen enableWhen) {
        Questionnaire.QuestionnaireItemEnableWhenComponent enableWhenComponent = new Questionnaire.QuestionnaireItemEnableWhenComponent();

        enableWhenComponent.setOperator( mapEnableWhenOperator(enableWhen.getOperator()) );
        enableWhenComponent.setQuestion(enableWhen.getAnswer().getLinkId());
        enableWhenComponent.setAnswer(getValue(enableWhen.getAnswer()));
        enableWhenComponent.setOperator(mapEnableWhenOperator(enableWhen.getOperator()));

        return enableWhenComponent;
    }

    private List<QuestionModel.EnableWhen> mapEnableWhenComponents(List<Questionnaire.QuestionnaireItemEnableWhenComponent> enableWhen) {
        return enableWhen
            .stream()
            .map(ew -> mapEnableWhenComponent(ew))
            .collect(Collectors.toList());
    }

    private QuestionModel.EnableWhen mapEnableWhenComponent(Questionnaire.QuestionnaireItemEnableWhenComponent enableWhen) {
        QuestionModel.EnableWhen newEnableWhen = new QuestionModel.EnableWhen();

        newEnableWhen.setOperator( mapEnableWhenOperator(enableWhen.getOperator()) );
        newEnableWhen.setAnswer( mapAnswer(enableWhen.getQuestion(), enableWhen.getAnswer()) );

        return newEnableWhen;
    }

    private AnswerModel mapAnswer(String question, Type answer) {
        AnswerModel answerModel = new AnswerModel();
        answerModel.setLinkId(question);

        if (answer instanceof StringType) {
            answerModel.setAnswerType(AnswerType.STRING);
            answerModel.setValue( ((StringType)answer).asStringValue() );
        }
        else if (answer instanceof BooleanType) {
            answerModel.setAnswerType(AnswerType.BOOLEAN);
            answerModel.setValue(((BooleanType) answer).asStringValue() );
        }
        else if (answer instanceof Quantity) {
            answerModel.setAnswerType(AnswerType.QUANTITY);
            answerModel.setValue(((Quantity) answer).getValueElement().asStringValue() );
        }
        else if (answer instanceof IntegerType) {
            answerModel.setAnswerType(AnswerType.INTEGER);
            answerModel.setValue(((IntegerType) answer).asStringValue() );
        }
        else {
            throw new IllegalArgumentException(String.format("Unsupported AnswerItem of type: %s", answer));
        }

        return answerModel;
    }

    private Questionnaire.QuestionnaireItemOperator mapEnableWhenOperator(EnableWhenOperator operator) {
        switch (operator) {
            case EQUAL:
                return Questionnaire.QuestionnaireItemOperator.EQUAL;
            case LESS_THAN:
                return Questionnaire.QuestionnaireItemOperator.LESS_THAN;
            case LESS_OR_EQUAL:
                return Questionnaire.QuestionnaireItemOperator.LESS_OR_EQUAL;
            case GREATER_THAN:
                return Questionnaire.QuestionnaireItemOperator.GREATER_THAN;
            case GREATER_OR_EQUAL:
                return Questionnaire.QuestionnaireItemOperator.GREATER_OR_EQUAL;
            default:
                throw new IllegalArgumentException(String.format("Don't know how to map Questionnaire.QuestionnaireItemOperator %s", operator.toString()));
        }
    }

    private EnableWhenOperator mapEnableWhenOperator(Questionnaire.QuestionnaireItemOperator operator) {
        switch (operator) {
            case EQUAL:
                return EnableWhenOperator.EQUAL;
            case LESS_THAN:
                return EnableWhenOperator.LESS_THAN;
            case LESS_OR_EQUAL:
                return EnableWhenOperator.LESS_OR_EQUAL;
            case GREATER_THAN:
                return EnableWhenOperator.GREATER_THAN;
            case GREATER_OR_EQUAL:
                return EnableWhenOperator.GREATER_OR_EQUAL;
            default:
                throw new IllegalArgumentException(String.format("Don't know how to map QuestionnaireItemOperator %s", operator.toString()));
        }
    }

    private List<Questionnaire.QuestionnaireItemAnswerOptionComponent> mapAnswerOptions(List<Option> answerOptions) {
        return answerOptions
            .stream()
            .map(oc -> (Questionnaire.QuestionnaireItemAnswerOptionComponent) new Questionnaire.QuestionnaireItemAnswerOptionComponent()
                    .setValue(new StringType(oc.getOption()))
                    .addExtension(ExtensionMapper.mapAnswerOptionComment(oc.getComment())))
            .collect(Collectors.toList());
    }

    private List<Option> mapAnswerOptionComponents(List<Questionnaire.QuestionnaireItemAnswerOptionComponent> optionComponents) {
        return optionComponents
                .stream()
                .map(oc -> new Option(oc.getValue().primitiveValue(), ExtensionMapper.extractAnswerOptionComment(oc.getExtension())))
                .collect(Collectors.toList());
    }

    private QuestionType mapQuestionType(Questionnaire.QuestionnaireItemType type) {
        switch(type) {
            case CHOICE:
                return QuestionType.CHOICE;
            case INTEGER:
                return QuestionType.INTEGER;
            case QUANTITY:
                return QuestionType.QUANTITY;
            case STRING:
                return QuestionType.STRING;
            case BOOLEAN:
                return QuestionType.BOOLEAN;
            case DISPLAY:
                return QuestionType.DISPLAY;
            case GROUP:
                return QuestionType.GROUP;
            default:
                throw new IllegalArgumentException(String.format("Don't know how to map QuestionnaireItemType %s", type.toString()));
        }
    }

    private Questionnaire.QuestionnaireItemType mapQuestionType(QuestionType type) {
        switch(type) {
            case CHOICE:
                return Questionnaire.QuestionnaireItemType.CHOICE;
            case INTEGER:
                return Questionnaire.QuestionnaireItemType.INTEGER;
            case QUANTITY:
                return Questionnaire.QuestionnaireItemType.QUANTITY;
            case STRING:
                return Questionnaire.QuestionnaireItemType.STRING;
            case BOOLEAN:
                return Questionnaire.QuestionnaireItemType.BOOLEAN;
            case DISPLAY:
                return Questionnaire.QuestionnaireItemType.DISPLAY;
            case GROUP:
                return Questionnaire.QuestionnaireItemType.GROUP;
            default:
                throw new IllegalArgumentException(String.format("Don't know how to map Questionnaire.ItemType %s", type.toString()));
        }
    }

    private AnswerModel getAnswer(QuestionnaireResponse.QuestionnaireResponseItemComponent item) {
        AnswerModel answer = new AnswerModel();
        answer.setLinkId(item.getLinkId());

        boolean emptyAnswer = item.getAnswer().isEmpty();
        boolean hasSubAnswers = !item.getItem().isEmpty();
        if (emptyAnswer && hasSubAnswers) {
            // group answer with sub-answers
            answer.setAnswerType(AnswerType.GROUP);
            answer.setSubAnswers(item.getItem().stream().map(this::getAnswer).collect(Collectors.toList()));
        }
        else {

            var answerItem = extractAnswerItem(item);

            if (answerItem.hasValueStringType()) {
                answer.setValue(answerItem.getValue().primitiveValue());
            } else if (answerItem.hasValueIntegerType()) {
                answer.setValue(answerItem.getValueIntegerType().primitiveValue());
            } else if (answerItem.hasValueQuantity()) {
                answer.setValue(answerItem.getValueQuantity().getValueElement().primitiveValue());
            } else if (answerItem.hasValueBooleanType()) {
                answer.setValue(answerItem.getValueBooleanType().primitiveValue());
            }
            answer.setAnswerType(getAnswerType(answerItem));
        }

        return answer;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent extractAnswerItem(QuestionnaireResponse.QuestionnaireResponseItemComponent item) {
        if(item.getAnswer() == null || item.getAnswer().size() != 1) {
            throw new IllegalStateException("Expected exactly one answer!");
        }
        return item.getAnswer().get(0);
    }

    private AnswerType getAnswerType(QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent answerItem) {
        Type answerType = answerItem.getValue();
        return getAnswerType(answerType);
    }

    private AnswerType getAnswerType(Type answerType) {
        if (answerType instanceof StringType) {
            return AnswerType.STRING;
        }
        if (answerType instanceof BooleanType) {
            return AnswerType.BOOLEAN;
        }
        else if (answerType instanceof Quantity) {
            return AnswerType.QUANTITY;
        }
        else if (answerType instanceof IntegerType) {
            return AnswerType.INTEGER;
        }
        else {
            throw new IllegalArgumentException(String.format("Unsupported AnswerItem of type: %s", answerType));
        }
    }

    private CarePlan.CarePlanActivityComponent buildCarePlanActivity(QuestionnaireWrapperModel questionnaireWrapperModel) {
        CanonicalType instantiatesCanonical = new CanonicalType(questionnaireWrapperModel.getQuestionnaire().getId().toString());
        Type timing = mapFrequencyModel(questionnaireWrapperModel.getFrequency());
        Extension activitySatisfiedUntil = ExtensionMapper.mapActivitySatisfiedUntil(questionnaireWrapperModel.getSatisfiedUntil());

        return buildActivity(instantiatesCanonical, timing, activitySatisfiedUntil);
    }

    private CarePlan.CarePlanActivityComponent buildActivity(CanonicalType instantiatesCanonical, Type timing, Extension activitySatisfiedUntil) {
        CarePlan.CarePlanActivityComponent activity = new CarePlan.CarePlanActivityComponent();

        activity.setDetail(buildDetail(instantiatesCanonical, timing, activitySatisfiedUntil));

        return activity;
    }

    private CarePlan.CarePlanActivityDetailComponent buildDetail(CanonicalType instantiatesCanonical, Type timing, Extension activitySatisfiedUntil) {
        CarePlan.CarePlanActivityDetailComponent detail = new CarePlan.CarePlanActivityDetailComponent();

        detail.setInstantiatesCanonical(List.of(instantiatesCanonical));
        detail.setStatus(CarePlan.CarePlanActivityStatus.NOTSTARTED);
        detail.addExtension(activitySatisfiedUntil);
        detail.setScheduled(timing);

        return detail;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemComponent getQuestionnaireResponseItem(AnswerModel answer) {
        var item = new QuestionnaireResponse.QuestionnaireResponseItemComponent();

        item.setLinkId(answer.getLinkId());
        item.getAnswer().add(getAnswerItem(answer));

        if (answer.getAnswerType() == AnswerType.GROUP && answer.getSubAnswers() != null) {
            item.setItem(answer.getSubAnswers().stream().map(this::getQuestionnaireResponseItem).collect(Collectors.toList()));
        }

        return item;
    }

    private QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent getAnswerItem(AnswerModel answer) {
        var answerItem = new QuestionnaireResponse.QuestionnaireResponseItemAnswerComponent();

        answerItem.setValue(getValue(answer));

        return answerItem;
    }

    private Type getValue(AnswerModel answer) {
        Type value = null;
        switch(answer.getAnswerType()) {
            case INTEGER:
                value = new IntegerType(answer.getValue());
                break;
            case STRING:
                value = new StringType(answer.getValue());
                break;
            case QUANTITY:
                value = new Quantity(Double.parseDouble(answer.getValue()));
                break;
            case BOOLEAN:
                value = new BooleanType(answer.getValue());
                break;
            case GROUP:
                // return default = null
                break;
            default:
                throw new IllegalArgumentException(String.format("Unknown AnswerType: %s", answer.getAnswerType()));
        }
        return value;
    }

    private QuestionnaireWrapperModel mapPlanDefinitionAction(PlanDefinition.PlanDefinitionActionComponent action, FhirLookupResult lookupResult) {
        var wrapper = new QuestionnaireWrapperModel();

        String questionnaireId = action.getDefinitionCanonicalType().getValue();
        Questionnaire questionnaire = lookupResult
                .getQuestionnaire(questionnaireId)
                .orElseThrow(() -> new IllegalStateException(String.format("Could not look up Questionnaire with id %s!", questionnaireId)));
        wrapper.setQuestionnaire(mapQuestionnaire(questionnaire));

        wrapper.setThresholds( ExtensionMapper.extractThresholds(action.getExtensionsByUrl(Systems.THRESHOLD)) );

        // initialize timeofday from responsible organization
        String organizationId = ExtensionMapper.extractOrganizationId(questionnaire.getExtension());
        Organization organization = lookupResult
                .getOrganization(organizationId)
                .orElseThrow(() -> new IllegalStateException(String.format("Could not look up Organization with id %s!", organizationId)));

        TimeType timeType = ExtensionMapper.extractOrganizationDeadlineTimeDefault(organization.getExtension());
        if (timeType != null) {
            Timing timing = new Timing();
            timing.getRepeat().getTimeOfDay().add(timeType);
            wrapper.setFrequency(mapTiming(timing));
        }


        return wrapper;
    }

    public PractitionerModel mapPractitioner(Practitioner practitioner) {
        PractitionerModel practitionerModel = new PractitionerModel();

        practitionerModel.setId(extractId(practitioner));
        practitionerModel.setGivenName(practitioner.getNameFirstRep().getGivenAsSingleString());
        practitionerModel.setFamilyName(practitioner.getNameFirstRep().getFamily());

        return practitionerModel;
    }

    public List<MeasurementTypeModel> extractMeasurementTypes(ValueSet valueSet) {
        List<MeasurementTypeModel> result = new ArrayList<>();

        valueSet.getCompose().getInclude()
            .forEach(csc -> {
                var measurementTypes = csc.getConcept().stream()
                    .map(crc -> mapCodingConcept(csc.getSystem(), crc))
                    .collect(Collectors.toList());

                result.addAll(measurementTypes);
            });

        return result;
    }

    private MeasurementTypeModel mapCodingConcept(String system, ValueSet.ConceptReferenceComponent concept) {
        return mapCodingConcept(system, concept.getCode(), concept.getDisplay());
    }

    private MeasurementTypeModel mapCodingConcept(String system, String code, String display) {
        MeasurementTypeModel measurementTypeModel = new MeasurementTypeModel();

        measurementTypeModel.setSystem(system);
        measurementTypeModel.setCode(code);
        measurementTypeModel.setDisplay(display);

        return measurementTypeModel;
    }

    public PlanDefinition mapPlanDefinitionModel(PlanDefinitionModel planDefinitionModel) {
        PlanDefinition planDefinition = new PlanDefinition();

        mapBaseAttributesToFhir(planDefinition, planDefinitionModel);

        planDefinition.setTitle(planDefinitionModel.getTitle());
        planDefinition.setStatus(Enumerations.PublicationStatus.valueOf(planDefinitionModel.getStatus().toString()));

        // Map questionnaires to actions
        if(planDefinitionModel.getQuestionnaires() != null) {
            planDefinition.setAction(planDefinitionModel.getQuestionnaires()
                .stream()
                .map(q -> buildPlanDefinitionAction(q))
                .collect(Collectors.toList()));
        }

        return planDefinition;
    }

    private PlanDefinition.PlanDefinitionActionComponent buildPlanDefinitionAction(QuestionnaireWrapperModel questionnaireWrapperModel) {
        CanonicalType definitionCanonical = new CanonicalType(questionnaireWrapperModel.getQuestionnaire().getId().toString());

        List<Extension> thresholds = ExtensionMapper.mapThresholds(questionnaireWrapperModel.getThresholds());

        PlanDefinition.PlanDefinitionActionComponent action = new PlanDefinition.PlanDefinitionActionComponent();
        action.setDefinition(definitionCanonical);
        action.setExtension(thresholds);

        return action;
    }


}
