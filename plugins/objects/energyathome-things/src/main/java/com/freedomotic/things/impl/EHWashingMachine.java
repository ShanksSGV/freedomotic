/**
 *
 * Copyright (c) 2014 Telecom Italia
 *
 * @author: Ing. Danny Noferi, SI.IIR.OIR, Joint Open Lab S-Cube
 *
 */
package com.freedomotic.things.impl;

import com.freedomotic.behaviors.RangedIntBehaviorLogic;
import com.freedomotic.model.ds.Config;
import com.freedomotic.model.object.BooleanBehavior;
import com.freedomotic.model.object.RangedIntBehavior;
import com.freedomotic.reactions.Command;
import static com.freedomotic.things.impl.ElectricDevice.BEHAVIOR_POWERED;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 * @author Danny
 */
public class EHWashingMachine extends ElectricDevice {

    //partial implementation of i18n... waiting for future improvements
    //at the moment this is embedded... Can we take it from the manifest? or from the JFrontend plugin?
    Locale it = new Locale("it", "IT");
    ResourceBundle messages = ResourceBundle.getBundle("data/i18n/energyathomethings", it);

    private RangedIntBehaviorLogic delay;
    protected final static String BEHAVIOR_DELAY = "delay";

    @Override
    public void init() {
        delay = new RangedIntBehaviorLogic((RangedIntBehavior) getPojo().getBehavior(BEHAVIOR_DELAY));
        delay.addListener(new RangedIntBehaviorLogic.Listener() {

            @Override
            public void onLowerBoundValue(Config params, boolean fireCommand) {
                boolean executed = executeCommand("set delay", params);
                if (executed) {
                    setDelay(delay.getMin(), params, fireCommand);
                }
            }

            @Override
            public void onUpperBoundValue(Config params, boolean fireCommand) {
                boolean executed = executeCommand("set delay", params);
                if (executed) {
                    setDelay(delay.getMax(), params, fireCommand);
                }
            }

            @Override
            public void onRangeValue(int rangeValue, Config params, boolean fireCommand) {
                boolean executed = executeCommand("set delay", params);
                if (executed) {
                    setDelay(rangeValue, params, fireCommand);
                }
            }
        }
        );

        registerBehavior(delay);

        super.init();
    }

    public void setDelay(int value, Config params, boolean fireCommand) {
        if (delay.getValue() != value) {
            delay.setValue(value);
            setChanged(true);
        }
    }

    @Override
    protected void createCommands() {
        super.createCommands();

        //i18n commands
        Command start = new Command();
        start.setName(messages.getString("start") + getPojo().getName());
        start.setReceiver("app.events.sensors.behavior.request.objects");
        start.setProperty("object", getPojo().getName());
        start.setProperty("behavior", BEHAVIOR_POWERED);
        start.setProperty("value", BooleanBehavior.VALUE_TRUE);

        Command startdelay30 = new Command();
        startdelay30.setName(messages.getString("start") + getPojo().getName() + messages.getString("delay30"));
        startdelay30.setReceiver("app.events.sensors.behavior.request.objects");
        startdelay30.setProperty("object", getPojo().getName());
        startdelay30.setProperty("behavior", BEHAVIOR_DELAY);
        startdelay30.setProperty("value", "30");

        Command startdelay60 = new Command();
        startdelay60.setName(messages.getString("start") + getPojo().getName() + messages.getString("delay60"));
        startdelay60.setReceiver("app.events.sensors.behavior.request.objects");
        startdelay60.setProperty("object", getPojo().getName());
        startdelay60.setProperty("behavior", BEHAVIOR_DELAY);
        startdelay60.setProperty("value", "60");

        Command stop = new Command();
        stop.setName(messages.getString("stop") + getPojo().getName());
        stop.setReceiver("app.events.sensors.behavior.request.objects");
        stop.setProperty("object", getPojo().getName());
        stop.setProperty("behavior", BEHAVIOR_POWERED);
        stop.setProperty("value", BooleanBehavior.VALUE_FALSE);

        commandRepository.create(start);
        commandRepository.create(startdelay30);
        commandRepository.create(startdelay60);
        commandRepository.create(stop);

    }

    @Override
    protected void createTriggers() {
        super.createTriggers();
    }

}
