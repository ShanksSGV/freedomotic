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
import java.net.URI;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.drafts.Draft;
import org.java_websocket.handshake.ServerHandshake;

public class EnergyAtHomeWebSocket extends WebSocketClient {

    MessageListener listener;

    public EnergyAtHomeWebSocket(URI serverUri, Draft draft, MessageListener listener) {
        super(serverUri, draft);
        this.listener = listener;
    }

    @Override
    public void onOpen(ServerHandshake sh) {
        System.out.println("WebSocket Connected!!");
        this.send("{\"dal.function.UID\":\"*\",\"dal.function.property.name\":\"*\"}");
    }

    @Override
    public void onMessage(String message) {
        System.out.println("\n\nreceived: " + message + "\n\n");
        MessageEvent me = new MessageEvent(this, message);
        listener.onMessageReceived(me);
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
