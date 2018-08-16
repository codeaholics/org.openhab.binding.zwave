/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.messages;

import java.io.ByteArrayOutputStream;

import org.openhab.binding.zwave.internal.protocol.ByteMessage;
import org.openhab.binding.zwave.internal.protocol.ByteMessage.MessageClass;
import org.openhab.binding.zwave.internal.protocol.ByteMessage.ByteMessagePriority;
import org.openhab.binding.zwave.internal.protocol.ByteMessage.ByteMessageType;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveByteMessageException;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveNetworkEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class processes a serial message from the zwave controller
 *
 * @author Jorg de Jong
 */
public class DeleteSucReturnRouteMessageClass extends ZWaveCommandProcessor {
    private final static Logger logger = LoggerFactory.getLogger(DeleteSucReturnRouteMessageClass.class);

    public ByteMessage doRequest(int nodeId) {
        logger.debug("NODE {}: Deleting SUC return routes", nodeId);

        // Queue the request
        ByteMessage newMessage = new ByteMessage(MessageClass.DeleteSUCReturnRoute, ByteMessageType.Request,
                MessageClass.DeleteSUCReturnRoute, ByteMessagePriority.High);

        ByteArrayOutputStream outputData = new ByteArrayOutputStream();
        outputData.write(nodeId);
        outputData.write(0x01); // callback id
        newMessage.setMessagePayload(outputData.toByteArray());

        return newMessage;
    }

    @Override
    public boolean handleResponse(ZWaveController zController, ByteMessage lastSentMessage,
            ByteMessage incomingMessage) throws ZWaveByteMessageException {
        int nodeId = lastSentMessage.getMessagePayloadByte(0);

        logger.debug("NODE {}: Got DeleteSUCReturnRoute response.", nodeId);
        if (incomingMessage.getMessagePayloadByte(0) != 0x00) {
            lastSentMessage.setAckRecieved();
            logger.debug("NODE {}: DeleteSUCReturnRoute command in progress.", nodeId);
        } else {
            logger.debug("NODE {}: DeleteSUCReturnRoute command failed.", nodeId);
            zController.notifyEventListeners(new ZWaveNetworkEvent(ZWaveNetworkEvent.Type.DeleteSucReturnRoute, nodeId,
                    ZWaveNetworkEvent.State.Failure));
        }

        return true;
    }

    @Override
    public boolean handleRequest(ZWaveController zController, ByteMessage lastSentMessage,
            ByteMessage incomingMessage) throws ZWaveByteMessageException {
        int nodeId = lastSentMessage.getMessagePayloadByte(0);

        logger.debug("NODE {}: Got DeleteSUCReturnRoute request.", nodeId);
        if (incomingMessage.getMessagePayloadByte(1) != 0x00) {
            logger.debug("NODE {}: Delete SUC return routes failed.", nodeId);
            zController.notifyEventListeners(new ZWaveNetworkEvent(ZWaveNetworkEvent.Type.DeleteSucReturnRoute, nodeId,
                    ZWaveNetworkEvent.State.Failure));
        } else {
            zController.notifyEventListeners(new ZWaveNetworkEvent(ZWaveNetworkEvent.Type.DeleteSucReturnRoute, nodeId,
                    ZWaveNetworkEvent.State.Success));
        }

        checkTransactionComplete(lastSentMessage, incomingMessage);

        return true;
    }
}