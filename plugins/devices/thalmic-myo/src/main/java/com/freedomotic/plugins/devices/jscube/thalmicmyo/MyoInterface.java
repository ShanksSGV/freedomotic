/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.freedomotic.plugins.devices.jscube.thalmicmyo;

import com.thalmic.myo.enums.PoseType;

/**
 *
 * @author Danny
 */
public interface MyoInterface {
    
    public void onPose(PoseType type);
    
}
