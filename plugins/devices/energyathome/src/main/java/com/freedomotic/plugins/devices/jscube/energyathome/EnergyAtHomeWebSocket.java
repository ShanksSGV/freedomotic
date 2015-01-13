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
import java.net.URI;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

import org.json.JSONException;
import org.json.JSONObject;

public class EnergyAtHomeWebSocket extends WebSocketClient {
    
    private final EnergyAtHome eah;

    public EnergyAtHomeWebSocket(URI serverUri, Draft draft, EnergyAtHome eah) {
        super(serverUri, draft);
        this.eah = eah;
    }

    @Override
    public void onOpen(ServerHandshake sh) {
        System.out.println("WebSocket Connected!!");
        this.send("{\"dal.function.UID\":\"*\",\"dal.function.property.name\":\"*\"}");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("\n\nreceived: " + message + "\n\n");
        String address = null;
        String value = null;
        String dalFunctionId = null;
        try {
            JSONObject json = new JSONObject(message);
            JSONObject properties = json.getJSONObject("properties");
            String temp = properties.getString("dal.function.UID");
            int i = temp.lastIndexOf(":");
            address = temp.substring(0,i);
            dalFunctionId = temp.substring(i);
            value = properties.getJSONObject("dal.function.property.value").getString("value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        eah.buildEvent(null, address, Behaviors.powered, value, null);
    }

    @Override
    public void onClose(int i, String string, boolean bln) {
        System.out.println("WebSocket closed");
    }

    @Override
    public void onError(Exception excptn) {
        System.out.println("Error...!");
        excptn.printStackTrace();
    }

}
