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

import com.freedomotic.plugins.devices.jscube.energyathome.enums.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.freedomotic.api.EventTemplate;
import com.freedomotic.api.Protocol;
import com.freedomotic.events.ProtocolRead;
import com.freedomotic.exceptions.PluginStartupException;
import com.freedomotic.reactions.Command;
import com.freedomotic.things.ThingRepository;

import com.google.inject.Inject;


public class EnergyAtHome extends Protocol {

    @Inject
    private ThingRepository thingsRepository;

    protected static URL url;
    protected static HttpURLConnection connection;

    private final String flexIP = configuration.getProperty("flexIP");
    private final String flexWS = configuration.getProperty("flexWS");
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
            case "0": {
                if (c.getProperty("state").equals("ON")) {
                    body = "{\"operation\":\"setTrue\"}";
                } else {
                    body = "{\"operation\":\"setFalse\"}";
                }
                try {
                    eahc.postToFlex(flexIP + "api/functions/" + c.getProperty("identifier")
                            + ":OnOff", body);
                } catch (IOException ex) {
                    eahc.manageConnectionFailure();
                }
                break;
            }
            case "1": {
                String hue = null, sat = null;
                if (c.getName().contains("hue")) {
                    hue = c.getProperty("value");
                    sat = thingsRepository.findByAddress(protocolName, c.getProperty("identifier")).get(0).getBehavior(Behaviors.saturation.toString()).getValueAsString();
                } else if (c.getName().contains("saturation")) {
                    hue = thingsRepository.findByAddress(protocolName, c.getProperty("identifier")).get(0).getBehavior(Behaviors.hue.toString()).getValueAsString();
                    sat = c.getProperty("value");
                } else if (c.getName().contains("brightness")) {
                    LOG.log(Level.INFO, "Command not supported!");
                }
                body = "{\"operation\":\"setHS\",\"arguments\":[{\"type\":\"java.lang.Short\",\"value\":\""
                        + hue + "\"},{\"type\":\"java.lang.Short\",\"value\":\"" + sat + "\"}]}";
                LOG.log(Level.INFO, body);
                try {
                    eahc.postToFlex(flexIP + "api/functions/" + c.getProperty("identifier")
                            + ":ColorControl", body);
                } catch (IOException ex) {
                    eahc.manageConnectionFailure();
                }
            }
        }
    }

    @Override
    protected void onEvent(EventTemplate e) {
    }

    @Override
    protected void onRun() {
        eahc.closeWebSocket(flexWS);
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
}
