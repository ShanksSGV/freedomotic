
package com.freedomotic.plugins.devices.jscube.thalmicmyo;

import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.exceptions.UnableToExecuteException;
import com.freedomotic.things.ThingRepository;
import com.freedomotic.reactions.Command;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.logging.Logger;

import com.thalmic.myo.Hub;
import com.thalmic.myo.Myo;
import com.thalmic.myo.enums.PoseType;
import java.util.logging.Level;

public class ThalmicMyo
        extends Protocol implements MyoInterface{
    
    private static final Logger LOG = Logger.getLogger(ThalmicMyo.class.getName());
    final int POLLING_WAIT;

    @Inject
    private ThingRepository thingsRepository;

    public ThalmicMyo() {
        //every plugin needs a name and a manifest XML file
        super("ThalmicMyo", "/thalmic-myo/thalmic-myo-manifest.xml");
        //read a property from the manifest file below which is in
        //FREEDOMOTIC_FOLDER/plugins/devices/com.freedomotic.hello/hello-world.xml
        POLLING_WAIT = configuration.getIntProperty("time-between-reads", 2000);
        //POLLING_WAIT is the value of the property "time-between-reads" or 2000 millisecs,
        //default value if the property does not exist in the manifest
        setPollingWait(POLLING_WAIT); //millisecs interval between hardware device status reads
    }

    @Override
    protected void onRun() {

    }

    @Override
    protected void onStart() {
        try {
            Hub hub = new Hub("it.jscube.fdplugin");

            LOG.log(Level.INFO, "Searching for a Myo...");
            Myo myo = hub.waitForMyo(10000);

            if (myo == null) {
                throw new RuntimeException("Unable to find a Myo!");
            }

            LOG.log(Level.INFO, "Connected to a Myo armband!");
            DataCollector dataCollector = new DataCollector(this);
            hub.addListener(dataCollector);

            /*while (true) {
                hub.run(1000 / 20);
                System.out.print(dataCollector);
            }*/
        } catch (Exception e) {
            System.err.println("Error: ");
            e.printStackTrace();
            System.exit(1);
        }
    }

    @Override
    protected void onStop() {
    }

    @Override
    protected void onCommand(Command c)
            throws IOException, UnableToExecuteException {
    }

    @Override
    protected boolean canExecute(Command c) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    protected void onEvent(EventTemplate event) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void onPose(PoseType type) {
        LOG.log(Level.INFO, "Myo "+type.toString());
    }
}
