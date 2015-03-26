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

public class EHHueLight
        extends Light {

    //partial implementation of i18n... waiting for future improvements
    //at the moment this is embedded... Can we take it from the manifest? or from the JFrontend plugin?
    Locale it = new Locale("it", "IT");
    ResourceBundle messages = ResourceBundle.getBundle("data/i18n/energyathomethings", it);

    private RangedIntBehaviorLogic hue;
    private RangedIntBehaviorLogic saturation;

    protected final static String HUE_RED = "254";
    protected final static String HUE_BLUE = "170";
    protected final static String HUE_GREEN = "85";
    protected final static String HUE_YELLOW = "50";
    protected final static String HUE_FUCSIA = "220";

    protected final static String BRI_MIN = "1";
    protected final static String BRI_LOW = "60";
    protected final static String BRI_MED = "125";
    protected final static String BRI_HIGH = "200";
    protected final static String BRI_MAX = "254";

    protected final static String BEHAVIOR_HUE = "hue";
    protected final static String BEHAVIOR_SATURATION = "saturation";
    protected final static String BEHAVIOR_BRIGHTNESS = "brightness";

    @Override
    public void init() {

        hue = new RangedIntBehaviorLogic((RangedIntBehavior) getPojo().getBehavior(BEHAVIOR_HUE));
        hue.addListener(new RangedIntBehaviorLogic.Listener() {

            @Override
            public void onLowerBoundValue(Config params, boolean fireCommand) {
                boolean executed = executeCommand("set hue", params);
                if (executed) {
                    setHue(0);
                }
            }

            @Override
            public void onUpperBoundValue(Config params, boolean fireCommand) {
                boolean executed = executeCommand("set hue", params);
                if (executed) {
                    setHue(254);
                }
            }

            @Override
            public void onRangeValue(int rangeValue, Config params, boolean fireCommand) {
                boolean executed = executeCommand("set hue", params);
                if (executed) {
                    setHue(rangeValue);
                }
            }
        });

        registerBehavior(hue);

        saturation = new RangedIntBehaviorLogic((RangedIntBehavior) getPojo().getBehavior(BEHAVIOR_SATURATION));
        saturation.addListener(new RangedIntBehaviorLogic.Listener() {

            @Override
            public void onLowerBoundValue(Config params, boolean fireCommand) {
                boolean executed = executeCommand("set saturation", params);
                if (executed) {
                    setSaturation(0);
                }
            }

            @Override
            public void onUpperBoundValue(Config params, boolean fireCommand) {
                boolean executed = executeCommand("set saturation", params);
                if (executed) {
                    setSaturation(254);
                }
            }

            @Override
            public void onRangeValue(int rangeValue, Config params, boolean fireCommand) {
                boolean executed = executeCommand("set saturation", params);
                if (executed) {
                    setSaturation(rangeValue);
                }
            }
        });
        //register this behavior to the superclass to make it visible to it
        registerBehavior(saturation);

        super.init();

    }

    public void setHue(int rangeValue) {
        if (hue.getValue() != rangeValue) {
            hue.setValue(rangeValue);
            setChanged(true);
        }
    }

    public void setSaturation(int rangeValue) {
        if (saturation.getValue() != rangeValue) {
            saturation.setValue(rangeValue);
            setChanged(true);
        }
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

        Command turnoff = new Command();
        turnoff.setName(messages.getString("turnoff") + getPojo().getName());
        turnoff.setReceiver("app.events.sensors.behavior.request.objects");
        turnoff.setProperty("object", getPojo().getName());
        turnoff.setProperty("behavior", BEHAVIOR_POWERED);
        turnoff.setProperty("value", BooleanBehavior.VALUE_FALSE);

        Command setHueBlue = new Command();
        setHueBlue.setName(messages.getString("actionhue") + getPojo().getName() + " " + messages.getString("blue"));
        setHueBlue.setDescription("the light " + getPojo().getName() + " changes its hue to blue");
        setHueBlue.setReceiver("app.events.sensors.behavior.request.objects");
        setHueBlue.setProperty("object", getPojo().getName());
        setHueBlue.setProperty("identifier", getPojo().getPhisicalAddress());
        setHueBlue.setProperty("behavior", BEHAVIOR_HUE);
        setHueBlue.setProperty("value", HUE_BLUE);

        Command setHueRed = new Command();
        setHueRed.setName(messages.getString("actionhue") + getPojo().getName() + " " + messages.getString("red"));
        setHueRed.setDescription("the light " + getPojo().getName() + " changes its hue to red");
        setHueRed.setReceiver("app.events.sensors.behavior.request.objects");
        setHueRed.setProperty("object", getPojo().getName());
        setHueRed.setProperty("behavior", BEHAVIOR_HUE);
        setHueRed.setProperty("value", HUE_RED);

        Command setHueGreen = new Command();
        setHueGreen.setName(messages.getString("actionhue") + getPojo().getName() + " " + messages.getString("green"));
        setHueGreen.setDescription("the light " + getPojo().getName() + " changes its hue to green");
        setHueGreen.setReceiver("app.events.sensors.behavior.request.objects");
        setHueGreen.setProperty("object", getPojo().getName());
        setHueGreen.setProperty("behavior", BEHAVIOR_HUE);
        setHueGreen.setProperty("value", HUE_GREEN);

        Command setHueWhite = new Command();
        setHueWhite.setName(messages.getString("actionhue") + getPojo().getName() + " " + messages.getString("white"));
        setHueWhite.setDescription("the light " + getPojo().getName() + " changes its hue to white");
        setHueWhite.setReceiver("app.events.sensors.behavior.request.objects");
        setHueWhite.setProperty("object", getPojo().getName());
        setHueWhite.setProperty("behavior", BEHAVIOR_SATURATION);
        setHueWhite.setProperty("value", "0");

        Command setHueYellow = new Command();
        setHueYellow.setName(messages.getString("actionhue") + getPojo().getName() + " " + messages.getString("yellow"));
        setHueYellow.setDescription("the light " + getPojo().getName() + " changes its hue to yellow");
        setHueYellow.setReceiver("app.events.sensors.behavior.request.objects");
        setHueYellow.setProperty("object", getPojo().getName());
        setHueYellow.setProperty("behavior", BEHAVIOR_HUE);
        setHueYellow.setProperty("value", HUE_YELLOW);

        Command setHueFucsia = new Command();
        setHueFucsia.setName(messages.getString("actionhue") + getPojo().getName() + " " + messages.getString("fucsia"));
        setHueFucsia.setDescription("the light " + getPojo().getName() + " changes its hue to fucsia");
        setHueFucsia.setReceiver("app.events.sensors.behavior.request.objects");
        setHueFucsia.setProperty("object", getPojo().getName());
        setHueFucsia.setProperty("behavior", BEHAVIOR_HUE);
        setHueFucsia.setProperty("value", HUE_FUCSIA);

        Command setMaxSaturation = new Command();
        setMaxSaturation.setName(messages.getString("actionsaturation") + getPojo().getName() + " " + messages.getString("maxvalue"));
        setMaxSaturation.setDescription("the light " + getPojo().getName() + " changes its saturation to max value");
        setMaxSaturation.setReceiver("app.events.sensors.behavior.request.objects");
        setMaxSaturation.setProperty("object", getPojo().getName());
        setMaxSaturation.setProperty("behavior", BEHAVIOR_SATURATION);
        setMaxSaturation.setProperty("value", "254");

        Command setMinBrightness = new Command();
        setMinBrightness.setName(messages.getString("actionbrightness") + getPojo().getName() + " " + messages.getString("minvalue"));
        setMinBrightness.setDescription("the light " + getPojo().getName() + " changes its brightness to min value");
        setMinBrightness.setReceiver("app.events.sensors.behavior.request.objects");
        setMinBrightness.setProperty("object", getPojo().getName());
        setMinBrightness.setProperty("behavior", BEHAVIOR_BRIGHTNESS);
        setMinBrightness.setProperty("value", BRI_MIN);
        
        Command setLowBrightness = new Command();
        setLowBrightness.setName(messages.getString("actionbrightness") + getPojo().getName() + " " + messages.getString("lowvalue"));
        setLowBrightness.setDescription("the light " + getPojo().getName() + " changes its brightness to low value");
        setLowBrightness.setReceiver("app.events.sensors.behavior.request.objects");
        setLowBrightness.setProperty("object", getPojo().getName());
        setLowBrightness.setProperty("behavior", BEHAVIOR_BRIGHTNESS);
        setLowBrightness.setProperty("value", BRI_LOW);
        
        Command setMedBrightness = new Command();
        setMedBrightness.setName(messages.getString("actionbrightness") + getPojo().getName() + " " + messages.getString("medvalue"));
        setMedBrightness.setDescription("the light " + getPojo().getName() + " changes its brightness to med value");
        setMedBrightness.setReceiver("app.events.sensors.behavior.request.objects");
        setMedBrightness.setProperty("object", getPojo().getName());
        setMedBrightness.setProperty("behavior", BEHAVIOR_BRIGHTNESS);
        setMedBrightness.setProperty("value", BRI_MED);
        
        Command setHighBrightness = new Command();
        setHighBrightness.setName(messages.getString("actionbrightness") + getPojo().getName() + " " + messages.getString("highvalue"));
        setHighBrightness.setDescription("the light " + getPojo().getName() + " changes its brightness to high value");
        setHighBrightness.setReceiver("app.events.sensors.behavior.request.objects");
        setHighBrightness.setProperty("object", getPojo().getName());
        setHighBrightness.setProperty("behavior", BEHAVIOR_BRIGHTNESS);
        setHighBrightness.setProperty("value", BRI_HIGH);
        
        Command setMaxBrightness = new Command();
        setMaxBrightness.setName(messages.getString("actionbrightness") + getPojo().getName() + " " + messages.getString("maxvalue"));
        setMaxBrightness.setDescription("the light " + getPojo().getName() + " changes its brightness to max value");
        setMaxBrightness.setReceiver("app.events.sensors.behavior.request.objects");
        setMaxBrightness.setProperty("object", getPojo().getName());
        setMaxBrightness.setProperty("behavior", BEHAVIOR_BRIGHTNESS);
        setMaxBrightness.setProperty("value", BRI_MAX);

        commandRepository.create(turnon);
        commandRepository.create(turnoff);
        commandRepository.create(setHueBlue);
        commandRepository.create(setHueRed);
        commandRepository.create(setHueGreen);
        commandRepository.create(setHueWhite);
        commandRepository.create(setHueYellow);
        commandRepository.create(setHueFucsia);
        commandRepository.create(setMaxSaturation);
        commandRepository.create(setMinBrightness);
        commandRepository.create(setLowBrightness);
        commandRepository.create(setMedBrightness);
        commandRepository.create(setHighBrightness);
        commandRepository.create(setMaxBrightness);
    }

    @Override
    protected void createTriggers() {
        super.createTriggers();
    }
}
