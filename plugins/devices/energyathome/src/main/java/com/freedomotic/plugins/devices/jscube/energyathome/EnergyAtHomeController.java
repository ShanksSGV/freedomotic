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

import com.freedomotic.plugins.devices.jscube.energyathome.utils.MessageEvent;
import com.freedomotic.plugins.devices.jscube.energyathome.utils.MessageListener;
import com.freedomotic.plugins.devices.jscube.energyathome.utils.ThingsResolver;
import com.freedomotic.plugins.devices.jscube.energyathome.utils.Value;
import com.freedomotic.things.EnvObjectLogic;
import com.freedomotic.things.ThingRepository;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.java_websocket.drafts.Draft_17;
import org.java_websocket.exceptions.WebsocketNotConnectedException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class EnergyAtHomeController implements MessageListener {

    EnergyAtHomeWebSocket wseah;
    JSONParser parser = new JSONParser();

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
        String line = getToFlex(flexIP + Value.API_DEVICES);
        try {
            Object obj = parser.parse(line);
            JSONArray json = (JSONArray) obj;
            Iterator<JSONObject> it = json.iterator();
            while (it.hasNext()) {
                JSONObject temp = it.next();
                String address = (String) temp.get("dal.device.UID");
                String name = ((String) temp.get("dal.device.UID")).split(":")[1];

                String type = getType(address);

                LOG.log(Level.INFO, "...Synchronizing object {0} {1}", new Object[]{type, address});

                if (type != null) {
                    String status = "false";
                    if ((type.equalsIgnoreCase(Value.FD_SMARTPLUG)) || (type.equalsIgnoreCase(Value.FD_HUELIGHT))) {
                        status = String.valueOf(getStatus(address));
                    }
                    eah.buildEvent(name, address, Value.FD_POWER, status, type);
                }

            }
            return true;
        } catch (ParseException ex) {
            LOG.log(Level.INFO, "Error in parsing JSON! Plugin will stop!");
            ex.printStackTrace();
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
        String line = getToFlex(flexIP + Value.API_DEVICES + "/" + address
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
        String line = postToFlex(flexIP + Value.API_FUNCTIONS + URLEncoder.encode(address + ":OnOff", "UTF-8"), body);
        try {
            Object obj = parser.parse(line);
            JSONObject json = (JSONObject) obj;
            status = (Boolean) ((JSONObject) json.get("result")).get("value");
        } catch (ParseException e) {
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
            URI uri = new URI(urlToInvoke);
            url = uri.toURL();
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

        } catch (URISyntaxException e) {
            LOG.log(Level.INFO,
                    "Bad url syntax:" + urlToInvoke);
            e.printStackTrace();
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
            Object obj = parser.parse(me.getBody());
            JSONObject json = (JSONObject) obj;
            JSONObject properties = (JSONObject) json.get("properties");
            String temp = (String) properties.get("dal.function.UID");
            int i = temp.lastIndexOf(":");
            address = temp.substring(0, i);
            dalFunctionId = temp.substring(i + 1);
            String property = (String) properties.get("dal.function.property.name");
            if (dalFunctionId.equalsIgnoreCase(Value.DAL_ONOFF)) {
                value = ((JSONObject) properties.get("dal.function.property.value")).get("value").toString();
                eah.buildEvent(null, address, Value.FD_POWER, value, null);
            }
            if (dalFunctionId.equalsIgnoreCase(Value.DAL_ENERGYMETER)) {
                if (property.equalsIgnoreCase("current")) {
                    value = ((JSONObject) properties.get("dal.function.property.value")).get("level").toString();
                    value = String.valueOf(Double.valueOf(value) * 10); //only way to have the decimal 

                    eah.buildEvent(null, address, Value.FD_POWERCONSUMPTION, value, null);
                }
            }
            if (dalFunctionId.equalsIgnoreCase(Value.DAL_WASHINGMACHINE)) {
                if (property.equalsIgnoreCase("ApplianceStatus")) {
                    value = ((JSONObject) properties.get("dal.function.property.value")).get("level").toString();
                    if (value.equals("5")) {
                        eah.buildEvent(null, address, Value.FD_POWER, "true", null);
                    } else {
                        eah.buildEvent(null, address, Value.FD_POWER, "false", null);
                    }
                }
            }
            if (dalFunctionId.equalsIgnoreCase(Value.DAL_OVEN)) {
                if (property.equalsIgnoreCase("ApplianceStatus")) {
                    value = ((JSONObject) properties.get("dal.function.property.value")).get("level").toString();
                    if (value.equals("5")) {
                        eah.buildEvent(null, address, Value.FD_POWER, "true", null);
                    } else {
                        eah.buildEvent(null, address, Value.FD_POWER, "false", null);
                    }
                }
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    /**
     * This method keep synchronized objects that don't send WS messages on
     * behavior change.
     *
     * @param tr
     * @param protocolName
     */
    protected void updateObjects(ThingRepository tr, String protocolName) {
        List<EnvObjectLogic> eahObjects = tr.findByProtocol(protocolName);
        Iterator i = eahObjects.iterator();
        while (i.hasNext()) {
            EnvObjectLogic eahObject = (EnvObjectLogic) i.next();
            String address = eahObject.getPojo().getPhisicalAddress();
            String type = eahObject.getPojo().getSimpleType();
            try {
                if (type.equalsIgnoreCase(Value.FD_DOORLOCK)) {
                    String url = (flexIP + Value.API_FUNCTIONS + URLEncoder.encode(address + ":DoorLock", "UTF-8"));
                    JSONObject json = new JSONObject();
                    json.put("operation", "getStatus");
                    String body = json.toJSONString();
                    Object object = parser.parse(postToFlex(url, body));
                    JSONObject response = (JSONObject) object;
                    JSONObject result = (JSONObject) response.get("result");
                    String status = (String) result.get("status");
                    if (status.equals("OPEN")) {
                        eah.buildEvent(null, address, Value.FD_DOORSTATUS, "true", null);
                    } else {
                        eah.buildEvent(null, address, Value.FD_DOORSTATUS, "false", null);
                    }
                }
            } catch (IOException ex) {
                Logger.getLogger(EnergyAtHomeController.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ParseException ex) {
                Logger.getLogger(EnergyAtHomeController.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    protected void log(String name, String value) {
        try {
            String path = System.getProperty("user.dir") + "/plugins/devices/energyathome/log/";
            String data = new SimpleDateFormat("yyyy_MM_dd_").format(Calendar.getInstance().getTime());
            File file = new File(path + data + name + ".txt");
            // if file doesn't exists, then create it
            if (!file.exists()) {
                file.createNewFile();
            }
            String timeStamp = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime());
            String temp = timeStamp + "," + name + "," + value + "\n";

            try (PrintWriter out = new PrintWriter(new FileWriter(file, true))) {
                BufferedReader in = new BufferedReader(new FileReader(file));
                String str = in.readLine();
                while (str != null) {
                    str = in.readLine();
                }
                in.close();
                out.println(str + temp);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
