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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.bus.BusService;
import com.freedomotic.events.MessageEvent;
import com.freedomotic.events.ProtocolRead;
import com.freedomotic.exceptions.PluginStartupException;
import com.freedomotic.plugins.devices.jscube.energyathome.utils.Value;
import com.freedomotic.reactions.Command;
import com.freedomotic.things.ThingRepository;

import com.google.inject.Inject;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class EnergyAtHome extends Protocol {

    @Inject
    private ThingRepository thingsRepository;
    private BusService busService;

    protected static URL url;
    protected static HttpURLConnection connection;

    private final String flexIP = configuration.getProperty("flexIP");
    private final String flexWS = configuration.getProperty("flexWS");
    private final boolean saveToLog = configuration.getBooleanProperty("log", false);
    private final String protocolName = configuration.getProperty("protocol.name");
    private final int POLLING_TIME = configuration.getIntProperty("pollingtime", 5000);

    private static final Logger LOG = Logger.getLogger(EnergyAtHome.class.getName());

    EnergyAtHomeController eahc;

    public EnergyAtHome() {
        super("Energy@Home",
                "/energyathome/energyathome-manifest.xml");
        setPollingWait(POLLING_TIME);
    }

    @Override
    protected void onStart() throws PluginStartupException {
        eahc = new EnergyAtHomeController(flexIP, this);
        try {
            if (!eahc.getDevices()) { // exit if no devices were found
                setPollingWait(-1);
                super.stop();
            }
        } catch (IOException ex) {
            // Stop the plugin and notify connection problems
            throw new PluginStartupException("Cannot connect to API at " + flexIP, ex);
        }

        if (!eahc.openWebSocket(flexWS)) {
            throw new PluginStartupException("Cannot register to Scubox WebSocket Endpoint");
        }
        setDescription("Energy@Home plugin connected!");
    }

    @Override
    protected boolean canExecute(Command arg0) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    protected void onCommand(Command c) {
        String body = null;
        switch (c.getProperty("code")) {
            case "0": { //OnOff commands
                if (c.getProperty("value").equals("ON")) {
                    body = "{\"operation\":\"setTrue\"}";
                } else {
                    body = "{\"operation\":\"setFalse\"}";
                }
                try {
                    eahc.postToFlex(flexIP + "api/functions/" + c.getProperty("identifier")
                            + ":" + Value.DAL_ONOFF, body);
                } catch (IOException ex) {
                    eahc.manageConnectionFailure();
                }
                break;
            }
            case "1": { //Hue commands
                String hue = null, sat = null, dalfunction = null;

                if (c.getProperty("function").equalsIgnoreCase(Value.FD_BRI)) {
                    String bri = c.getProperty("value");
                    dalfunction = Value.DAL_LEVELCONTROL;
                    body = "{\"operation\":\"setData\",\"arguments\":[{\"type\":\"java.math.BigDecimal\",\"value\":\""
                            + bri + "\"}]}";
                } else {
                    if (c.getProperty("function").equalsIgnoreCase(Value.FD_HUE)) {
                        hue = c.getProperty("value");
                        sat = "254";
                        dalfunction = Value.DAL_COLORCONTROL;
                    } else if (c.getProperty("function").equalsIgnoreCase(Value.FD_SAT)) {
                        hue = thingsRepository.findByAddress(protocolName, c.getProperty("identifier")).get(0).getBehavior(Value.FD_HUE).getValueAsString();
                        sat = c.getProperty("value");
                        dalfunction = Value.DAL_COLORCONTROL;
                    }
                    body = "{\"operation\":\"setHS\",\"arguments\":[{\"type\":\"java.lang.Short\",\"value\":\""
                            + hue + "\"},{\"type\":\"java.lang.Short\",\"value\":\"" + sat + "\"}]}";

                }
                LOG.log(Level.INFO, body);
                try {
                    eahc.postToFlex(flexIP + "api/functions/" + c.getProperty("identifier")
                            + ":" + dalfunction, body);
                } catch (IOException ex) {
                    eahc.manageConnectionFailure();
                }
            }

            case "2": { //WashingMachine commands
                if (c.getProperty("function").equalsIgnoreCase("status")) {
                    if (c.getProperty("value").equals("START")) {
                        body = "{\"operation\":\"execStartCycle\"}";
                    }
                    if (c.getProperty("value").equals("STOP")) {
                        body = "{\"operation\":\"execStopCycle\"}";
                    }
                    if (c.getProperty("value").equals("DELAY")) {
                        //valutare come passare questo ritardo
                        String delay = c.getProperty("option");
                        body = "{\"operation\":\"setStartTime\"}";
                    }
                } else if (c.getProperty("function").equalsIgnoreCase("cycle")) {
                    JSONObject json = new JSONObject();
                    JSONArray args = new JSONArray();
                    JSONObject argsObj = new JSONObject();
                    argsObj.put("type", "java.lang.Short");
                    argsObj.put("value", "3");  //da sostituire con il numero del ciclo associato...
                    args.add(argsObj);
                    json.put("arguments", args);
                    json.put("operation","setCycle");
                    body = json.toJSONString();
                    //body = "{\"operation\":\"execStartCycle\"}";
                }
                try {
                    eahc.postToFlex(flexIP + "api/functions/" + c.getProperty("identifier")
                            + ":" + Value.DAL_WASHINGMACHINE, body);
                } catch (IOException ex) {
                    eahc.manageConnectionFailure();
                }
                break;
            }

            case "3": { //Oven commands
                if (c.getProperty("function").equalsIgnoreCase("status")) {
                    if (c.getProperty("value").equals("ON")) {
                        body = "{\"operation\":\"execStartCycle\"}";
                    }
                    if (c.getProperty("value").equals("OFF")) {
                        body = "{\"operation\":\"execStopCycle\"}";
                    }
                    try {
                        eahc.postToFlex(flexIP + "api/functions/" + c.getProperty("identifier")
                                + ":" + Value.DAL_OVEN, body);
                    } catch (IOException ex) {
                        eahc.manageConnectionFailure();
                    }
                }
                break;
            }
        }
    }

    @Override
    protected void onEvent(EventTemplate e
    ) {
    }

    @Override
    protected void onRun() {
        eahc.updateWebSocket(flexWS);
    }

    /**
     * buildEvent(String name, String address, String property, String value,
     * String type) generates an event for create/update the object
     *
     * @param name
     * @param address
     * @param property
     * @param value
     * @param type
     */
    protected void buildEvent(String name, String address, String property,
            String value, String type) {
        ProtocolRead event = new ProtocolRead(this, protocolName, address);
        event.addProperty("object.name", name);
        event.addProperty("object.protocol", protocolName);
        event.addProperty("object.address", address);
        event.addProperty("behavior.name", property);
        event.addProperty("behaviorValue", value);
        event.addProperty("object.class", type);
        LOG.log(Level.INFO, event.getPayload().toString());
        notifyEvent(event);

        if (saveToLog) {
            if (property.equalsIgnoreCase(Value.FD_POWERCONSUMPTION)) {
                eahc.log(address.split(":")[1], value);
            }

        }

    }

    private void sendGUIToast(String text) {
        MessageEvent callout = new MessageEvent(this, text);
        callout.setType("callout"); //display as callout on frontends
        callout.setExpiration(15 * 1000);//message lasts 10 seconds
        busService.send(callout);
    }
}
