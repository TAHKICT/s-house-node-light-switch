package com.shouse.node.lightSwitch;

import shouse.core.node.NodeInfo;

public class LightSwitchNodeInfo extends NodeInfo {
    private boolean turnedOn;
    private boolean inProcess;

    public LightSwitchNodeInfo(LightSwitchNode lightSwitchNode) {
        super(lightSwitchNode.getId(),
                lightSwitchNode.getTypeName(),
                lightSwitchNode.getNodeLocation(),
                lightSwitchNode.getDescription());
        this.turnedOn = lightSwitchNode.isTurnedOn();
        this.inProcess = lightSwitchNode.isInProcess();
    }

    public boolean isTurnedOn() {
        return turnedOn;
    }

    public boolean isInProcess() {
        return inProcess;
    }
}
