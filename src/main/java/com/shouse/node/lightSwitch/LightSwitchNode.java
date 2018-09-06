package com.shouse.node.lightSwitch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import shouse.core.api.Notifier;
import shouse.core.common.SystemConstants;
import shouse.core.communication.NodeCommunicator;
import shouse.core.communication.Packet;
import shouse.core.node.ExecutableNode;
import shouse.core.node.NodeInfo;
import shouse.core.node.NodeLocation;
import shouse.core.node.request.Request;
import shouse.core.node.response.ExecutionStatus;
import shouse.core.node.response.Response;
import shouse.core.node.response.ResponseStatus;

import java.time.LocalDateTime;
import java.util.List;

public class LightSwitchNode extends ExecutableNode {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private boolean turnedOn;
    private boolean requestedState;

    public LightSwitchNode(int id, NodeLocation nodeLocation, String description, NodeCommunicator nodeCommunicator, List<Notifier> notifiers) {
        super(id, nodeLocation, nodeCommunicator, notifiers, description);
        setTypeName(this.getClass().getSimpleName());
        this.turnedOn = false;
        this.requestedState = false;
    }

    public NodeInfo getNodeInfo() {
        return new LightSwitchNodeInfo(this);
    }

    public Response processRequest(Request request) {
        LOGGER.info(Thread.currentThread().getStackTrace()[1].getMethodName().concat(": ").concat(request.toString()));
        Response response = new Response();

        if (!isActive()) {
            return nodeIsNotActiveResponse();
        }

        if(isHasControlCommand()){
            return alreadyHasControlCommand();
        }

        if (request.getBody().getParameter("requestedState").equals("on")) {
            requestedState = true;
            setHasControlCommand(true);
            LOGGER.info("requestedSwitchState: on");
        }
        else if (request.getBody().getParameter("requestedState").equals("off")) {
            requestedState = false;
            setHasControlCommand(true);
            LOGGER.info("requestedSwitchState: off");
        }
        else {
            LOGGER.error("Request processing fail. Parameter value is wrong.");
            response.setStatus(ResponseStatus.FAILURE);
            response.put(SystemConstants.failureMessage, "Parameter value is wrong.");
            return response;
        }

        response.setStatus(ResponseStatus.SUCCESS);
        response.put(SystemConstants.nodeId, getId());
        response.put(SystemConstants.executionStatus, ExecutionStatus.IN_PROGRESS);

        Packet packet = new Packet(getId());
        packet.putData("switch", String.valueOf(requestedState));

        LOGGER.info("Control packet sending: ".concat(packet.toString()));
        getNodeCommunicator().sendPacket(packet);

        LOGGER.info("Return temporary: ".concat(response.toString()));
        return response;    }

    public void processPacket(Packet packet) {
        LOGGER.info(Thread.currentThread().getStackTrace()[1].getMethodName() + ": " + packet);

        //Alive packet detection
        if (packet.getData().get("alive") != null) {
            LOGGER.info("Received alive packet from Light Switch Node.");

            setLastAliveDate(LocalDateTime.now());

            if(isActive()) {
                LOGGER.info("Node already active. Last alive date updated.");
                return;
            }

            setActive(true);
            Response response = new Response(ResponseStatus.SUCCESS);
            response.put(SystemConstants.topic, SystemConstants.nodeAliveTopic);
            response.put(SystemConstants.nodeAliveState, true);
            response.put(SystemConstants.nodeId, getId());
            getNotifiers().stream().filter(notifier -> notifier != null).forEach(notifier -> notifier.sendResponse(response));

            return;
        }

        //Executed task detection
        LOGGER.info("Executed task detected. Requested switch state:"+requestedState);

        if ((packet.getData().get("state") != null) &&
                ((packet.getData().get("state").equals("true") && requestedState == true)
                            ||
                            (packet.getData().get("state").equals("false") &&  requestedState == false)
                )
        ){
            turnedOn = requestedState;
            setHasControlCommand(false);
            LOGGER.info(String.format("Executed successfully"));

            Response response = new Response(ResponseStatus.SUCCESS);
            response.put(SystemConstants.executionStatus, ExecutionStatus.READY);
            response.put(SystemConstants.nodeId, getId());
            response.put("turnedOn", turnedOn);
            getNotifiers().stream().filter(notifier -> notifier != null).forEach(notifier -> notifier.sendResponse(response));
        }
        else
            LOGGER.error("processPacket. Invalid packet from node. Packet: " + packet);
    }

    public boolean isTurnedOn() {
        return turnedOn;
    }
}
