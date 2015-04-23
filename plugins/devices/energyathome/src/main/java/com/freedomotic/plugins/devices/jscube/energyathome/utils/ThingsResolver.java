/**
 *
 * Copyright (c) 2014 Telecom Italia
 *
 * @author: Ing. Danny Noferi, SI.IIR.OIR, Joint Open Lab S-Cube
 *
 */
package com.freedomotic.plugins.devices.jscube.energyathome.utils;

/**
 *
 * @author Danny
 */
public class ThingsResolver {

    //Raw implementation, waiting for future improvements..
    public static String resolver(String line) {

        if ((line.contains(Value.DAL_ONOFF)) && (line.contains(Value.DAL_ENERGYMETER))) {
            return Value.FD_SMARTPLUG;
        }
        if ((line.contains(Value.DAL_COLORCONTROL)) && (line.contains(Value.DAL_LEVELCONTROL)) && (line.contains(Value.DAL_ONOFF))) {
            return Value.FD_HUELIGHT;
        }
        if ((line.contains(Value.DAL_WASHINGMACHINE)) && (line.contains(Value.DAL_ENERGYMETER))) {
            return Value.FD_WASHINGMACHINE;
        }
        if ((line.contains(Value.DAL_OVEN)) && (line.contains(Value.DAL_ENERGYMETER))) {
            return Value.FD_OVEN;
        }
        if ((line.contains(Value.DAL_DOORLOCK))) {
            return Value.FD_DOORLOCK;
        } else {
            return null;
        }
    }

}
