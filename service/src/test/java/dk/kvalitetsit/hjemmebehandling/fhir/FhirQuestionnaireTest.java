package dk.kvalitetsit.hjemmebehandling.fhir;

import org.hl7.fhir.r4.model.BooleanType;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Questionnaire;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import ca.uhn.fhir.context.FhirContext;

@ExtendWith(MockitoExtension.class)
public class FhirQuestionnaireTest {
    @Test
    void questionnaire() {
      Coding NPU08676 = new Coding("urn:oid:1.2.208.176.2.1","NPU08676","Legeme temp.;Pt");
      Coding UCUM_NPU08676 = new Coding().setSystem("http://unitsofmeasure.org").setCode("Cel").setDisplay("grader Celsius");
      Coding NPU01423 = new Coding("urn:oid:1.2.208.176.2.1","NPU19748","C-reaktivt protein [CRP];P");
      Coding UCUM_NPU01423 = new Coding().setSystem("http://unitsofmeasure.org").setCode("mg/L").setDisplay("milligram per liter");


      Questionnaire questionnaire = new Questionnaire();
      questionnaire.setLanguage("da-DK");
      questionnaire.setStatus(Enumerations.PublicationStatus.ACTIVE);

      // spørgsmål - temperatur
      Questionnaire.QuestionnaireItemComponent question1 = questionnaire.addItem()
          .setLinkId("temperature")
          .setText("Indtast din morgen temperatur?")
          .setType(Questionnaire.QuestionnaireItemType.QUANTITY)
          .addCode(NPU08676);
      question1.addExtension()
            .setUrl("http://hl7.org/fhir/StructureDefinition/questionnaire-unit")
            .setValue(UCUM_NPU08676);
      question1.addItem()
          .setLinkId("help")
          .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
          .setText("For at få den korrekte værdi skal der være en rektal temperatur dvs. målt i endetarmen");

      // spørgsmål - CRP
      Questionnaire.QuestionnaireItemComponent question2 = questionnaire.addItem()
          .setLinkId("crp")
          .setText("Indtast den målte CRP værdi")
          .setType(Questionnaire.QuestionnaireItemType.QUANTITY)
          .addCode(NPU01423);
      question2.addExtension()
          .setUrl("http://hl7.org/fhir/StructureDefinition/questionnaire-unit")
          .setValue(UCUM_NPU01423);
      question2.addItem()
          .setLinkId("help")
          .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
          .setText("CRP er et udtryk for infektion/betændelse.");
      question2.addItem()
          .setLinkId("help")
          .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
          .setText("CRP måles på det udleverede apparat. Se den vedlagte instruktion.");

      // spørgsmål - Antibiotika
      Questionnaire.QuestionnaireItemComponent question3 = questionnaire.addItem()
          .setLinkId("antibiotika")
          .setText("Har du fået alt den ordinerede antibiotika det sidste døgn?")
          .setType(Questionnaire.QuestionnaireItemType.BOOLEAN);
      question3.addItem()
          .setLinkId("help")
          .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
          .setText("Hvis du får antibiotika på pumpe skal du svare nej hvis der har været problemer med indløb.");

      // spørgsmål - helbredstilstand
      Questionnaire.QuestionnaireItemComponent question4 = questionnaire.addItem()
          .setLinkId("helbredstilstand")
          .setText("Er din helbredstilstand værre idag sammenlignet med igår?")
          .setType(Questionnaire.QuestionnaireItemType.BOOLEAN);
      question4.addItem()
          .setLinkId("help")
          .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
          .setText("Hvis den er uændret fra i går skal du svare nej.");

      // spørgsmål - nye symptomer
      Questionnaire.QuestionnaireItemComponent question5 = questionnaire.addItem()
          .setLinkId("nye_symptomer")
          .setText("Er der kommet nye symptomer i det sidste døgn?")
          .setType(Questionnaire.QuestionnaireItemType.BOOLEAN);
      question5.addItem()
          .setLinkId("help")
          .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
          .setText("Nye symptomer kan være: smerter, hoste, diare, svie ved vandladning mv.");

      // spørgsmål - udslæt
      Questionnaire.QuestionnaireItemComponent question6 = questionnaire.addItem()
          .setLinkId("udslæt")
          .setText("Har du udslæt?")
          .setType(Questionnaire.QuestionnaireItemType.BOOLEAN);
      question6.addItem()
          .setLinkId("help")
          .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
          .setText("Hvis dit udslæt er kommet i løbet af dette døgn skal du svare nej");

      // spørgsmål - udslæt 2
      Questionnaire.QuestionnaireItemComponent question7 = questionnaire.addItem()
          .setLinkId("udslæt_2")
          .setText("Er dit udslæt værre idag end i går?")
          .setType(Questionnaire.QuestionnaireItemType.BOOLEAN);
      question7.addItem()
          .setLinkId("help")
          .setType(Questionnaire.QuestionnaireItemType.DISPLAY)
          .setText("Hvis dit udslæt er kommet i løbet af dette døgn skaldu svare nej");
      Questionnaire.QuestionnaireItemEnableWhenComponent enableWhenFirstRep = question7.getEnableWhenFirstRep();
      enableWhenFirstRep.setQuestionElement(question6.getLinkIdElement());
      enableWhenFirstRep.setOperator(Questionnaire.QuestionnaireItemOperator.EQUAL);
      enableWhenFirstRep.setAnswer(new BooleanType(true));
      
      // print
    //  System.out.println( FhirContext.forR4().newValidator().validateWithResult(questionnaire).isSuccessful() );
    //  System.out.println("----");
      System.out.println( FhirContext.forR4().newJsonParser().setPrettyPrint(true).encodeResourceToString(questionnaire) );
      System.out.println("----");
      System.out.println( FhirContext.forR4().newXmlParser().setPrettyPrint(true).encodeResourceToString(questionnaire) );
      System.out.println("----");
    }

}