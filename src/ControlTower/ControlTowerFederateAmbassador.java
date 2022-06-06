package ControlTower;

import hla.rti1516e.*;
import hla.rti1516e.encoding.DecoderException;
import hla.rti1516e.encoding.HLAfloat32BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederateInternalError;
import hla.rti1516e.time.HLAfloat64Time;
import org.portico.impl.hla1516e.types.encoding.HLA1516eFloat32BE;
import org.portico.impl.hla1516e.types.encoding.HLA1516eInteger32BE;

import static java.lang.Math.max;

public class ControlTowerFederateAmbassador  extends NullFederateAmbassador
{
    //----------------------------------------------------------
    //                    STATIC VARIABLES
    //----------------------------------------------------------

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private final ControlTowerFederate federate;

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

    public ControlTowerFederateAmbassador(ControlTowerFederate federate )
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
    public void synchronizationPointRegistrationFailed( String label,
                                                        SynchronizationPointFailureReason reason )
    {
        log( "Failed to register sync point: " + label + ", reason=: "+reason );
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
        if( label.equals(ControlTowerFederate.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    @Override
    public void federationSynchronized( String label, FederateHandleSet failed )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(ControlTowerFederate.READY_TO_RUN) )
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
        log( "Discovered Object: handle: " + theObject + ", classHandle: " +
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


        // print the attribute information
        builder.append( ", attributeCount: " + theAttributes.size() );
        builder.append( "\n" );
        for( AttributeHandle attributeHandle : theAttributes.keySet() )
        {
            // print the attribute handle
            builder.append( "\tattributeHandle: " );
            builder.append( "\n" );
        }

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
        builder.append(interactionClass );

        int idValue;
        float delayValue;
        if ( interactionClass.equals(federate.forwardHandle) )
        {
            for( ParameterHandle parameter : theParameters.keySet() )
            {
                if (parameter.equals(federate.forwardIdHandle))
                {
                    byte[] bytes = theParameters.get(federate.forwardIdHandle);
                    HLAinteger32BE id = new HLA1516eInteger32BE();
                    try {
                        id.decode(bytes);
                    } catch (DecoderException e) {
                        e.printStackTrace();
                    }
                    idValue = id.getValue();
                    builder.append(",\tID: " + idValue);
                }
            }
            federate.controlTower.forwardedPlanes++;
        }
        if ( interactionClass.equals(federate.takeOffHandle) )
        {
            for( ParameterHandle parameter : theParameters.keySet() )
            {
                if (parameter.equals(federate.takeOffIdHandle))
                {
                    byte[] bytes = theParameters.get(federate.takeOffIdHandle);
                    HLAinteger32BE id = new HLA1516eInteger32BE();
                    try {
                        id.decode(bytes);
                    } catch (DecoderException e) {
                        e.printStackTrace();
                    }
                    idValue = id.getValue();
                    builder.append(",\tID: " + idValue);
                }
                if (parameter.equals(federate.takeOffDelayHandle))
                {
                    byte[] bytes = theParameters.get(federate.takeOffDelayHandle);
                    HLAfloat32BE delay = new HLA1516eFloat32BE();
                    try {
                        delay.decode(bytes);
                    } catch (DecoderException e) {
                        e.printStackTrace();
                    }
                    delayValue = delay.getValue();
                    federate.controlTower.maxDelay = max(federate.controlTower.maxDelay, delayValue);
                    builder.append(",\tDelay: " + delayValue);
                }
            }
        }
        if( interactionClass.equals(federate.landingHandle) )
        {
            for( ParameterHandle parameter : theParameters.keySet() )
            {
                if (parameter.equals(federate.landingIdHandle))
                {
                    byte[] bytes = theParameters.get(federate.landingIdHandle);
                    HLAinteger32BE id = new HLA1516eInteger32BE();
                    try {
                        id.decode(bytes);
                    } catch (DecoderException e) {
                        e.printStackTrace();
                    }
                    idValue = id.getValue();
                    builder.append(",\tID: " + idValue);
                }
            }
            federate.controlTower.landingCount++;
        }
        if( interactionClass.equals(federate.emergencyLandingHandle) )
        {
            for( ParameterHandle parameter : theParameters.keySet() )
            {
                if (parameter.equals(federate.emergencyIdHandle))
                {
                    byte[] bytes = theParameters.get(federate.emergencyIdHandle);
                    HLAinteger32BE id = new HLA1516eInteger32BE();
                    try {
                        id.decode(bytes);
                    } catch (DecoderException e) {
                        e.printStackTrace();
                    }
                    idValue = id.getValue();
                    builder.append(",\tID: " + idValue);
                }
            }
            federate.controlTower.emergencyCount++;
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