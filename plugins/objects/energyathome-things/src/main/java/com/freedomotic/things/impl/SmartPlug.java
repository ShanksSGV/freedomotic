/**
 *
 * Copyright (c) 2014 Telecom Italia
 *
 * @author: Ing. Danny Noferi, SI.IIR.OIR, Joint Open Lab S-Cube
 *
 */
package com.freedomotic.things.impl;

import com.freedomotic.model.object.BooleanBehavior;
import com.freedomotic.reactions.Command;
import static com.freedomotic.things.impl.ElectricDevice.BEHAVIOR_POWERED;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * @author Danny
 */
public class SmartPlug extends ElectricDevice {

    //partial implementation of i18n... waiting for future improvements
    //at the moment this is embedded... Can we take it from the manifest? or from the JFrontend plugin?
    Locale it = new Locale("it", "IT");
    ResourceBundle messages = ResourceBundle.getBundle("data/i18n/energyathomethings", it);

    @Override
    public void init() {
        super.init();
    }

    @Override
    protected void createCommands() {
        super.createCommands();

        //i18n commands
        Command turnon = new Command();
        turnon.setName(messages.getString("turnon") + getPojo().getName());
        turnon.setReceiver("app.events.sensors.behavior.request.objects");
        turnon.setProperty("object", getPojo().getName());
        turnon.setProperty("behavior", BEHAVIOR_POWERED);
        turnon.setProperty("value", BooleanBehavior.VALUE_TRUE);

        commandRepository.create(turnon);

        Command turnoff = new Command();
        turnoff.setName(messages.getString("turnoff") + getPojo().getName());
        turnoff.setReceiver("app.events.sensors.behavior.request.objects");
        turnoff.setProperty("object", getPojo().getName());
        turnoff.setProperty("behavior", BEHAVIOR_POWERED);
        turnoff.setProperty("value", BooleanBehavior.VALUE_FALSE);

        commandRepository.create(turnoff);
    }

    @Override
    protected void createTriggers() {
        super.createTriggers();
    }
}
