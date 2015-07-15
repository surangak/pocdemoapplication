package org.ohie.pocdemo.form;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryRequest;
import org.dcm4chee.xds2.infoset.rim.AdhocQueryResponse;
import org.ohie.pocdemo.form.model.*;
import org.ohie.pocdemo.form.model.ModifyXDSbMessage;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.rim.RegistryResponseType;


import org.ohie.pocdemo.form.util.ClientRegistryUtil;
import org.ohie.pocdemo.form.util.XdsMessageUtil;
import org.ohie.pocdemo.form.util.MainUtil;


import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.ohie.pocdemo.form.model.AphpDocument;

import javax.xml.bind.JAXBException;
import javax.xml.xpath.XPathExpressionException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

@Controller
@RequestMapping("/form.htm")
public class MainController {

   /** @Autowired
    @Qualifier("formValidator")
    private Validator validator;

    @Qualifier("patientValidator")**/

    List<Patient> patients = new ArrayList<Patient>();
    List<Provider> providers = new ArrayList<Provider>();
	
	@RequestMapping(method = RequestMethod.GET)
	public String initForm(Model model) {
        AphpDocument form = new AphpDocument();

		model.addAttribute("form", form);
		initModelList(model);
		return "form";
	}

	@RequestMapping(method = RequestMethod.POST)
	public String submitForm(Model model, AphpDocument form, Patient patient, BindingResult result, @RequestParam String action) throws XPathExpressionException, HL7Exception {
		if( action.equals("submit") ) {

            model.addAttribute("form", form);
            String returnVal = "successForm";

            String documentId = String.format("1.3.6.1.4.1.21367.2010.1.2.%s", new SimpleDateFormat("HHmmss").format(new Date()));
            ModifyXDSbMessage modify = new ModifyXDSbMessage(patients.get(0), providers.get(0));
            modify.modify("OHIE-XDS-01-30.xml");

            try {
                ProvideAndRegisterDocumentSetRequestType pnrRequest = XdsMessageUtil.loadMessage("OHIE-XDS-01-30", ProvideAndRegisterDocumentSetRequestType.class);
                RegistryResponseType xdsResponse = XdsMessageUtil.provideAndRegister(pnrRequest);
                XdsMessageUtil.assertSuccess(xdsResponse);

            } catch (JAXBException e) {
                e.printStackTrace();
                form.setException(e);
            } catch (IOException e) {
                e.printStackTrace();
                form.setException(e);
            } catch (Exception e) {
                e.printStackTrace();
                return returnVal;
            }

            return returnVal;
        }


        if( action.equals("query") ) {

            model.addAttribute("form", form);
            String returnVal = "successForm";

            AdhocQueryRequest queryRequest = null;
            try {
                queryRequest = XdsMessageUtil.loadMessage("OHIE-XDS-01-20", AdhocQueryRequest.class);

            } catch (JAXBException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            AdhocQueryResponse queryResponse = XdsMessageUtil.registryStoredQuery(queryRequest);
            XdsMessageUtil.assertSuccess(queryResponse);
            System.out.println(queryResponse);
            XdsMessageUtil.assertHasDocumentId(queryResponse, "2.16.840.1.113883.3.72.5.9.1.0.5386503746339693382111111111");

            // STEP 50 - Retrieve
            RetrieveDocumentSetRequestType retrieveRequest =  XdsMessageUtil.createRetrieveRequest("2.16.840.1.113883.3.72.5.9.1.0.5386503746339693382111111111", queryResponse);
            RetrieveDocumentSetResponseType retrieveResponse = XdsMessageUtil.retrieveDocumentSet(retrieveRequest);
            assertEquals(1, retrieveResponse.getDocumentResponse().size());
            XdsMessageUtil.assertMatchesHash(queryResponse, retrieveResponse);

            return returnVal;
        }


        if(action.equals("patient")){
            model.addAttribute("patient", patient);
            String returnVal = "successForm";

            Message response = MainUtil.createPatient(patient);

                Terser assertTerser = new Terser(response);
                ClientRegistryUtil.assertAccepted(assertTerser);
                System.out.println("to string " + assertTerser.toString());

            String message = "MSH|^~\\&|TEST_HARNESS^^|TEST^^|CR1^^|MOH_CAAT^^|20141104174451||ADT^A01^ADT_A01|TEST-CR-05-10|P|2.3.1\n" +
                    "EVN||20101020\n" +
                    "PID|||"+ patient.getIdentifier()+"^^^TEST||"+ patient.getLastName()+"^"+ patient.getFirstName()+"^^^^^L|SMITH^^^^^^L|19840125|F|||123 Main Street West ^^NEWARK^NJ^30293||^PRN^PH^^^409^3049506||||||\n" +
                    "PV1||I";

            try {
                Message request = ClientRegistryUtil.loadMessage(message);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return null;

	}

	private void initModelList(Model model) {




        String csvFile = "/Users/snkasthu/Downloads/pocdemo/src/main/resources/xds/patient.csv";
        BufferedReader br = null;
        String line = "";
        String cvsSplitBy = ",";

        try {

            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {

                String[] result = line.split("\\,");

                Patient patient = new Patient(result[0], result[1], result[2], result[3], result[4], result[5], result[6]);
                patients.add(patient);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        System.out.println("Done");


        model.addAttribute("patients", patients);

        String csvFileProvider = "/Users/snkasthu/Downloads/pocdemo/src/main/resources/xds/provider.csv";
        BufferedReader brProvider = null;
        line = "";


        try {

            brProvider = new BufferedReader(new FileReader(csvFileProvider));
            while ((line = brProvider.readLine()) != null) {

                String[] result = line.split("\\,");

                Provider provider = new Provider(result[0], result[1], result[2]);
                providers.add(provider);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        model.addAttribute("providers", providers);

	}
}
