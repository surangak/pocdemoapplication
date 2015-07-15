import org.ohie.pocdemo.form.model.ModifyXDSbMessage;
import org.ohie.pocdemo.form.model.Patient;
import org.ohie.pocdemo.form.model.Provider;

import javax.xml.xpath.XPathExpressionException;

public class Test {

    public static void main(String args[]) throws XPathExpressionException {
        ModifyXDSbMessage modify = new ModifyXDSbMessage(new Patient("1", "fname", "lname","5132894987946830603_123","2.16.840.1.113883.3.72.5.9.1", "19910606", "F"), new Provider("1", "123", "456"));
        modify.modify("OHIE-XDS-01-30.xml");
    }
}
