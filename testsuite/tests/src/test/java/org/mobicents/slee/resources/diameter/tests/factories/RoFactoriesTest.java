package org.mobicents.slee.resources.diameter.tests.factories;

import static org.jdiameter.client.impl.helpers.Parameters.AcctApplId;
import static org.jdiameter.client.impl.helpers.Parameters.ApplicationId;
import static org.jdiameter.client.impl.helpers.Parameters.Assembler;
import static org.jdiameter.client.impl.helpers.Parameters.AuthApplId;
import static org.jdiameter.client.impl.helpers.Parameters.OwnDiameterURI;
import static org.jdiameter.client.impl.helpers.Parameters.OwnRealm;
import static org.jdiameter.client.impl.helpers.Parameters.OwnVendorID;
import static org.jdiameter.client.impl.helpers.Parameters.PeerName;
import static org.jdiameter.client.impl.helpers.Parameters.PeerRating;
import static org.jdiameter.client.impl.helpers.Parameters.PeerTable;
import static org.jdiameter.client.impl.helpers.Parameters.RealmEntry;
import static org.jdiameter.client.impl.helpers.Parameters.RealmTable;
import static org.jdiameter.client.impl.helpers.Parameters.VendorId;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentity;
import net.java.slee.resource.diameter.ro.RoAvpFactory;
import net.java.slee.resource.diameter.ro.RoMessageFactory;
import net.java.slee.resource.diameter.ro.RoServerSessionActivity;
import net.java.slee.resource.diameter.ro.events.RoCreditControlAnswer;
import net.java.slee.resource.diameter.ro.events.RoCreditControlRequest;

import org.jdiameter.api.Answer;
import org.jdiameter.api.IllegalDiameterStateException;
import org.jdiameter.api.InternalException;
import org.jdiameter.api.OverloadException;
import org.jdiameter.api.Request;
import org.jdiameter.api.RouteException;
import org.jdiameter.api.Stack;
import org.jdiameter.api.acc.events.AccountAnswer;
import org.jdiameter.api.acc.events.AccountRequest;
import org.jdiameter.api.app.AppAnswerEvent;
import org.jdiameter.api.app.AppRequestEvent;
import org.jdiameter.api.app.AppSession;
import org.jdiameter.api.auth.events.AbortSessionAnswer;
import org.jdiameter.api.auth.events.AbortSessionRequest;
import org.jdiameter.api.auth.events.ReAuthAnswer;
import org.jdiameter.api.auth.events.ReAuthRequest;
import org.jdiameter.api.auth.events.SessionTermAnswer;
import org.jdiameter.api.auth.events.SessionTermRequest;
import org.jdiameter.api.cca.ServerCCASession;
import org.jdiameter.api.cca.events.JCreditControlRequest;
import org.jdiameter.api.ro.ServerRoSession;
import org.jdiameter.api.ro.ServerRoSessionListener;
import org.jdiameter.client.impl.helpers.EmptyConfiguration;
import org.jdiameter.common.api.app.ro.IRoMessageFactory;
import org.jdiameter.server.impl.app.ro.ServerRoSessionImpl;
import org.junit.Test;
import org.mobicents.diameter.dictionary.AvpDictionary;
import org.mobicents.slee.resource.diameter.base.DiameterAvpFactoryImpl;
import org.mobicents.slee.resource.diameter.base.DiameterMessageFactoryImpl;
import org.mobicents.slee.resource.diameter.ro.RoAvpFactoryImpl;
import org.mobicents.slee.resource.diameter.ro.RoMessageFactoryImpl;
import org.mobicents.slee.resource.diameter.ro.RoServerSessionActivityImpl;

/**
 * 
 * RoFactoriesTest.java
 *
 * <br>Project:  mobicents
 * <br>2:10:37 PM Apr 14, 2009 
 * <br>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class RoFactoriesTest implements IRoMessageFactory, ServerRoSessionListener {
  
  private static String clientHost = "127.0.0.1";
  private static String clientPort = "13868";
  private static String clientURI  = "aaa://" + clientHost + ":" + clientPort;
  
  private static String serverHost = "localhost";
  private static String serverPort = "3868";
  private static String serverURI = "aaa://" + serverHost + ":" + serverPort;
  
  private static String realmName = "mobicents.org";

  private static RoMessageFactory roMessageFactory;
  private static RoAvpFactory roAvpFactory;
  
  private static Stack stack;
  
  private static ServerRoSession session; 

  
  static
  {
    stack = new org.jdiameter.client.impl.StackImpl();
    try
    {
      stack.init(new MyConfiguration());
    }
    catch ( Exception e ) {
      throw new RuntimeException("Failed to initialize the stack.");
    }
    
    DiameterMessageFactoryImpl baseFactory = new DiameterMessageFactoryImpl(stack);
    DiameterAvpFactoryImpl baseAvpFactory = new DiameterAvpFactoryImpl();
    
    roAvpFactory = new RoAvpFactoryImpl(baseAvpFactory);
    try {
      roMessageFactory = new RoMessageFactoryImpl(baseFactory, stack.getSessionFactory().getNewSession().getSessionId(), stack);
    }
    catch ( Exception e ) {
      e.printStackTrace();
    }
    
    try
    {
      AvpDictionary.INSTANCE.parseDictionary( RoFactoriesTest.class.getClassLoader().getResourceAsStream( "dictionary.xml" ) );
    }
    catch ( Exception e ) {
      throw new RuntimeException("Failed to parse dictionary file.");
    }
  }
  
  private RoServerSessionActivity roServerSession = null;
  
  public RoFactoriesTest()
  {
    try {
      session = new ServerRoSessionImpl(this, stack.getSessionFactory(), this, null, null);
      roServerSession = new RoServerSessionActivityImpl(roMessageFactory.getBaseMessageFactory(), roAvpFactory.getBaseFactory(), session, new DiameterIdentity("127.0.0.2"), new DiameterIdentity("mobicents.org"), stack);
      ((RoServerSessionActivityImpl)roServerSession).fetchCurrentState(roMessageFactory.createRoCreditControlRequest());
    }
    catch (IllegalDiameterStateException e) {
      throw new RuntimeException("Failed to parse dictionary file.");
    }
  }
  
  @Test
  public void isRequestCCR() throws Exception
  {
    RoCreditControlRequest ccr = roMessageFactory.createRoCreditControlRequest();
    assertTrue("Request Flag in Credit-Control-Request is not set.", ccr.getHeader().isRequest());
  }
  
  @Test
  public void testGettersAndSettersCCR() throws Exception
  {
    RoCreditControlRequest ccr = roMessageFactory.createRoCreditControlRequest();
    
    int nFailures = AvpAssistant.testMethods(ccr, RoCreditControlRequest.class);
    
    assertTrue("Some methods have failed. See logs for more details.", nFailures == 0);
  }  
  
  @Test
  public void hasRoApplicationIdCCR() throws Exception
  {
    RoCreditControlRequest ccr = roMessageFactory.createRoCreditControlRequest();
    assertTrue("Auth-Application-Id AVP in Ro CCR must be 4, it is " + ccr.getAuthApplicationId(), ccr.getAuthApplicationId() == 4);
  }
  
  @Test
  public void isAnswerCCA() throws Exception
  {
    RoCreditControlAnswer cca = roServerSession.createRoCreditControlAnswer();
    assertFalse("Request Flag in Credit-Control-Answer is set.", cca.getHeader().isRequest());
  }

  @Test
  public void testGettersAndSettersCCA() throws Exception
  {
    RoCreditControlAnswer cca = roServerSession.createRoCreditControlAnswer();
    
    int nFailures = AvpAssistant.testMethods(cca, RoCreditControlAnswer.class);
    
    assertTrue("Some methods have failed. See logs for more details.", nFailures == 0);
  }  
  
  @Test
  public void hasRoApplicationIdCCA() throws Exception
  {
    RoCreditControlAnswer cca = roServerSession.createRoCreditControlAnswer();
    assertTrue("Auth-Application-Id AVP in Ro CCA must be 4, it is " + cca.getAuthApplicationId(), cca.getAuthApplicationId() == 4);
  }
  
  @Test
  public void hasDestinationHostCCA() throws Exception
  {
    RoCreditControlAnswer cca = roServerSession.createRoCreditControlAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", cca.getDestinationHost());    
  }

  @Test
  public void hasDestinationRealmCCA() throws Exception
  {
    RoCreditControlAnswer cca = roServerSession.createRoCreditControlAnswer();
    assertNull("The Destination-Host and Destination-Realm AVPs MUST NOT be present in the answer message. [RFC3588/6.2]", cca.getDestinationRealm());    
  }

  
  /**
   * Class representing the Diameter Configuration  
   */
  public static class MyConfiguration extends EmptyConfiguration 
  {
    public MyConfiguration() 
    {
      super();
      
      add(Assembler, Assembler.defValue());
      add(OwnDiameterURI, clientURI);
      add(OwnRealm, realmName);
      add(OwnVendorID, 193L);
      // Set Ericsson SDK feature
      //add(UseUriAsFqdn, true);
      // Set Common Applications
      add(ApplicationId,
          // AppId 1
          getInstance().
          add(VendorId,   193L).
          add(AuthApplId, 0L).
          add(AcctApplId, 19302L)
      );
      // Set peer table
      add(PeerTable,
          // Peer 1
          getInstance().
          add(PeerRating, 1).
          add(PeerName, serverURI));
      // Set realm table
      add(RealmTable,
          // Realm 1
          getInstance().
          add(RealmEntry, realmName + ":" + clientHost + "," + serverHost)
      );
    }
  }

  public ReAuthAnswer createReAuthAnswer( Answer answer )
  {
    // TODO Auto-generated method stub
    return null;
  }

  public ReAuthRequest createReAuthRequest( Request req )
  {
    // TODO Auto-generated method stub
    return null;
  }

  public long[] getApplicationIds()
  {
    return new long[]{RoMessageFactory._RO_AUTH_APP_ID};
  }

  public void doAbortSessionAnswer( ServerCCASession session, AbortSessionRequest request, AbortSessionAnswer answer ) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException
  {
    // TODO Auto-generated method stub
    
  }

  public void doAbortSessionRequest( ServerCCASession session, AbortSessionRequest request ) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException
  {
    // TODO Auto-generated method stub
    
  }

  public void doAccountingAnswer( ServerCCASession session, AccountRequest request, AccountAnswer answer ) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException
  {
    // TODO Auto-generated method stub
    
  }

  public void doAccountingRequest( ServerCCASession session, AccountRequest request ) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException
  {
    // TODO Auto-generated method stub
    
  }

  public void doCreditControlRequest( ServerCCASession session, JCreditControlRequest request ) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException
  {
    // TODO Auto-generated method stub
    
  }

  public void doOtherEvent( AppSession session, AppRequestEvent request, AppAnswerEvent answer ) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException
  {
    // TODO Auto-generated method stub
    
  }

  public void doReAuthAnswer( ServerCCASession session, ReAuthRequest request, ReAuthAnswer answer ) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException
  {
    // TODO Auto-generated method stub
    
  }

  public void doSessionTerminationAnswer( ServerCCASession session, SessionTermRequest request, SessionTermAnswer answer ) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException
  {
    // TODO Auto-generated method stub
    
  }

  public void doSessionTerminationRequest( ServerCCASession session, SessionTermRequest request ) throws InternalException, IllegalDiameterStateException, RouteException, OverloadException
  {
    // TODO Auto-generated method stub
    
  }

  public void doCreditControlRequest(ServerRoSession session, RoCreditControlRequest request) throws InternalException, IllegalDiameterStateException, RouteException,
      OverloadException {
    // TODO Auto-generated method stub
    
  }

  public void doReAuthAnswer(ServerRoSession session, ReAuthRequest request, ReAuthAnswer answer) throws InternalException, IllegalDiameterStateException, RouteException,
      OverloadException {
    // TODO Auto-generated method stub
    
  }

  public void doCreditControlRequest(ServerRoSession session, org.jdiameter.api.ro.events.RoCreditControlRequest request) throws InternalException, IllegalDiameterStateException,
      RouteException, OverloadException {
    // TODO Auto-generated method stub
    
  }

  public org.jdiameter.api.ro.events.RoCreditControlRequest createCreditControlRequest(Request request) {
    // TODO Auto-generated method stub
    return null;
  }

  public org.jdiameter.api.ro.events.RoCreditControlAnswer createCreditControlAnswer(Answer answer) {
    // TODO Auto-generated method stub
    return null;
  }

}
