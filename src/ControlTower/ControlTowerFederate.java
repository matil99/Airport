package ControlTower;

import hla.rti1516e.*;
import hla.rti1516e.encoding.EncoderFactory;
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

public class ControlTowerFederate {
    //----------------------------------------------------------
    //                    STATIC VARIABLES
    //----------------------------------------------------------

    /** The sync point all federates will sync up on before starting */
    public static final String READY_TO_RUN = "ReadyToRun";

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    private RTIambassador rtiamb;
    private ControlTowerFederateAmbassador fedamb;  // created when we connect
    private HLAfloat64TimeFactory timeFactory; // set when we join
    protected EncoderFactory encoderFactory;     // set when we join

    // caches of handle types - set once we join a federation
    protected ObjectClassHandle statsPackageHandle;
    protected ObjectInstanceHandle objectStatsPackage;
    protected AttributeHandle maxDelayHandle;
    protected AttributeHandle forwardedPlanesHandle;
    protected AttributeHandle landingCountHandle;
    protected AttributeHandle emergencyCountHandle;

    protected InteractionClassHandle forwardHandle;
    protected InteractionClassHandle takeOffHandle;
    protected InteractionClassHandle landingHandle;
    protected InteractionClassHandle emergencyLandingHandle;

    protected ParameterHandle landingIdHandle;
    protected ParameterHandle landingTypeHandle;
    protected ParameterHandle landingDurationHandle;
    protected ParameterHandle emergencyIdHandle;
    protected ParameterHandle emergencyTypeHandle;
    protected ParameterHandle emergencyDurationHandle;
    protected ParameterHandle takeOffIdHandle;
    protected ParameterHandle takeOffDelayHandle;
    protected ParameterHandle forwardIdHandle;

    protected ControlTower controlTower;

    //----------------------------------------------------------
    //                      CONSTRUCTORS
    //----------------------------------------------------------

    //----------------------------------------------------------
    //                    INSTANCE METHODS
    //----------------------------------------------------------
    /**
     * This is just a helper method to make sure all logging it output in the same form
     */
    private void log( String message )
    {
        System.out.println( "ControlTowerFederate   : " + message );
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
        fedamb = new ControlTowerFederateAmbassador( this );
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
        rtiamb.joinFederationExecution( federateName,            // name for the federate
                "airport",   // federate type
                "AirportFederation"     // name of federation
        );           // modules we want to add

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
        objectStatsPackage= rtiamb.registerObjectInstance( statsPackageHandle );
        log( "Registered StatsPackage, handle: " + objectStatsPackage );

        /////////////////////////////////////
        // 10. do the main simulation loop //
        /////////////////////////////////////
        // here is where we do the meat of our work. in each iteration, we will
        // update the attribute values of the object we registered, and will
        // send an interaction.
        controlTower = new ControlTower();
        while( fedamb.isRunning )
        {
           /*Aktualizacja paczki statystyk*/
            updateStatsPackage();

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


    private void updateStatsPackage() throws Exception
    {
        /*Update StatsPackage parameters to current values*/
        AttributeHandleValueMap attributes = rtiamb.getAttributeHandleValueMapFactory().create(4);

        HLAfloat32BE maxDelayValue = encoderFactory.createHLAfloat32BE(controlTower.maxDelay);
        attributes.put( maxDelayHandle, maxDelayValue.toByteArray() );

        HLAinteger32BE forwardedPlanesValue = encoderFactory.createHLAinteger32BE(controlTower.forwardedPlanes);
        attributes.put( forwardedPlanesHandle, forwardedPlanesValue.toByteArray() );

        HLAinteger32BE landingCountValue = encoderFactory.createHLAinteger32BE(controlTower.landingCount);
        attributes.put(landingCountHandle, landingCountValue.toByteArray() );

        HLAinteger32BE emergencyCountValue = encoderFactory.createHLAinteger32BE(controlTower.emergencyCount);
        attributes.put(emergencyCountHandle, emergencyCountValue.toByteArray() );

        rtiamb.updateAttributeValues( objectStatsPackage, attributes, generateTag(), timeFactory.makeTime( fedamb.federateTime+fedamb.federateLookahead));
    }



    /**
     * This method will inform the RTI about the types of data that the federate will
     * be creating, and the types of data we are interested in hearing about as other
     * federates produce it.
     */
    private void publishAndSubscribe() throws RTIexception
    {
        /*Publish StatsPackage object*/
        this.statsPackageHandle = rtiamb.getObjectClassHandle( "HLAobjectRoot.StatsPackage" );
        this.maxDelayHandle = rtiamb.getAttributeHandle( statsPackageHandle, "maxDelay" );
        this.forwardedPlanesHandle = rtiamb.getAttributeHandle( statsPackageHandle, "forwardedPlanes" );
        this.landingCountHandle = rtiamb.getAttributeHandle( statsPackageHandle, "landingCount" );
        this.emergencyCountHandle = rtiamb.getAttributeHandle( statsPackageHandle, "emergencyCount" );

        /*Package the information into a handle set*/
        AttributeHandleSet attributes = rtiamb.getAttributeHandleSetFactory().create();
        attributes.add(maxDelayHandle);
        attributes.add(forwardedPlanesHandle);
        attributes.add(landingCountHandle);
        attributes.add(emergencyCountHandle);
        rtiamb.publishObjectClassAttributes( statsPackageHandle, attributes );

        /*Subscribe Forward Interaction*/
        String forward = "HLAinteractionRoot.PlanesManagment.Forward";
        forwardHandle = rtiamb.getInteractionClassHandle( forward );
        forwardIdHandle = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.Forward"), "id");
        rtiamb.subscribeInteractionClass(forwardHandle);


        /*Subscribe TakeOff Interaction*/
        String takeOff = "HLAinteractionRoot.PlanesManagment.TakeOff";
        takeOffHandle = rtiamb.getInteractionClassHandle( takeOff );
        takeOffIdHandle = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.TakeOff"), "id");
        takeOffDelayHandle = rtiamb.getParameterHandle(rtiamb.getInteractionClassHandle( "HLAinteractionRoot.PlanesManagment.TakeOff"), "delay");
        rtiamb.subscribeInteractionClass(takeOffHandle);

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
        String federateName = "ControlTower";
        if( args.length != 0 )
        {
            federateName = args[0];
        }

        try
        {
            // run the example federate
            new ControlTowerFederate().runFederate( federateName );
        }
        catch( Exception rtie )
        {
            // an exception occurred, just log the information and exit
            rtie.printStackTrace();
        }
    }
}
