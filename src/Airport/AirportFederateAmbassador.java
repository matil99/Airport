package Airport;

import Others.Plane;
import hla.rti1516e.*;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.HLAfloat32BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;
import org.portico.impl.hla1516e.types.encoding.HLA1516eFloat32BE;
import org.portico.impl.hla1516e.types.encoding.HLA1516eInteger32BE;

public class AirportFederateAmbassador extends NullFederateAmbassador
{
    //----------------------------------------------------------
    //                    STATIC VARIABLES
    //----------------------------------------------------------

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private final AirportFederate federate;

    // these variables are accessible in the package
    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    protected boolean isRunning       = true;

    //----------------------------------------------------------
    //                      CONSTRUCTORS
    //----------------------------------------------------------

    public AirportFederateAmbassador(AirportFederate federate )
    {
        this.federate = federate;
    }

    //----------------------------------------------------------
    //                    INSTANCE METHODS
    //----------------------------------------------------------
    private void log( String message )
    {
        System.out.println( "FederateAmbassador: " + message );
    }

    //////////////////////////////////////////////////////////////////////////
    ////////////////////////// RTI Callback Methods //////////////////////////
    //////////////////////////////////////////////////////////////////////////
    @Override
    public void synchronizationPointRegistrationFailed( String label, SynchronizationPointFailureReason reason )
    {
        log( "Failed to register sync point: " + label + ", reason: "+reason );
    }

    @Override
    public void synchronizationPointRegistrationSucceeded( String label )
    {
        log( "Successfully registered sync point: " + label );
    }

    @Override
    public void announceSynchronizationPoint( String label, byte[] tag )
    {
        log( "Synchronization point announced: " + label );
        if( label.equals(AirportFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized( String label, FederateHandleSet failed )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(AirportFederate.READY_TO_RUN) )
            this.isReadyToRun = true;
    }

    /**
     * The RTI has informed us that time regulation is now enabled.
     */
    @Override
    public void timeRegulationEnabled( LogicalTime time )
    {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isRegulating = true;
    }

    @Override
    public void timeConstrainedEnabled( LogicalTime time )
    {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isConstrained = true;
    }

    @Override
    public void timeAdvanceGrant( LogicalTime time )
    {
        this.federateTime = ((HLAfloat64Time)time).getValue();
        this.isAdvancing = false;
    }

    @Override
    public void discoverObjectInstance( ObjectInstanceHandle theObject,
                                        ObjectClassHandle theObjectClass,
                                        String objectName )
            throws FederateInternalError
    {
        log( "Discovered Object: handle; " + theObject + ", classHandle: " +
                theObjectClass + ", name: " + objectName );
    }

    @Override
    public void reflectAttributeValues( ObjectInstanceHandle theObject,
                                        AttributeHandleValueMap theAttributes,
                                        byte[] tag,
                                        OrderType sentOrder,
                                        TransportationTypeHandle transport,
                                        SupplementalReflectInfo reflectInfo )
            throws FederateInternalError
    {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        reflectAttributeValues( theObject,
                theAttributes,
                tag,
                sentOrder,
                transport,
                null,
                sentOrder,
                reflectInfo );
    }

    @Override
    public void reflectAttributeValues( ObjectInstanceHandle theObject,
                                        AttributeHandleValueMap theAttributes,
                                        byte[] tag,
                                        OrderType sentOrdering,
                                        TransportationTypeHandle theTransport,
                                        LogicalTime time,
                                        OrderType receivedOrdering,
                                        SupplementalReflectInfo reflectInfo )
            throws FederateInternalError
    {
        StringBuilder builder = new StringBuilder( "Reflection for object:" );

        // print the handle
        builder.append( " handle: " + theObject );
        // print the tag
        builder.append( ", tag: " + new String(tag) );
        // print the time (if we have it) we'll get null if we are just receiving
        // a forwarded call from the other reflect callback above
        if( time != null )
        {
            builder.append( ", time: " + ((HLAfloat64Time)time).getValue() );
        }

        // print the attribute information
        builder.append( ", attributeCount: " + theAttributes.size() );
        builder.append( "\n" );
        for( AttributeHandle attributeHandle : theAttributes.keySet() )
        {
            // print the attribute handle
            builder.append( "\tattributeHandle: " );
            if( attributeHandle.equals(federate.maxDelayHandle) )
            {
                builder.append( attributeHandle );
                builder.append( " MaxDelay: " );
                HLAfloat32BE maxDelay = new HLA1516eFloat32BE();
                try
                {
                    maxDelay.decode(theAttributes.get(attributeHandle));
                } catch (DecoderException e)
                {
                    e.printStackTrace();
                }
                builder.append( maxDelay.getValue() );
                federate.airport.maxDelay = maxDelay.getValue();
            }
            if( attributeHandle.equals(federate.forwardedPlanesHandle) )
            {
                builder.append( attributeHandle );
                builder.append( " ForwardedPLanes: " );
                HLAinteger32BE forwardedPLanes = new HLA1516eInteger32BE();
                try
                {
                    forwardedPLanes.decode(theAttributes.get(attributeHandle));
                } catch (DecoderException e)
                {
                    e.printStackTrace();
                }
                builder.append( forwardedPLanes.getValue() );
                federate.airport.forwardedPlanes = forwardedPLanes.getValue();
            }
            if( attributeHandle.equals(federate.landingCountHandle) )
            {
                builder.append( attributeHandle );
                builder.append( " LandingCount: " );
                HLAinteger32BE landingCount = new HLA1516eInteger32BE();
                try
                {
                    landingCount.decode(theAttributes.get(attributeHandle));
                } catch (DecoderException e)
                {
                    e.printStackTrace();
                }
                builder.append( landingCount.getValue() );
                federate.airport.landingCount = landingCount.getValue();
            }
            if( attributeHandle.equals(federate.emergencyCountHandle) )
            {
                builder.append( attributeHandle );
                builder.append( " EmergencyCount: " );
                HLAinteger32BE emergencyCount = new HLA1516eInteger32BE();
                try
                {
                    emergencyCount.decode(theAttributes.get(attributeHandle));
                } catch (DecoderException e)
                {
                    e.printStackTrace();
                }
                builder.append( emergencyCount.getValue() );
                federate.airport.emergencyCount = emergencyCount.getValue();
            }
            builder.append( "\n" );
        }
        federate.airport.repaint();
        log( builder.toString() );
    }

    @Override
    public void receiveInteraction( InteractionClassHandle interactionClass,
                                    ParameterHandleValueMap theParameters,
                                    byte[] tag,
                                    OrderType sentOrdering,
                                    TransportationTypeHandle theTransport,
                                    SupplementalReceiveInfo receiveInfo )
            throws FederateInternalError
    {
        // just pass it on to the other method for printing purposes
        // passing null as the time will let the other method know it
        // it from us, not from the RTI
        this.receiveInteraction( interactionClass,
                theParameters,
                tag,
                sentOrdering,
                theTransport,
                null,
                sentOrdering,
                receiveInfo );
    }

    @Override
    public void receiveInteraction( InteractionClassHandle interactionClass,
                                    ParameterHandleValueMap theParameters,
                                    byte[] tag,
                                    OrderType sentOrdering,
                                    TransportationTypeHandle theTransport,
                                    LogicalTime time,
                                    OrderType receivedOrdering,
                                    SupplementalReceiveInfo receiveInfo )
            throws FederateInternalError
    {
        StringBuilder builder = new StringBuilder( "Interaction Received: " );

        // print the handle
        builder.append(interactionClass );
        if( interactionClass.equals(federate.landingHandle) )
        {
            receiveLanding(interactionClass,theParameters,tag,sentOrdering,theTransport,time,receivedOrdering,receiveInfo, builder);
        }
        if( interactionClass.equals(federate.emergencyLandingHandle) )
        {
            receiveEmergencyLanding(interactionClass,theParameters,tag,sentOrdering,theTransport,time,receivedOrdering,receiveInfo, builder);
        }

        // print the tag
        builder.append( ", tag: " + new String(tag) );
        // print the time (if we have it) we'll get null if we are just receiving
        // a forwarded call from the other reflect callback above
        if( time != null )
        {
            builder.append( ", time: " + ((HLAfloat64Time)time).getValue() );
        }

        // print the parameer information
        builder.append( ", parameterCount: " + theParameters.size() );
        builder.append( "\n" );

        log( builder.toString() );
    }

    private void receiveLanding(InteractionClassHandle interactionClass,
                               ParameterHandleValueMap theParameters,
                               byte[] tag,
                               OrderType sentOrdering,
                               TransportationTypeHandle theTransport,
                               LogicalTime time,
                               OrderType receivedOrdering,
                               SupplementalReceiveInfo receiveInfo,
                               StringBuilder builder)
    {
        int idValue = 0;
        int typeValue = 0;
        float durationValue = 0;
        federate.airport.free = false;
        federate.airport.direction = 1;
        builder.append( " (Landing)" );
        for( ParameterHandle parameter : theParameters.keySet() )
        {
            if (parameter.equals(federate.landingIdHandle))
            {
                byte[] bytes = theParameters.get(federate.landingIdHandle);
                HLAinteger32BE id = new HLA1516eInteger32BE();
                try
                {
                    id.decode(bytes);
                } catch (DecoderException e)
                {
                    e.printStackTrace();
                }
                idValue = id.getValue();
                builder.append( ",\tID: " + idValue );
            }
            if (parameter.equals(federate.landingTypeHandle))
            {
                byte[] bytes = theParameters.get(federate.landingTypeHandle);
                HLAinteger32BE type = new HLA1516eInteger32BE();
                try
                {
                    type.decode(bytes);
                } catch (DecoderException e)
                {
                    e.printStackTrace();
                }
                typeValue = type.getValue();
                builder.append( ",\tType:" + typeValue );
            }
            if (parameter.equals(federate.landingDurationHandle))
            {
                byte[] bytes = theParameters.get(federate.landingDurationHandle);
                HLAfloat32BE duration = new HLA1516eFloat32BE();
                try
                {
                    duration.decode(bytes);
                } catch (DecoderException e)
                {
                    e.printStackTrace();
                }
                durationValue = duration.getValue();
                builder.append( ",\tDuration: " + durationValue );
            }
        }
        Plane plane = new Plane(idValue, typeValue,0, 0);
        federate.airport.land(plane, (float) federateTime, durationValue);
    }

    private void receiveEmergencyLanding(InteractionClassHandle interactionClass,
                                         ParameterHandleValueMap theParameters,
                                         byte[] tag,
                                         OrderType sentOrdering,
                                         TransportationTypeHandle theTransport,
                                         LogicalTime time,
                                         OrderType receivedOrdering,
                                         SupplementalReceiveInfo receiveInfo,
                                         StringBuilder builder)
    {
        int idValue = 0;
        int typeValue = 0;
        float durationValue = 0;
        federate.airport.free = false;
        federate.airport.direction = 2;
        builder.append( " (EmergencyLanding)" );
        for( ParameterHandle parameter : theParameters.keySet() )
        {
            if (parameter.equals(federate.emergencyIdHandle))
            {
                byte[] bytes = theParameters.get(federate.emergencyIdHandle);
                HLAinteger32BE id = new HLA1516eInteger32BE();
                try
                {
                    id.decode(bytes);
                } catch (DecoderException e)
                {
                    e.printStackTrace();
                }
                idValue = id.getValue();
                builder.append( ",\tID: " + idValue );
            }
            if (parameter.equals(federate.emergencyTypeHandle))
            {
                byte[] bytes = theParameters.get(federate.emergencyTypeHandle);
                HLAinteger32BE type = new HLA1516eInteger32BE();
                try
                {
                    type.decode(bytes);
                } catch (DecoderException e)
                {
                    e.printStackTrace();
                }
                typeValue = type.getValue();
                builder.append( ",\tType:" + typeValue );
            }
            if (parameter.equals(federate.emergencyDurationHandle))
            {
                byte[] bytes = theParameters.get(federate.emergencyDurationHandle);
                HLAfloat32BE duration = new HLA1516eFloat32BE();
                try
                {
                    duration.decode(bytes);
                } catch (DecoderException e)
                {
                    e.printStackTrace();
                }
                durationValue = duration.getValue();
                builder.append( ",\tDuration: " + durationValue );
            }
        }
        Plane plane = new Plane(idValue, typeValue,0, 0);
        federate.airport.land(plane, (float) federateTime, durationValue);
    }

    @Override
    public void removeObjectInstance( ObjectInstanceHandle theObject,
                                      byte[] tag,
                                      OrderType sentOrdering,
                                      SupplementalRemoveInfo removeInfo )
            throws FederateInternalError
    {
        log( "Object Removed: handle: " + theObject );
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
}
