package org.mobicents.slee.resource.diameter.sh.client;

import java.io.IOException;

import javax.slee.resource.SleeEndpoint;

import net.java.slee.resource.diameter.base.events.DiameterMessage;
import net.java.slee.resource.diameter.base.events.avp.DiameterIdentityAvp;
import net.java.slee.resource.diameter.sh.client.DiameterShAvpFactory;
import net.java.slee.resource.diameter.sh.client.ShClientMessageFactory;
import net.java.slee.resource.diameter.sh.client.ShClientSubscriptionActivity;
import net.java.slee.resource.diameter.sh.client.ShSessionState;
import net.java.slee.resource.diameter.sh.client.events.PushNotificationRequest;
import net.java.slee.resource.diameter.sh.client.events.avp.DataReferenceType;
import net.java.slee.resource.diameter.sh.client.events.avp.SubsReqType;
import net.java.slee.resource.diameter.sh.client.events.avp.UserIdentityAvp;
import net.java.slee.resource.diameter.sh.server.events.PushNotificationAnswer;
import net.java.slee.resource.diameter.sh.server.events.SubscribeNotificationsRequest;

import org.jdiameter.api.Answer;
import org.jdiameter.api.EventListener;
import org.jdiameter.api.Request;
import org.jdiameter.api.app.StateChangeListener;
import org.jdiameter.api.sh.ClientShSession;
import org.jdiameter.common.impl.app.sh.PushNotificationAnswerImpl;
import org.jdiameter.common.impl.app.sh.SubscribeNotificationsRequestImpl;
import org.mobicents.slee.resource.diameter.base.DiameterActivityImpl;
import org.mobicents.slee.resource.diameter.base.DiameterAvpFactoryImpl;
import org.mobicents.slee.resource.diameter.base.DiameterMessageFactoryImpl;
import org.mobicents.slee.resource.diameter.base.events.DiameterMessageImpl;
import org.mobicents.slee.resource.diameter.sh.client.handlers.ShClientSessionListener;

/**
 * 
 * ShClientSubscriptionActivityImpl.java
 *
 * @author <a href="mailto:baranowb@gmail.com"> Bartosz Baranowski </a>
 * @author <a href="mailto:brainslog@gmail.com"> Alexandre Mendonca </a>
 */
public class ShClientSubscriptionActivityImpl extends DiameterActivityImpl implements ShClientSubscriptionActivity, StateChangeListener {

	protected ClientShSession clientSession = null;
	protected ShSessionState state = ShSessionState.NOTSUBSCRIBED;
	protected ShClientSessionListener listener = null;
	protected DiameterShAvpFactory shAvpFactory = null;
	protected ShClientMessageFactory messageFactory = null;

	 // Last received message
  protected PushNotificationRequest lastMessage = null;
	
	public ShClientSubscriptionActivityImpl(DiameterMessageFactoryImpl messageFactory, ShClientMessageFactory shClientMessageFactory, DiameterAvpFactoryImpl avpFactory,
			DiameterShAvpFactory diameterShAvpFactory, ClientShSession session, long timeout, DiameterIdentityAvp destinationHost, DiameterIdentityAvp destinationRealm, SleeEndpoint endpoint)
	{
		super(messageFactory, avpFactory, null, (EventListener<Request, Answer>) session, timeout, destinationHost, destinationRealm, endpoint);
		this.clientSession = session;
		this.clientSession.addStateChangeNotification(this);
		super.setCurrentWorkingSession(this.clientSession.getSessions().get(0));
		this.shAvpFactory = diameterShAvpFactory;
		this.messageFactory = shClientMessageFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see net.java.slee.resource.diameter.sh.client.ShClientSubscriptionActivity#getSubscribedUserIdendity()
	 */
	public UserIdentityAvp getSubscribedUserIdendity()
	{
		return lastMessage != null ? lastMessage.getUserIdentity() : null;
	}

	/*
	 * (non-Javadoc)
	 * @see net.java.slee.resource.diameter.sh.client.ShClientSubscriptionActivity#sendPushNotificationAnswer(net.java.slee.resource.diameter.sh.server.events.PushNotificationAnswer)
	 */
	public void sendPushNotificationAnswer(PushNotificationAnswer answer) throws IOException
	{
		try
		{
	    DiameterMessageImpl msg = (DiameterMessageImpl) answer;
	    
			this.clientSession.sendPushNotificationAnswer(new PushNotificationAnswerImpl((Answer) msg.getGenericData()));
		}
		catch (Exception e) {
		  throw new IOException("Failure while trying to send Push-Notification-Answer: " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.java.slee.resource.diameter.sh.client.ShClientSubscriptionActivity#sendPushNotificationAnswer(long, boolean)
	 */
	public void sendPushNotificationAnswer(long resultCode, boolean isExperimentalResultCode) throws IOException
	{
    PushNotificationAnswer pna = this.messageFactory.createPushNotificationAnswer(resultCode, isExperimentalResultCode);

    setSessionData( pna );
    
		this.sendPushNotificationAnswer(pna);
	}

	/*
	 * (non-Javadoc)
	 * @see net.java.slee.resource.diameter.sh.client.ShClientSubscriptionActivity#sendSubscriptionNotificationRequest(net.java.slee.resource.diameter.sh.server.events.SubscribeNotificationsRequest)
	 */
	public void sendSubscriptionNotificationRequest(SubscribeNotificationsRequest request) throws IOException
	{
    try
    {
		  DiameterMessageImpl msg = (DiameterMessageImpl) request;
		  
			this.clientSession.sendSubscribeNotificationsRequest(new SubscribeNotificationsRequestImpl((Request) msg.getGenericData()));
		}
    catch (Exception e) {
      throw new IOException("Failure while trying to send Push-Notification-Answer: " + e.getMessage());
		}
	}

	/*
	 * (non-Javadoc)
	 * @see net.java.slee.resource.diameter.sh.client.ShClientSubscriptionActivity#sendUnsubscribeRequest()
	 */
	public void sendUnsubscribeRequest() throws IOException
	{
    try
    {
  	  // FIXME: Alexandre: How do we know DataReferenceType?
  	  SubscribeNotificationsRequest snr = this.messageFactory.createSubscribeNotificationsRequest( getSubscribedUserIdendity(), DataReferenceType.REPOSITORY_DATA, SubsReqType.UNSUBSCRIBE );
  	  
      DiameterMessageImpl msg = (DiameterMessageImpl) snr;
  
      this.clientSession.sendSubscribeNotificationsRequest(new SubscribeNotificationsRequestImpl((Request) msg.getGenericData()));
    }
    catch (Exception e) {
      throw new IOException("Failure while trying to send Subscribe-Notifications-Request for unsubscribing: " + e.getMessage());
    }
	}

	/*
	 * (non-Javadoc)
	 * @see org.jdiameter.api.app.StateChangeListener#stateChanged(java.lang.Enum, java.lang.Enum)
	 */
	public void stateChanged(Enum oldState, Enum newState)
	{
		org.jdiameter.common.api.app.sh.ShSessionState shNewState = (org.jdiameter.common.api.app.sh.ShSessionState) newState;
		
		switch (shNewState)
		{
		case NOTSUBSCRIBED:
			break;
		case SUBSCRIBED:
			state = ShSessionState.SUBSCRIBED;
			// FIXME: error?
			break;
		case TERMINATED:
			state = ShSessionState.TERMINATED;
			listener.sessionDestroyed(getSessionId(), clientSession);
			break;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.slee.resource.diameter.base.DiameterActivityImpl#getSessionListener()
	 */
	public Object getSessionListener()
	{
		return this.listener;
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.slee.resource.diameter.base.DiameterActivityImpl#setSessionListener(java.lang.Object)
	 */
	public void setSessionListener(Object ra)
	{
		this.listener = (ShClientSessionListener) ra;
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.slee.resource.diameter.base.DiameterActivityImpl#endActivity()
	 */
	public void endActivity()
	{
		this.clientSession.release();
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.slee.resource.diameter.base.DiameterActivityImpl#getDiameterAvpFactory()
	 */
	public Object getDiameterAvpFactory()
	{
		return this.shAvpFactory;
	}

	/*
	 * (non-Javadoc)
	 * @see org.mobicents.slee.resource.diameter.base.DiameterActivityImpl#getDiameterMessageFactory()
	 */
	public Object getDiameterMessageFactory()
	{
		return this.messageFactory;
	}

	/**
	 * 
	 * @return
	 */
	ClientShSession getClientSession()
	{
		return this.clientSession;
	}

	/**
	 * 
	 * @param request
	 */
	void fetchSubscriptionData(PushNotificationRequest request)
	{
	  lastMessage = request;
	}

	/**
	 * Sets session data obtained from last received message into parameter message.
	 * 
	 * @param message the message to be changed with session data
	 * @return true if changes were made, false otherwise
	 */
  private boolean setSessionData(DiameterMessage message)
  {
    // Just some sanity checks...
    if(lastMessage != null && lastMessage.getCommand().getCode() == message.getCommand().getCode())
    {
      message.getHeader().setEndToEndId( lastMessage.getHeader().getEndToEndId() );
      message.getHeader().setHopByHopId( lastMessage.getHeader().getHopByHopId() );
      message.setSessionId( lastMessage.getSessionId() );
      
      return true;
    }
    return false;
  }
}
