/**
 *
 * Copyright (c) 2014 Telecom Italia
 *
 * @author: Ing. Danny Noferi, SI.IIR.OIR, Joint Open Lab S-Cube
 *
 */
package com.freedomotic.plugins.devices.jscube.energyathome.utils;

import java.util.EventObject;

/**
 *
 * @author Danny
 */
public class MessageEvent extends EventObject {

    private final String body;

    public MessageEvent(Object source, String body) {
        super(source);
        this.body = body;
    }

    public String getBody() {
        return body;
    }

}
