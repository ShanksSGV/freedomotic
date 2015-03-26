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

import com.freedomotic.plugins.devices.jscube.energyathome.enums.Behaviors;
import com.freedomotic.plugins.devices.jscube.energyathome.utils.MessageEvent;
import com.freedomotic.plugins.devices.jscube.energyathome.utils.MessageListener;
import com.freedomotic.plugins.devices.jscube.energyathome.utils.ThingsResolver;
import com.freedomotic.plugins.devices.jscube.energyathome.utils.Value;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.java_websocket.drafts.Draft_17;
import org.java_websocket.exceptions.WebsocketNotConnectedException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class EnergyAtHomeController implements MessageListener {

    EnergyAtHomeWebSocket wseah;

    protected static URL url;
    protected static HttpURLConnection connection;
    private final EnergyAtHome eah;
    private final String flexIP;
    // Stores the count of subsequent API connection failures
    private int connectionFailuresCount = 0;
    // Max allowed API connection attempts before making the plugin as FAILED
    private static final int MAX_CONNECTON_FAILURES = 5;
    private static final Logger LOG = Logger.getLogger(EnergyAtHome.class.getName());

    public EnergyAtHomeController(String flexIP, EnergyAtHome eah) {
        //Important: reset connection failure counter
        connectionFailuresCount = 0;
        this.flexIP = flexIP;
        this.eah = eah;
    }

    /**
     * getDevices() gathers devices linked to flexGW and create\synchronize them
     * on Freedomotic;
     *
     * @return
     * @throws java.io.IOException
     */
    protected boolean getDevices() throws IOException {
        String line = getToFlex(flexIP + "api/devices");
        try {
            JSONArray json = new JSONArray(line);
            for (int i = 0; i < json.length(); i++) {
                String address = json.getJSONObject(i).getString(
                        "dal.device.UID");

                String name = json.getJSONObject(i).getString(
                        "dal.device.UID").split(":")[1];

                String type = getType(address);

                LOG.log(Level.INFO, "...Synchronizing object {0} {1}", new Object[]{type, address});

                if (type != null) {
                    String status = "false";
                    if ((type.equalsIgnoreCase(Value.FD_SMARTPLUG))||(type.equalsIgnoreCase(Value.FD_HUELIGHT))) {
                        status = String.valueOf(getStatus(address));
                    }
                    eah.buildEvent(name, address, Behaviors.powered, status, type);
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
     *
     * @param address
     * @return
     * @throws java.io.IOException
     */
    protected String getType(String address) throws IOException {
        String line = getToFlex(flexIP + "api/devices/" + address
                + "/functions");

        String type = ThingsResolver.resolver(line);
        return type;
    }

    /**
     * getStatus(String address) gives device status with OnOff function.UID
     *
     * @param address
     * @return
     * @throws java.io.IOException
     */
    protected boolean getStatus(String address) throws IOException {
        boolean status = false;
        String body = "{\"operation\":\"getData\"}";
        String line = postToFlex(flexIP + "api/functions/" + address + ":OnOff", body);
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
     *
     * @param urlToInvoke
     * @return
     * @throws java.io.IOException
     */
    protected String getToFlex(String urlToInvoke) throws IOException {
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
     *
     * @param urlToInvoke
     * @param body
     * @return
     * @throws java.io.IOException
     */
    protected String postToFlex(String urlToInvoke, String body) throws IOException {
        try {
            url = new URL(urlToInvoke.replaceAll(" ", "%20"));
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setUseCaches(false);

            try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                wr.writeBytes(body);
                wr.flush();
            }

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

    /**
     * Counts API connection failures and marks the plugin as FAILED if the max
     * number of subsequent failed connection is reached.
     */
    protected void manageConnectionFailure() {
        connectionFailuresCount++;
        if (connectionFailuresCount <= MAX_CONNECTON_FAILURES) {
            LOG.log(Level.INFO, "API connection failed {0} times", connectionFailuresCount);
        } else {
            LOG.log(Level.INFO, "Too many API connection failures ({0} failures)", connectionFailuresCount);
        }
    }

    protected boolean openWebSocket(String flexWS) {
        try {
            URI uriWS = new URI(flexWS);
            wseah = new EnergyAtHomeWebSocket(uriWS, new Draft_17(), this);
            wseah.connect();
        } catch (WebsocketNotConnectedException | URISyntaxException wne) {
            wne.printStackTrace();
            return false;
        }
        return true;
    }

    protected void updateWebSocket(String flexWS) {
        wseah.close();
        openWebSocket(flexWS);
    }

    /**
     * Manages the socket messages
     *
     * @param me
     */
    @Override
    public void onMessageReceived(MessageEvent me) {
        String address;
        String value;
        String dalFunctionId;
        try {
            JSONObject json = new JSONObject(me.getBody());
            JSONObject properties = json.getJSONObject("properties");
            String temp = properties.getString("dal.function.UID");
            int i = temp.lastIndexOf(":");
            address = temp.substring(0, i);
            dalFunctionId = temp.substring(i + 1);
            String property = properties.getString("dal.function.property.name");
            if (dalFunctionId.equalsIgnoreCase(Value.DAL_ONOFF)) {
                value = properties.getJSONObject("dal.function.property.value").getString("value");
                eah.buildEvent(null, address, Behaviors.powered, value, null);
            }
            if (dalFunctionId.equalsIgnoreCase(Value.DAL_ENERGYMETER)) {
                if (property.equalsIgnoreCase("current")) {
                    value = properties.getJSONObject("dal.function.property.value").getString("level");
                    value = String.valueOf(Double.valueOf(value) * 10);
                    eah.buildEvent(null, address, Behaviors.power_consumption, value, null);
                }
            }

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

}
