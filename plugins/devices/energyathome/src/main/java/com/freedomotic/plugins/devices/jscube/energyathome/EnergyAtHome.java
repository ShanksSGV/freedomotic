/**
 *
 * Copyright (c) 2009-2014 Freedomotic team, 2014 Telecom Italia
 *
 * @author: Ing. Danny Noferi, SI.IIR.OIR, Joint Open Lab S-Cube
 *
 * This file is part of Freedomotic http://www.freedomotic.com
 *
 * This Program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2, or (at your option) any later version.
 *
 * This Program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Freedomotic; see the file COPYING. If not, see
 * <http://www.gnu.org/licenses/>.
 */
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
import com.freedomotic.exceptions.PluginStartupException;
import com.freedomotic.reactions.Command;
import com.freedomotic.things.EnvObjectLogic;
import com.freedomotic.things.ThingRepository;

import com.freedomotic.plugins.devices.jscube.energyathome.enums.*;

import com.google.inject.Inject;

public class EnergyAtHome extends Protocol {

    @Inject
    private ThingRepository thingsRepository;

    protected static URL url;
    protected static HttpURLConnection connection;

    private final String flexIP = configuration.getProperty("flexIP");
    private final String protocolName = configuration.getProperty("protocol.name");
    private final int POLLING_TIME = configuration.getIntProperty("pollingtime", 5000);

    // Stores the count of subsequent API connection failures
    private int connectionFailuresCount = 0;
    // Max allowed API connection attempts before making the plugin as FAILED
    private static final int MAX_CONNECTON_FAILURES = 5;

    private static final Logger LOG = Logger.getLogger(EnergyAtHome.class
            .getName());

    public EnergyAtHome() {
        super("Energy@Home",
                "/energyathome/energyathome-manifest.xml");
        setPollingWait(POLLING_TIME);
    }

    @Override
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
            try {
                postToFlex(flexIP + "api/functions/" + c.getProperty("identifier")
                        + ":OnOff", body);
            } catch (IOException ex) {
                manageConnectionFailure();
            }
        }
    }

    @Override
    protected void onEvent(EventTemplate e) {

    }

    @Override
    protected void onRun() {
        for (EnvObjectLogic thing : thingsRepository.findByProtocol(protocolName)) {
            String address = thing.getPojo().getPhisicalAddress();
            String name = thing.getPojo().getName();
            try {
                String type = getType(address);
                if (type.equalsIgnoreCase("OnOff")) {
                    //object is a SmartPlug
                    String status = String.valueOf(getStatus(address));
                    
                    /**
                     * this part can be improved using webSubscription (to update
                     * POWERED status of the PLUG) instead of this polling...
                    */
                   
                    buildEvent(name, address, Behaviors.POWERED, status, "SmartPlug");
                    if (status.equalsIgnoreCase("true")){
                        //if SmartPlug is on
                        String body = "{\"operation\":\"getCurrent\"}";
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
                    }
                }
                //else if() for other object types...
            } catch (IOException ex) {
                manageConnectionFailure();
            }
            
        }
    }

    /**
     * Counts API connection failures and marks the plugin as FAILED if the max
     * number of subsequent failed connection is reached.
     */
    private void manageConnectionFailure() {
        connectionFailuresCount++;
        if (connectionFailuresCount <= MAX_CONNECTON_FAILURES) {
            setDescription("API connection failed " + connectionFailuresCount + " times");
        } else {
            this.notifyCriticalError("Too many API connection failures (" + connectionFailuresCount + " failures)");
        }
    }

    /**
     * getDevices() gathers devices linked to flexGW and create\synchronize them
     * on Freedomotic;
     */
    protected boolean getDevices() throws IOException {
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
                    buildEvent(name, address, Behaviors.POWERED, status, "SmartPlug");

                }
            }
            return true;
        } catch (JSONException e) {
            LOG.log(Level.INFO, "Error in parsing JSON! Plugin will stop!");
            e.printStackTrace();
            return false;
        }

    }

    /**
     * getType(String address) gives the class of a device, in order to create
     * it on Freedomotic
     */
    protected String getType(String address) throws IOException {
        String line = getToFlex(flexIP + "api/devices/" + address
                + "/functions");
        try {
            JSONArray json = new JSONArray(line);
            String[] temp = json.getJSONObject(0).getString("dal.function.UID")
                    .split(":"); 
        //it takes the FIRST dal.function.UID. NB TBD a better method...
            String type = temp[temp.length - 1];
            return type;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * buildEvent(String name, String address, String property, String value,
     * String type) generates an event for create/update the object
     */
    protected void buildEvent(String name, String address, Behaviors property,
            String value, String type) {
        ProtocolRead event = new ProtocolRead(this, protocolName, address);
        event.addProperty("object.name", name);
        event.addProperty("object.protocol", protocolName);
        event.addProperty("object.address", address);
        event.addProperty("behavior.name", property.toString().toLowerCase());
        event.addProperty("behaviorValue", value);
        event.addProperty("object.class", type);
        LOG.log(Level.INFO, event.getPayload().toString());
        notifyEvent(event);

    }

    /**
     * getStatus(String address) gives device status with OnOff function.UID
     */
    protected boolean getStatus(String address) throws IOException {
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

    /**
     * GET to the flexGW
     */
    private String getToFlex(String urlToInvoke) throws IOException {
        String line = null;
        try {
            url = new URL(urlToInvoke.replaceAll(" ", "%20"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            BufferedReader read = new BufferedReader(new InputStreamReader(
                    connection.getInputStream()));
            line = read.readLine();
            // Succesfull connection, reset connection failures counter
            connectionFailuresCount = 0;
        } catch (MalformedURLException e) {
            LOG.log(Level.INFO,
                    "Malformed URL! Please check IP address in manifest.xml!");
        }
        return line;
    }

    /**
     * POST to the flexGW
     */
    private String postToFlex(String urlToInvoke, String body) throws IOException {
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

            // Succesfull connection, reset connection failures counter
            connectionFailuresCount = 0;
            return line;
        } catch (MalformedURLException e) {
            LOG.log(Level.INFO,
                    "Malformed URL! Please check IP address in manifest.xml!");

        }
        return null;
    }

}
