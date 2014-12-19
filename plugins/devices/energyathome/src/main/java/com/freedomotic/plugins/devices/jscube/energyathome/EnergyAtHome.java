package com.freedomotic.plugins.devices.jscube.energyathome;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.events.ProtocolRead;
<<<<<<< HEAD
import com.freedomotic.reactions.Command;
import com.freedomotic.things.EnvObjectLogic;
import com.freedomotic.things.ThingRepository;

import com.freedomotic.plugins.devices.jscube.energyathome.enums.*;

=======
import com.freedomotic.exceptions.PluginStartupException;
import com.freedomotic.reactions.Command;
import com.freedomotic.things.EnvObjectLogic;
import com.freedomotic.things.ThingRepository;
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
import com.google.inject.Inject;

public class EnergyAtHome extends Protocol {

    @Inject
    private ThingRepository thingsRepository;

    protected static URL url;
    protected static HttpURLConnection connection;

    protected String flexIP = configuration.getProperty("flexIP");
    private final String protocolName = configuration.getProperty("protocol.name");
<<<<<<< HEAD
    
    private final int POLLING_TIME = configuration
            .getIntProperty("pollingtime", 1000);
=======
    private final int POLLING_TIME = configuration
            .getIntProperty("pollingtime", 1000);
    // Stores the count of subsequent API connection failures
    private int connectionFailuresCount = 0;
    // Max allowed API connection attempts before making the plugin as FAILED
    private static final int MAX_CONNECTON_FAILURES = 5;
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2

    private static final Logger LOG = Logger.getLogger(EnergyAtHome.class
            .getName());

    public EnergyAtHome() {
        super("Energy@Home",
                "/energyathome/energyathome-manifest.xml");
        setPollingWait(POLLING_TIME);
    }

    @Override
<<<<<<< HEAD
    protected void onStart() {
        if (!getDevices()) { // exit if no devices were found
            setPollingWait(-1);
            super.stop();
=======
    protected void onStart() throws PluginStartupException {
        //Important: reset connection failure counter
        connectionFailuresCount = 0;
        try {
            if (!getDevices()) { // exit if no devices were found
                setPollingWait(-1);
                super.stop();
            }
        } catch (IOException ex) {
            // Stop the plugin and notify connection problems
            throw new PluginStartupException("Cannot connect to API at " + flexIP, ex);
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
        }
    }

    @Override
    protected boolean canExecute(Command arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void onCommand(Command c) {
        if (c.getProperty("code").equals("0")) {
            String body;
            if (c.getProperty("state").equals("ON")) {
                body = "{\"operation\":\"setTrue\"}";
            } else {
                body = "{\"operation\":\"setFalse\"}";
            }
<<<<<<< HEAD
            postToFlex(flexIP + "api/functions/" + c.getProperty("identifier")
                    + ":OnOff", body);
=======
            try {
                postToFlex(flexIP + "api/functions/" + c.getProperty("identifier")
                        + ":OnOff", body);
            } catch (IOException ex) {
                manageConnectionFailure();
            }
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
        }
    }

    @Override
    protected void onEvent(EventTemplate e) {

    }

    @Override
    protected void onRun() {
        for (EnvObjectLogic thing : thingsRepository.findAll()) {
            if (thing.getPojo().getProtocol().equals(protocolName)) {
                String address = thing.getPojo().getPhisicalAddress();
                String name = thing.getPojo().getName();
                String body = "{\"operation\":\"getCurrent\"}";
<<<<<<< HEAD
                String line = postToFlex(
                        flexIP + "api/functions/" + address + ":EnergyMeter", body);
                try {
                    JSONObject json = new JSONObject(line);
                    Double value = json.getJSONObject("result").getDouble("level");
                    LOG.log(Level.INFO, "Object {0}is consuming: {1}W", new Object[]{address, value});
                    buildEvent(name, address, Behaviors.POWER_CONSUMPTION, String.valueOf(value), null);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
=======
                String line;
                try {
                    line = postToFlex(
                            flexIP + "api/functions/" + address + ":EnergyMeter", body);
                    try {
                        JSONObject json = new JSONObject(line);
                        Double value = json.getJSONObject("result").getDouble("level");
                        LOG.log(Level.INFO, "Object {0}is consuming: {1}W", new Object[]{address, value});
                        buildEvent(name, address, "powerUsage", String.valueOf(value), null);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                } catch (IOException ex) {
                    manageConnectionFailure();
                }

>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
            }
        }
    }

<<<<<<< HEAD
=======
    /**
     * Counts API connection failures and marks the plugin as FAILED if the
     * max number of subsequent failed connection is reached.
     */
    private void manageConnectionFailure() {
        connectionFailuresCount++;
        if (connectionFailuresCount <= MAX_CONNECTON_FAILURES) {
            setDescription("API connection failed " + connectionFailuresCount + " times");
        } else {
            this.notifyCriticalError("Too many API connection failures (" + connectionFailuresCount + " failures)");
        }
    }

>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
    /*
     * getDevices() gathers devices linked to flexGW and create\synchronize 
     * them on Freedomotic;  
     */
<<<<<<< HEAD
    protected boolean getDevices() {
=======
    protected boolean getDevices() throws IOException {
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
        String line = getToFlex(flexIP + "api/devices");
        try {
            JSONArray json = new JSONArray(line);
            for (int i = 0; i < json.length(); i++) {
                String address = json.getJSONObject(i).getString(
                        "dal.device.UID");
                String name = json.getJSONObject(i).getString("component.name");
                String type = getType(address);
                LOG.log(Level.INFO, "...Synchronizing object {0} {1}", new Object[]{type, address});
                if (type.equalsIgnoreCase("OnOff")) {
                    String status = String.valueOf(getStatus(address));
<<<<<<< HEAD
                    buildEvent(name, address, Behaviors.POWERED , status, "SmartPlug");
=======
                    buildEvent(name, address, "powered", status, "SmartPlug");
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
                }
            }
            return true;
        } catch (JSONException e) {
            LOG.log(Level.INFO, "Error in parsing JSON! Plugin will stop!");
            e.printStackTrace();
            return false;
        }

    }

    /*
     * getType(String address) gives the class of a device, 
     * in order to create it on Freedomotic
     */
<<<<<<< HEAD
    protected String getType(String address) {
=======
    protected String getType(String address) throws IOException {
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
        String line = getToFlex(flexIP + "api/devices/" + address
                + "/functions");
        try {
            JSONArray json = new JSONArray(line);
            String[] temp = json.getJSONObject(0).getString("dal.function.UID")
                    .split(":");
            String type = temp[temp.length - 1];
            return type;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /*
     * buildEvent(String name, String address, String property, String value,
     * String type) generates an event for create/update the object
     */
<<<<<<< HEAD
    protected void buildEvent(String name, String address, Behaviors property,
=======
    protected void buildEvent(String name, String address, String property,
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
            String value, String type) {
        ProtocolRead event = new ProtocolRead(this, protocolName, address);
        event.addProperty("object.name", name);
        event.addProperty("object.protocol", protocolName);
        event.addProperty("object.address", address);
<<<<<<< HEAD
        event.addProperty("behavior.name", property.toString());
        event.addProperty("behaviorValue", value);
        event.addProperty("object.class", type);
        LOG.log(Level.INFO, event.getPayload().toString());
        notifyEvent(event); 
=======
        event.addProperty("behavior.name", property);
        event.addProperty("behaviorValue", value);
        event.addProperty("object.class", type);
        LOG.log(Level.INFO, event.getPayload().toString());
        notifyEvent(event);
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
    }

    /*
     * getStatus(String address) gives device status with OnOff function.UID
     */
<<<<<<< HEAD
    protected boolean getStatus(String address) {
=======
    protected boolean getStatus(String address) throws IOException {
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
        boolean status = false;
        String body = "{\"operation\":\"getData\"}";
        String line = postToFlex(
                flexIP + "api/functions/" + address + ":OnOff", body);
        try {
            JSONObject json = new JSONObject(line);
            status = json.getJSONObject("result").getBoolean("value");
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return status;
    }

    /*
     * GET to the flexGW
     */
<<<<<<< HEAD
    private String getToFlex(String urlToInvoke) {
=======
    private String getToFlex(String urlToInvoke) throws IOException {
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
        String line = null;
        try {
            url = new URL(urlToInvoke.replaceAll(" ", "%20"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader read = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            line = read.readLine();
<<<<<<< HEAD
        } catch (MalformedURLException e) {
            LOG.log(Level.INFO,
                    "Malformed URL! Please check IP address in manifest.xml!");
        } catch (IOException e) {
            e.printStackTrace();
=======
            // Succesfull connection, reset connection failures counter
            connectionFailuresCount = 0;
        } catch (MalformedURLException e) {
            LOG.log(Level.INFO,
                    "Malformed URL! Please check IP address in manifest.xml!");
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
        }
        return line;
    }

    /*
     * POST to the flexGW
     */
<<<<<<< HEAD
    private String postToFlex(String urlToInvoke, String body) {
=======
    private String postToFlex(String urlToInvoke, String body) throws IOException {
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
        try {
            url = new URL(urlToInvoke.replaceAll(" ", "%20"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");

            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setUseCaches(false);

            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(body);
            wr.flush();
            wr.close();

            BufferedReader read = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            String line = read.readLine();
<<<<<<< HEAD
=======
            // Succesfull connection, reset connection failures counter
            connectionFailuresCount = 0;
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
            return line;
        } catch (MalformedURLException e) {
            LOG.log(Level.INFO,
                    "Malformed URL! Please check IP address in manifest.xml!");
<<<<<<< HEAD
        } catch (IOException e) {
            e.printStackTrace();
=======
>>>>>>> 65805c9f65cb4c2750e0631115215fe449d1efc2
        }
        return null;
    }

}
