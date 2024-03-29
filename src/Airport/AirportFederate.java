package Airport;

import Others.Plane;
import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
import hla.rti1516e.encoding.HLAboolean;
import hla.rti1516e.encoding.HLAfloat32BE;
import hla.rti1516e.encoding.HLAinteger32BE;
import hla.rti1516e.exceptions.FederatesCurrentlyJoined;
import hla.rti1516e.exceptions.FederationExecutionAlreadyExists;
import hla.rti1516e.exceptions.FederationExecutionDoesNotExist;
import hla.rti1516e.exceptions.RTIexception;
import hla.rti1516e.time.HLAfloat64Interval;
import hla.rti1516e.time.HLAfloat64Time;
import hla.rti1516e.time.HLAfloat64TimeFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;

public class AirportFederate
{
    //----------------------------------------------------------
    //                    STATIC VARIABLES
    //----------------------------------------------------------

    /** The sync point all federates will sync up on before starting */
    public static final String READY_TO_RUN = "ReadyToRun";

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private RTIambassador rtiamb;
    private AirportFederateAmbassador fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    /*Airstrip and attributes*/
    protected ObjectClassHandle airstripHandle;
    protected ObjectInstanceHandle objectAirstrip;
    protected AttributeHandle freeHandle;
    protected AttributeHandle freeWindowHandle;
    protected AttributeHandle availablePassengerHandle;
    protected AttributeHandle availableSpecialHandle;

    /*StatsPackage and attributes*/
    protected ObjectClassHandle statsPackageHandle;
    protected ObjectInstanceHandle objectStatsPackage;
    protected AttributeHandle maxDelayHandle;
    protected AttributeHandle forwardedPlanesHandle;
    protected AttributeHandle landingCountHandle;
    protected AttributeHandle emergencyCountHandle;

    /*TakeOff and parameters*/
    protected InteractionClassHandle takeOffHandle;
    protected ParameterHandle takeOffIdHandle;
    protected ParameterHandle takeOffDelayHandle;

    /*Landing and parameters*/
    protected InteractionClassHandle landingHandle;
    protected ParameterHandle landingIdHandle;
    protected ParameterHandle landingTypeHandle;
    protected ParameterHandle landingDurationHandle;

    /*EmergencyLanding and parameters*/
    protected InteractionClassHandle emergencyLandingHandle;
    protected ParameterHandle emergencyIdHandle;
    protected ParameterHandle emergencyTypeHandle;
    protected ParameterHandle emergencyDurationHandle;

    /*Airport object*/
    protected Airport airport;


    //----------------------------------------------------------
    //                    INSTANCE METHODS
    //----------------------------------------------------------
    /**
     * This is just a helper method to make sure all logging it output in the same form
     */
    private void log( String message )
    {
        System.out.println( "AirportFederate   : " + message );
    }

    /**
     * This method will block until the user presses enter
     */
    private void waitForUser()
    {
        log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            reader.readLine();
        }
        catch( Exception e )
        {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    ////////////////////////// Main Simulation Method /////////////////////////
    ///////////////////////////////////////////////////////////////////////////
    /**
     * This is the main simulation loop. It can be thought of as the main method of
     * the federate. For a description of the basic flow of this federate, see the
     * class level comments
     */
    public void runFederate( String federateName ) throws Exception
    {
        /////////////////////////////////////////////////
        // 1 & 2. create the RTIambassador and Connect //
        /////////////////////////////////////////////////
        log( "Creating RTIambassador" );
        rtiamb = RtiFactoryFactory.getRtiFactory().getRtiAmbassador();
        encoderFactory = RtiFactoryFactory.getRtiFactory().getEncoderFactory();

        // connect
        log( "Connecting..." );
        fedamb = new AirportFederateAmbassador( this );
        rtiamb.connect( fedamb, CallbackModel.HLA_EVOKED );

        //////////////////////////////
        // 3. create the federation //
        //////////////////////////////
        log( "Creating Federation..." );
        // We attempt to create a new federation with the first three of the
        // restaurant FOM modules covering processes, food and drink
        try
        {
            URL[] modules = new URL[]
                    {
                    (new File("foms/Airport.xml")).toURI().toURL(),
            };

            rtiamb.createFederationExecution( "AirportFederation", modules );
            log( "Created Federation" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            log( "Didn't create federation, it already existed" );
        }
        catch( MalformedURLException urle )
        {
            log( "Exception loading one of the FOM modules from disk: " + urle.getMessage() );
            urle.printStackTrace();
            return;
        }

        ////////////////////////////
        // 4. join the federation //
        ////////////////////////////
        rtiamb.joinFederationExecution( federateName,"airport","AirportFederation");

        log( "Joined Federation as " + federateName );

        // cache the time factory for easy access
        this.timeFactory = (HLAfloat64TimeFactory)rtiamb.getTimeFactory();

        ////////////////////////////////
        // 5. announce the sync point //
        ////////////////////////////////
        // announce a sync point to get everyone on the same page. if the point
        // has already been registered, we'll get a callback saying it failed,
        // but we don't care about that, as long as someone registered it
        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );
        // wait until the point is announced
        while(!fedamb.isAnnounced)
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        // WAIT FOR USER TO KICK US OFF
        // So that there is time to add other federates, we will wait until the
        // user hits enter before proceeding. That was, you have time to start
        // other federates.
        waitForUser();

        ///////////////////////////////////////////////////////
        // 6. achieve the point and wait for synchronization //
        ///////////////////////////////////////////////////////
        // tell the RTI we are ready to move past the sync point and then wait
        // until the federation has synchronized on
        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while(!fedamb.isReadyToRun)
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        /////////////////////////////
        // 7. enable time policies //
        /////////////////////////////
        // in this section we enable/disable all time policies
        // note that this step is optional!
        enableTimePolicy();
        log( "Time Policy Enabled" );

        //////////////////////////////
        // 8. publish and subscribe //
        //////////////////////////////
        // in this section we tell the RTI of all the data we are going to
        // produce, and all the data we want to know about
        publishAndSubscribe();
        log( "Published and Subscribed" );

        /////////////////////////////////////
        // 9. register an object to update //
        /////////////////////////////////////
        objectAirstrip = rtiamb.registerObjectInstance( airstripHandle );
        log( "Registered Airstrip, handle: " + objectAirstrip );

        /////////////////////////////////////
        // 10. do the main simulation loop //
        /////////////////////////////////////
        airport = new Airport(15, 25, 20);
        while( fedamb.isRunning )
        {
            if (airport.free && (fedamb.federateTime >= airport.getTakeOffTime()) && airport.getTakeOffQueueSize() != 0)
            {
                Plane plane = airport.takeOff((float) fedamb.federateTime);
                takeOff(plane);
                updateAirstrip();
            }
            if (fedamb.federateTime == airport.getReleaseTime())
            {
                airport.release();
                updateAirstrip();
                log("Airstrip is now free.");
            }
            updateAirstripFreeWindow();
            advanceTime(1);
            log( "Time Advanced to " + fedamb.federateTime );
        }

        //////////////////////////////////////
        // 11. delete the object we created //
        //////////////////////////////////////
//		deleteObject( objectHandle );
//		log( "Deleted Object, handle=" + objectHandle );

        ////////////////////////////////////
        // 12. resign from the federation //
        ////////////////////////////////////
        rtiamb.resignFederationExecution( ResignAction.DELETE_OBJECTS );
        log( "Resigned from Federation" );

        ////////////////////////////////////////
        // 13. try and destroy the federation //
        ////////////////////////////////////////
        // NOTE: we won't die if we can't do this because other federates
        //       remain. in that case we'll leave it for them to clean up
        try
        {
            rtiamb.destroyFederationExecution( "AirportFederation" );
            log( "Destroyed Federation" );
        }
        catch( FederationExecutionDoesNotExist dne )
        {
            log( "No need to destroy federation, it doesn't exist" );
        }
        catch( FederatesCurrentlyJoined fcj )
        {
            log( "Didn't destroy federation, federates still joined" );
        }
    }

    ////////////////////////////////////////////////////////////////////////////
    ////////////////////////////// Helper Methods //////////////////////////////
    ////////////////////////////////////////////////////////////////////////////
    /**
     * This method will attempt to enable the various time related properties for
     * the federate
     */
    private void enableTimePolicy() throws Exception
    {
        // NOTE: Unfortunately, the LogicalTime/LogicalTimeInterval create code is
        //       Portico specific. You will have to alter this if you move to a
        //       different RTI implementation. As such, we've isolated it into a
        //       method so that any change only needs to happen in a couple of spots
        HLAfloat64Interval lookahead = timeFactory.makeInterval( fedamb.federateLookahead );

        ////////////////////////////
        // enable time regulation //
        ////////////////////////////
        this.rtiamb.enableTimeRegulation( lookahead );

        // tick until we get the callback
        while(!fedamb.isRegulating)
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }

        /////////////////////////////
        // enable time constrained //
        /////////////////////////////
        this.rtiamb.enableTimeConstrained();

        // tick until we get the callback
        while(!fedamb.isConstrained)
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );
        }
    }

    private void updateAirstrip() throws Exception
    {
        // update Airstrip parameters max and available to current values
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(4);

        HLAboolean freeValue = encoderFactory.createHLAboolean( airport.getFree());
        attributes.put( freeHandle, freeValue.toByteArray() );

        HLAfloat32BE freeWindowValue;
        if (airport.getTakeOffQueueSize() == 0)
        {
            freeWindowValue = encoderFactory.createHLAfloat32BE((float) (Integer.MAX_VALUE));
        }
        else
        {
            float window = (float) (airport.getTakeOffTime() - fedamb.federateTime);
            if (window < 0)
            {
                window = 0;
            }
            freeWindowValue = encoderFactory.createHLAfloat32BE(window);
        }
        attributes.put( freeWindowHandle, freeWindowValue.toByteArray() );

        HLAinteger32BE passengerValue = encoderFactory.createHLAinteger32BE( airport.getAvailablePassenger());
        attributes.put(availablePassengerHandle, passengerValue.toByteArray() );

        HLAinteger32BE specialValue = encoderFactory.createHLAinteger32BE( airport.getAvailableSpecial());
        attributes.put(availableSpecialHandle, specialValue.toByteArray() );

        rtiamb.updateAttributeValues( objectAirstrip, attributes, generateTag(), timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead));
    }
    private void updateAirstripFreeWindow() throws Exception
    {
        // update Airstrip parameters max and available to current values
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(1);

        HLAfloat32BE freeWindowValue;
        if (airport.getTakeOffQueueSize() == 0)
        {
            freeWindowValue = encoderFactory.createHLAfloat32BE((float) (Integer.MAX_VALUE));
        }
        else
        {
            float window = (float) (airport.getTakeOffTime() - fedamb.federateTime);
            if (window < 0)
            {
                window = 0;
            }
            freeWindowValue = encoderFactory.createHLAfloat32BE(window);
        }
        attributes.put( freeWindowHandle, freeWindowValue.toByteArray() );
        rtiamb.updateAttributeValues( objectAirstrip, attributes, generateTag(), timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead));
    }
    private void takeOff(Plane plane) throws Exception
    {
        airport.free = false;
        ParameterHandleValueMap parameterHandleValueMap = rtiamb.getParameterHandleValueMapFactory().create(2);
        ParameterHandle takeOffIdHandle = rtiamb.getParameterHandle(takeOffHandle, "id");
        HLAinteger32BE id = encoderFactory.createHLAinteger32BE(plane.getId());
        parameterHandleValueMap.put(takeOffIdHandle, id.toByteArray());
        ParameterHandle takeOffDelayHandle = rtiamb.getParameterHandle(takeOffHandle, "delay");
        HLAfloat32BE delay = encoderFactory.createHLAfloat32BE((float) (fedamb.federateTime - plane.getStartTime()));
        parameterHandleValueMap.put(takeOffDelayHandle, delay.toByteArray());
        rtiamb.sendInteraction(takeOffHandle, parameterHandleValueMap, generateTag(), timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead));
        log(plane + " take off with deley " + (fedamb.federateTime - plane.getStartTime()));
    }


    /**
     * This method will inform the RTI about the types of data that the federate will
     * be creating, and the types of data we are interested in hearing about as other
     * federates produce it.
     */
    private void publishAndSubscribe() throws RTIexception
    {
        /*Publish Airstrip object*/
        this.airstripHandle = rtiamb.getObjectClassHandle( "HLAobjectRoot.Airstrip" );
        this.freeHandle = rtiamb.getAttributeHandle( airstripHandle, "free" );
        this.freeWindowHandle = rtiamb.getAttributeHandle( airstripHandle, "freeWindow" );
        this.availablePassengerHandle = rtiamb.getAttributeHandle( airstripHandle, "availablePassenger" );
        this.availableSpecialHandle = rtiamb.getAttributeHandle( airstripHandle, "availableSpecial" );

        /*Package attributes*/
        AttributeHandleSet airstripAttributes = rtiamb.getAttributeHandleSetFactory().create();
        airstripAttributes.add(freeHandle);
        airstripAttributes.add(freeWindowHandle);
        airstripAttributes.add(availablePassengerHandle);
        airstripAttributes.add(availableSpecialHandle);
        rtiamb.publishObjectClassAttributes( airstripHandle, airstripAttributes );

        /*Subscribe StatsPackage object*/
        this.statsPackageHandle = rtiamb.getObjectClassHandle( "HLAobjectRoot.StatsPackage" );
        this.maxDelayHandle= rtiamb.getAttributeHandle( statsPackageHandle, "maxDelay" );
        this.forwardedPlanesHandle = rtiamb.getAttributeHandle( statsPackageHandle, "forwardedPlanes" );
        this.landingCountHandle = rtiamb.getAttributeHandle( statsPackageHandle, "landingCount" );
        this.emergencyCountHandle = rtiamb.getAttributeHandle( statsPackageHandle, "emergencyCount" );

        /*Package the information into a handle set*/
        AttributeHandleSet statsPackageAttributes = rtiamb.getAttributeHandleSetFactory().create();
        statsPackageAttributes.add( maxDelayHandle );
        statsPackageAttributes.add( forwardedPlanesHandle );
        statsPackageAttributes.add( landingCountHandle );
        statsPackageAttributes.add( emergencyCountHandle );
        rtiamb.subscribeObjectClassAttributes( statsPackageHandle, statsPackageAttributes );

        /*Publish TakeOff Interaction*/
        String takeOff = "HLAinteractionRoot.PlanesManagment.TakeOff";
        takeOffHandle = rtiamb.getInteractionClassHandle( takeOff );
        takeOffIdHandle = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.TakeOff"), "id");
        takeOffDelayHandle = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.TakeOff"), "delay");
        rtiamb.publishInteractionClass(takeOffHandle);

        /*Subscribe Landing Interaction*/
        String landingName = "HLAinteractionRoot.PlanesManagment.Landing";
        landingHandle = rtiamb.getInteractionClassHandle( landingName );
        landingIdHandle = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.Landing"), "id");
        landingTypeHandle = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.Landing"), "type");
        landingDurationHandle  = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.Landing"),"duration");
        rtiamb.subscribeInteractionClass(landingHandle);

        /*Subscribe EmergencyLanding Interaction*/
        String emergencyLandingName = "HLAinteractionRoot.PlanesManagment.EmergencyLanding";
        emergencyLandingHandle = rtiamb.getInteractionClassHandle( emergencyLandingName );
        emergencyIdHandle = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.EmergencyLanding"), "id");
        emergencyTypeHandle = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.EmergencyLanding"), "type");
        emergencyDurationHandle  = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.EmergencyLanding"),"duration");
        rtiamb.subscribeInteractionClass(emergencyLandingHandle);
    }

    /**
     * This method will request a time advance to the current time, plus the given
     * timestep. It will then wait until a notification of the time advance grant
     * has been received.
     */
    private void advanceTime( double timestep ) throws RTIexception
    {
        // request the advance
        fedamb.isAdvancing = true;
        HLAfloat64Time time = timeFactory.makeTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( time );

        // wait for the time advance to be granted. ticking will tell the
        // LRC to start delivering callbacks to the federate
        while( fedamb.isAdvancing )
        {
            rtiamb.evokeMultipleCallbacks( 0.1, 0.2 );

        }
    }

    private short getTimeAsShort()
    {
        return (short)fedamb.federateTime;
    }

    private byte[] generateTag()
    {
        return ("(timestamp) "+System.currentTimeMillis()).getBytes();
    }

    //----------------------------------------------------------
    //                     STATIC METHODS
    //----------------------------------------------------------
    public static void main( String[] args )
    {
        // get a federate name, use "exampleFederate" as default
        String federateName = "Airport";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            // run the example federate
            new AirportFederate().runFederate( federateName );
        }
        catch( Exception rtie )
        {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}
