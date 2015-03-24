/**
 *
 * Copyright (c) 2014 Telecom Italia
 *
 * @author: Ing. Danny Noferi, SI.IIR.OIR, Joint Open Lab S-Cube
 *
 */
package com.freedomotic.plugins.devices.jscube.energyathome.utils;

import java.util.EventListener;

/**
 *
 * @author Danny
 */
public interface MessageListener extends EventListener {
    public void onMessageReceived(MessageEvent me);
    
}
