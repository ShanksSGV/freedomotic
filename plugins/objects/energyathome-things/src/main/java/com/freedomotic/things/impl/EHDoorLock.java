/**
 *
 * Copyright (c) 2009-2014 Freedomotic team http://freedomotic.com
 *
 * This file is part of Freedomotic
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
package com.freedomotic.things.impl;

import com.freedomotic.things.GenericGate;
import com.freedomotic.environment.EnvironmentLogic;
import com.freedomotic.environment.Room;
import com.freedomotic.environment.ZoneLogic;
import com.freedomotic.model.ds.Config;
import com.freedomotic.model.geometry.FreedomPolygon;
import com.freedomotic.model.object.BooleanBehavior;
import com.freedomotic.model.object.Representation;
import com.freedomotic.behaviors.BooleanBehaviorLogic;
import com.freedomotic.things.EnvObjectLogic;
import com.freedomotic.reactions.Command;
import com.freedomotic.reactions.Trigger;
import com.freedomotic.util.TopologyUtils;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 *
 * @author Enrico
 */
public class EHDoorLock extends EnvObjectLogic implements GenericGate {
    //suppose from and to are always reflexive from->to; to->from

    //partial implementation of i18n... waiting for future improvements
    //at the moment this is embedded... Can we take it from the manifest? or from the JFrontend plugin?
    Locale it = new Locale("it", "IT");
    ResourceBundle messages = ResourceBundle.getBundle("data/i18n/energyathomethings", it);

    
    private Room from;
    private Room to;

    protected BooleanBehaviorLogic status;
    protected final static String BEHAVIOR_STATUS = "status";

    @Override
    public void init() {
        super.init();
        //linking this open property with the open behavior defined in the XML
        status = new BooleanBehaviorLogic((BooleanBehavior) getPojo().getBehavior(BEHAVIOR_STATUS));
        status.addListener(new BooleanBehaviorLogic.Listener() {
            @Override
            public void onTrue(Config params, boolean fireCommand) {
                //open = true
                setOpen(params);
            }

            @Override
            public void onFalse(Config params, boolean fireCommand) {
                //open = false -> not open
                setClosed(params);
            }
        });

        //register this behavior to the superclass to make it visible to it
        registerBehavior(status);
        getPojo().setDescription("Connects no rooms");
        //evaluate witch rooms it connects (based on gate position)
        //the evaluation updates the gate description
        evaluateGate();
    }

    /**
     *
     * @param params
     */
    protected void setClosed(Config params) {
        boolean executed = executeCommand("close doorlock", params); //executes the developer level command associated with 'set brightness' action

        if (executed) {
            status.setValue(false);
            //set the light graphical representation
            getPojo().setCurrentRepresentation(0); //points to the first element in the XML views array (closed door)
            setChanged(true);
        }
    }

    /**
     *
     * @param params
     */
    protected void setOpen(Config params) {
        boolean executed = executeCommand("open doorlock", params); //executes the developer level command associated with 'set brightness' action

        if (executed) {
            status.setValue(true);
            //set the light graphical representation
            getPojo().setCurrentRepresentation(1); //points to the second element in the XML views array (open door)
            setChanged(true);
        }
    }

    /**
     *
     * @param value
     */
    @Override
    public final void setChanged(boolean value) {
        //update the room that can be reached
        for (EnvironmentLogic env : environmentRepository.findAll()) {
            for (ZoneLogic z : env.getZones()) {
                if (z instanceof Room) {
                    final Room room = (Room) z;
                    //the gate is opened or closed we update the reachable rooms
                    room.visit();
                }
            }

            for (ZoneLogic z : env.getZones()) {
                if (z instanceof Room) {
                    final Room room = (Room) z;
                    room.updateDescription();
                }
            }
        }

        //then executeCommand the super which notifies the event
        super.setChanged(true);
    }

    /**
     *
     * @return
     */
    @Override
    public boolean isOpen() {
        return status.getValue();
    }

    /**
     *
     * @return
     */
    @Override
    public Room getFrom() {
        return from;
    }

    /**
     *
     * @return
     */
    @Override
    public Room getTo() {
        return to;
    }

    /**
     *
     * @param x
     * @param y
     */
    @Override
    public void setLocation(int x, int y) {
        super.setLocation(x, y);
        evaluateGate();
    }

    /**
     *
     */
    @Override
    public void evaluateGate() {
        //checks the intersection with the first view in the list
        //others views are ignored!!!
        Representation representation = getPojo().getRepresentations().get(0);
        FreedomPolygon pojoShape = (FreedomPolygon) representation.getShape();
        int xoffset = representation.getOffset().getX();
        int yoffset = representation.getOffset().getY();
        from = null;
        to = null;

        //REGRESSION
        FreedomPolygon objShape
                = TopologyUtils.rotate(TopologyUtils.translate(pojoShape, xoffset, yoffset),
                        (int) representation.getRotation());
        EnvironmentLogic env = environmentRepository.findOne(getPojo().getEnvironmentID());

        if (env != null) {
            for (Room room : env.getRooms()) {
                if (TopologyUtils.intersects(objShape,
                        room.getPojo().getShape())) {
                    if (from == null) {
                        from = (Room) room;
                        to = (Room) room;
                    } else {
                        to = (Room) room;
                    }
                }
            }
        } else {
            LOG.severe("The gate '" + getPojo().getName()
                    + "' is not linked to any any environment");
        }

        if (to != from) {
            getPojo().setDescription("Connects " + from + " to " + to);
            from.addGate(this); //informs the room that it has a gate to another room
            to.addGate(this); //informs the room that it has a gate to another room
        } else {
            //the gate interects two equals zones
            if (from != null) {
                LOG.warning("The gate '" + getPojo().getName() + "' connects the same zones ["
                        + from.getPojo().getName() + "; " + to.getPojo().getName()
                        + "]. This is not possible.");
            }
        }

        //notify if the passage connect two rooms
        LOG.config("The gate '" + getPojo().getName() + "' connects " + from + " to " + to);
    }

    /**
     *
     */
    @Override
    protected void createCommands() {

        Command h = new Command();
        h.setName(messages.getString("open") + getPojo().getName());
        h.setDescription(getPojo().getSimpleType() + " opens");
        h.setReceiver("app.events.sensors.behavior.request.objects");
        h.setProperty("object",
                getPojo().getName());
        h.setProperty("behavior", BEHAVIOR_STATUS);
        h.setProperty("value", "true");

        Command i = new Command();
        i.setName(messages.getString("close") + getPojo().getName());
        i.setDescription(getPojo().getSimpleType() + " closes");
        i.setReceiver("app.events.sensors.behavior.request.objects");
        i.setProperty("object",
                getPojo().getName());
        i.setProperty("behavior", BEHAVIOR_STATUS);
        i.setProperty("value", "false");

        commandRepository.create(h);
        commandRepository.create(i);
    }

    /**
     *
     */
    @Override
    protected void createTriggers() {
        Trigger clicked = new Trigger();
        clicked.setName("When " + this.getPojo().getName() + " is clicked");
        clicked.setChannel("app.event.sensor.object.behavior.clicked");
        clicked.getPayload().addStatement("object.name",
                this.getPojo().getName());
        clicked.getPayload().addStatement("click", "SINGLE_CLICK");

        Trigger turnsOpen = new Trigger();
        turnsOpen.setName(this.getPojo().getName() + " becomes open");
        turnsOpen.setChannel("app.event.sensor.object.behavior.change");
        turnsOpen.getPayload().addStatement("object.name",
                this.getPojo().getName());
        turnsOpen.getPayload().addStatement("object.behavior." + BEHAVIOR_STATUS, "true");

        Trigger turnsClosed = new Trigger();
        turnsClosed.setName(this.getPojo().getName() + " becomes closed");
        turnsClosed.setChannel("app.event.sensor.object.behavior.change");
        turnsClosed.getPayload().addStatement("object.name",
                this.getPojo().getName());
        turnsClosed.getPayload().addStatement("object.behavior." + BEHAVIOR_STATUS, "false");

        triggerRepository.create(clicked);
        triggerRepository.create(turnsOpen);
        triggerRepository.create(turnsClosed);
    }
    private static final Logger LOG = Logger.getLogger(Gate.class.getName());
}
