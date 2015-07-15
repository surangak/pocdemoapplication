package org.ohie.pocdemo.form.util;

import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.util.Terser;
import org.ohie.pocdemo.form.model.*;

import static org.junit.Assert.fail;

public class MainUtil {

    public static Message createPatient(Patient patient){

        Message response = null;

        try
        {
            String message = "MSH|^~\\&|TEST_HARNESS^^|TEST^^|CR1^^|MOH_CAAT^^|20141104174451||ADT^A01^ADT_A01|TEST-CR-05-10|P|2.3.1\n" +
                    "EVN||20101020\n" +
                    "PID|||"+ patient.getIdentifier()+"^^^TEST||"+ patient.getLastName()+"^"+ patient.getFirstName()+"^^^^^L|SMITH^^^^^^L|19840125|F|||123 Main Street West ^^NEWARK^NJ^30293||^PRN^PH^^^409^3049506||||||\n" +
                    "PV1||I";
            Message request = ClientRegistryUtil.loadMessage(message);
            response = ClientRegistryUtil.sendMessage(request);
        }
        catch(Exception e)
        {
            e.printStackTrace();
            fail();
        }

        return response;
    }

}
