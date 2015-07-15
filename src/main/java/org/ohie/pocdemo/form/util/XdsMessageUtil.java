package org.ohie.pocdemo.form.util;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.AbstractComposite;
import ca.uhn.hl7v2.model.Segment;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dcm4chee.xds2.common.XDSConstants;
import org.dcm4chee.xds2.infoset.ihe.ProvideAndRegisterDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetRequestType;
import org.dcm4chee.xds2.infoset.ihe.RetrieveDocumentSetResponseType;
import org.dcm4chee.xds2.infoset.rim.*;
import org.dcm4chee.xds2.infoset.util.DocumentRegistryPortTypeFactory;
import org.dcm4chee.xds2.infoset.util.DocumentRepositoryPortTypeFactory;
import org.dcm4chee.xds2.infoset.util.InfosetUtil;
import org.dcm4chee.xds2.infoset.ws.registry.DocumentRegistryPortType;
import org.dcm4chee.xds2.infoset.ws.repository.DocumentRepositoryPortType;
import org.junit.Assert;

import javax.xml.bind.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import static org.junit.Assert.fail;

/**
 * XDS Message utility
 */
public class XdsMessageUtil {

    // For localhost testing only, remove this static block if not
    // using a server on localhost
    static {
        javax.net.ssl.HttpsURLConnection
                .setDefaultHostnameVerifier(new javax.net.ssl.HostnameVerifier() {
                    public boolean verify(String hostname,
                                          javax.net.ssl.SSLSession sslSession) {
                        return true;
                    }
                });
    }

    private static String s_repositoryEndpoint = "http://iol.test.ohie.org:5001/xdsrepository";
    private static String s_registryEndpoint = "http://iol.test.ohie.org:5001/xdsregistry";

    private static final Log log = LogFactory.getLog(XdsMessageUtil.class);

    /**
     * Load a provide and register document type message from the file system
     * @param messageName
     * @return
     * @throws javax.xml.bind.JAXBException
     * @throws IOException
     */
    public static <T> T loadMessage(String messageName, Class<T> clazz) throws JAXBException, IOException
    {
        JAXBContext jaxbContext = JAXBContext.newInstance("org.dcm4chee.xds2.infoset.ihe:org.dcm4chee.xds2.infoset.rim");
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();

        URL fileUrl = XdsMessageUtil.class.getResource(String.format("/xds/%s.xml", messageName));
        File fileUnderTest = new File(fileUrl.getFile());
        FileInputStream fs = null;
        try
        {
            fs = new FileInputStream(fileUnderTest);
            //InputStream is = XdsMessageUtil.class.getResourceAsStream(String.format("/xds/%s.xml", messageName));
            Object unmarshalled = unmarshaller.unmarshal(fs);
            if(unmarshalled instanceof JAXBElement)
            {
                JAXBElement<T> request = (JAXBElement<T>) unmarshalled;
                return request.getValue();
            }
            else
                return (T)unmarshalled;
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException();
        }
        finally
        {
            if(fs != null)
                fs.close();
        }
    }

    /**
     * Update patient information in the XDS request
     * @param patientInfo
     * @throws JAXBException
     * @throws HL7Exception
     */
    public static void updatePnrPatientInfo(ProvideAndRegisterDocumentSetRequestType pnrRequest, Segment pidSegmentInfo, String documentId) throws JAXBException, HL7Exception
    {

        // Extrinsic object type
        AbstractComposite cx = (AbstractComposite)pidSegmentInfo.getField(3, 0);
        AbstractComposite hd = (AbstractComposite)cx.getComponent(3);

        String patientId = String.format("%s^^^&%s&ISO", cx.getComponent(0).encode(), hd.getComponent(1).encode()),
                mbun = new SimpleDateFormat("HHmmss").format(new Date());

        for(ExtrinsicObjectType eo : InfosetUtil.getExtrinsicObjects(pnrRequest.getSubmitObjectsRequest()))
        {

            Map<String, SlotType1> slots = InfosetUtil.getSlotsFromRegistryObject(eo);

            // Slot for sourcePatientInfop
            InfosetUtil.addOrOverwriteSlot(eo, XDSConstants.SLOT_NAME_SOURCE_PATIENT_ID, patientId);
            InfosetUtil.addOrOverwriteSlot(eo, XDSConstants.SLOT_NAME_SOURCE_PATIENT_INFO,
                    String.format("PID-3|%s", patientId),
                    String.format("PID-5|%s", pidSegmentInfo.getField(5, 0).encode()),
                    String.format("PID-7|%s", pidSegmentInfo.getField(7, 0).encode()),
                    String.format("PID-8|%s", pidSegmentInfo.getField(8, 0).encode())
            );
            InfosetUtil.addOrOverwriteSlot(eo, XDSConstants.SLOT_NAME_CREATION_TIME, mbun);

            // Set the external identifiers
            InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_patientId, patientId, eo);
            InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_uniqueId, documentId, eo);

        }

        RegistryPackageType pack = InfosetUtil.getRegistryPackage(pnrRequest.getSubmitObjectsRequest(), XDSConstants.UUID_XDSSubmissionSet);
        InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_patientId, patientId, pack);
        InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_sourceId, String.format("1.3.6.1.4.1.21367.2010.1.2.5.%s", mbun), pack);
        InfosetUtil.setExternalIdentifierValue(XDSConstants.UUID_XDSSubmissionSet_uniqueId, String.format("1.3.6.1.4.1.21367.2010.1.2.4.%s", mbun), pack);

    }


    /**
     * Send a provide and register document set type
     * @param request
     * @return
     */
    public static RetrieveDocumentSetResponseType retrieveDocumentSet(RetrieveDocumentSetRequestType request)
    {
        DocumentRepositoryPortType port = DocumentRepositoryPortTypeFactory.getDocumentRepositoryPortSoap12(s_repositoryEndpoint);

        try {
            return port.documentRepositoryRetrieveDocumentSet(request);
        } catch (Exception e) {

            e.printStackTrace();
            log.error(e);
            throw new RuntimeException("Document Repository not available: " + s_repositoryEndpoint, e);
        }

    }

    /**
     * Send a provide and register document set type
     * @param request
     * @return
     */
    public static RegistryResponseType provideAndRegister(ProvideAndRegisterDocumentSetRequestType request)
    {
        DocumentRepositoryPortType port = DocumentRepositoryPortTypeFactory.getDocumentRepositoryPortSoap12(s_repositoryEndpoint);

        try {
            RegistryResponseType rrt = port.documentRepositoryProvideAndRegisterDocumentSetB(request);
            System.out.println("S :" + rrt.getStatus());
            return rrt;

        } catch (Exception e) {

            e.printStackTrace();

            log.error(e);
            throw new RuntimeException("Document Repository not available: " + s_repositoryEndpoint, e);
        }

    }

    /**
     * Executes a registry stored query
     * @param request
     * @return
     */
    public static AdhocQueryResponse registryStoredQuery(AdhocQueryRequest request)
    {
        DocumentRegistryPortType port = DocumentRegistryPortTypeFactory.getDocumentRegistryPortSoap12(s_registryEndpoint);
        try
        {
            return port.documentRegistryRegistryStoredQuery(request);
        }
        catch(Exception e)
        {
            throw new RuntimeException("Document Registry not available: " + s_registryEndpoint, e);
        }
    }
    /**
     * Assert that there are no errors in the response
     * @param xdsResponse
     */
    public static void assertSuccess(RegistryResponseType xdsResponse) {
        Assert.assertEquals(XDSConstants.XDS_B_STATUS_SUCCESS, xdsResponse.getStatus());
    }

    /**
     * Update the query request with the specified data
     * @param queryRequest
     * @param segment
     * @throws HL7Exception
     * @throws JAXBException
     */
    public static void updateAdhocQueryRequest(AdhocQueryRequest queryRequest,
                                               Segment pidSegmentInfo) throws HL7Exception, JAXBException {

        String patientId = pidSegmentInfo.getField(3,0).encode();
        InfosetUtil.addOrOverwriteSlot(queryRequest.getAdhocQuery(), XDSConstants.QRY_DOCUMENT_ENTRY_PATIENT_ID, String.format("('%s')",patientId));

    }

    /**
     * Assert that the document response has the identified document unique identifier in the
     * meta0-data
     * @param documentId
     */
    /** public static void assertHasDocumentId(AdhocQueryResponse queryResponse, String documentId) {

     boolean hasMatch = false;
     for(JAXBElement<? extends IdentifiableType> jaxElement : queryResponse.getRegistryObjectList().getIdentifiable())
     {
     //System.out.println("aaaah " + jaxElement.getValue().getId());
     if(jaxElement.getValue() instanceof ExtrinsicObjectType) {

     Assert.assertEquals(jaxElement.getValue().getId(), documentId);

     //System.out.println((RegistryObjectType)jaxElement.getValue());
     //hasMatch |= InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_uniqueId, (RegistryObjectType)jaxElement.getValue()).equals(documentId);
     }

     }


     //(String.format("Document %s not found in the result set", documentId), hasMatch);
     }**/


    public static void assertHasDocumentId(AdhocQueryResponse queryResponse, String documentId) {

        boolean hasMatch = false;
        for(JAXBElement<? extends IdentifiableType> jaxElement : queryResponse.getRegistryObjectList().getIdentifiable())
        {
            System.out.println((RegistryObjectType)jaxElement.getValue());
            if(jaxElement.getValue() instanceof ExtrinsicObjectType) {
                System.out.println((RegistryObjectType)jaxElement.getValue());
                hasMatch |= InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_uniqueId, (RegistryObjectType)jaxElement.getValue()).equals(documentId);
            }

        }

        Assert.assertTrue(String.format("Document %s not found in the result set", documentId), hasMatch);
    }



    /**
     * Get the extrinsic object
     */
    /** private static ExtrinsicObjectType getExtrinsicObject(String documentId, AdhocQueryResponse queryResponse)
     {
     for(JAXBElement<? extends IdentifiableType> jaxElement : queryResponse.getRegistryObjectList().getIdentifiable()) {

     if (jaxElement.getValue() instanceof ExtrinsicObjectType) {
     if ((jaxElement.getValue().getId()).equals(documentId)) {
     return (ExtrinsicObjectType) jaxElement.getValue();
     }
     }
     }
     return null;
     }**/


    private static ExtrinsicObjectType getExtrinsicObject(String documentId, AdhocQueryResponse queryResponse)
    {
        for(JAXBElement<? extends IdentifiableType> jaxElement : queryResponse.getRegistryObjectList().getIdentifiable())
        {

            if(jaxElement.getValue() instanceof ExtrinsicObjectType && InfosetUtil.getExternalIdentifierValue(XDSConstants.UUID_XDSDocumentEntry_uniqueId, (RegistryObjectType)jaxElement.getValue()).equals(documentId))
                return (ExtrinsicObjectType)jaxElement.getValue();
        }
        return null;
    }

    /**
     * Create the retrivee requst
     * @param documentId
     * @param queryResponse
     */
    public static RetrieveDocumentSetRequestType createRetrieveRequest(String documentId,
                                                                       AdhocQueryResponse queryResponse) {

        RetrieveDocumentSetRequestType retVal = new RetrieveDocumentSetRequestType();
        ExtrinsicObjectType documentMetaData = getExtrinsicObject(documentId, queryResponse);
        Assert.assertNotNull("Missing document meta-data", documentMetaData);
        RetrieveDocumentSetRequestType.DocumentRequest dr = new RetrieveDocumentSetRequestType.DocumentRequest();
        dr.setDocumentUniqueId(documentId);
        System.out.println("Infoset  " + InfosetUtil.getSlotValue(documentMetaData.getSlot(), XDSConstants.SLOT_NAME_REPOSITORY_UNIQUE_ID, ""));
        dr.setRepositoryUniqueId(InfosetUtil.getSlotValue(documentMetaData.getSlot(), XDSConstants.SLOT_NAME_REPOSITORY_UNIQUE_ID, ""));
        retVal.getDocumentRequest().add(dr);

        return retVal;

    }

    /**
     * Assert the document entries returned match the document hash in the meta-data
     * @param queryResponse
     * @param retrieveResponse
     */
    public static void assertMatchesHash(AdhocQueryResponse queryResponse,
                                         RetrieveDocumentSetResponseType retrieveResponse) {
        for(RetrieveDocumentSetResponseType.DocumentResponse dr : retrieveResponse.getDocumentResponse())
        {
            ExtrinsicObjectType eot = getExtrinsicObject(dr.getDocumentUniqueId(), queryResponse);
            System.out.println("ee" + dr.getDocumentUniqueId());
            Assert.assertNotNull("Missing document meta-data in query", eot);
            // Get the hash
            String hashValue = InfosetUtil.getSlotValue(eot.getSlot(), XDSConstants.SLOT_NAME_HASH, null),
                    sizeValue = InfosetUtil.getSlotValue(eot.getSlot(), XDSConstants.SLOT_NAME_SIZE,  null);

            // No size or hash is only allowed in ODD
            if(sizeValue == null || hashValue == null)
                Assert.assertEquals("urn:uuid:34268e47-fdf5-41a6-ba33-82133c465248", eot.getObjectType());
            else
            {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                try
                {
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    dr.getDocument().writeTo(bos);
                    digest.update(bos.toByteArray());
                    String calculatedHash = DatatypeConverter.printHexBinary(digest.digest());
                    Assert.assertEquals(Integer.parseInt(sizeValue), bos.size());
                    System.out.println(Integer.parseInt(sizeValue));
                    System.out.println(bos.size());
                    Assert.assertEquals(hashValue, calculatedHash);
                    log.info("The document size: " + bos.size());
                }
                catch(Exception e)
                {
                    e.printStackTrace();
                    log.error(e);
                    fail();
                }
                finally
                {
                    try {
                        bos.close();
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }

            }
        }
    }
}