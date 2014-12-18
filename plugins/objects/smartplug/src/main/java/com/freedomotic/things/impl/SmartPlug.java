package com.freedomotic.things.impl;

import com.freedomotic.model.ds.Config;
import com.freedomotic.model.object.RangedIntBehavior;
import com.freedomotic.behaviors.RangedIntBehaviorLogic;

public class SmartPlug extends ElectricDevice
{
	private RangedIntBehaviorLogic powerUsage;	
	protected static final String BEHAVIOR_POWERUSAGE = "powerUsage";
	
	public SmartPlug()
	{
	}
	
        @Override
	public void init()
	{
		powerUsage = new RangedIntBehaviorLogic((RangedIntBehavior)getPojo().getBehavior(BEHAVIOR_POWERUSAGE)); 
		powerUsage.addListener(new RangedIntBehaviorLogic.Listener() {

			@Override
			public void onLowerBoundValue(Config params, boolean fireCommand) {
			}

			@Override
			public void onRangeValue(int value, Config params, boolean fireCommand) {
//				setPowerUsage(value, params);
			}

			@Override
			public void onUpperBoundValue(Config params, boolean fireCommand) {
			}
		});
		
		registerBehavior(powerUsage);
		super.init();
	}
	
//	public void setPowerUsage(int value, Config params){
//		
//		powerUsage.setValue(value);
//	}
	
    @Override
    protected void createCommands() {

    }
    
    @Override
    protected void createTriggers() {
        super.createTriggers();
    }
}
