/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.commandclass;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.openhab.binding.zwave.internal.protocol.ByteMessage;
import org.openhab.binding.zwave.internal.protocol.ByteMessage.MessageClass;
import org.openhab.binding.zwave.internal.protocol.ByteMessage.ByteMessagePriority;
import org.openhab.binding.zwave.internal.protocol.ByteMessage.ByteMessageType;
import org.openhab.binding.zwave.internal.protocol.ZWaveController;
import org.openhab.binding.zwave.internal.protocol.ZWaveEndpoint;
import org.openhab.binding.zwave.internal.protocol.ZWaveNode;
import org.openhab.binding.zwave.internal.protocol.ZWaveByteMessageException;
import org.openhab.binding.zwave.internal.protocol.event.ZWaveCommandClassValueEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamOmitField;

/**
 * Handles the binary toggle switch command class.
 *
 * @author Chris Jackson
 */
@XStreamAlias("binaryToggleSwitchCommandClass")
public class ZWaveBinaryToggleSwitchCommandClass extends ZWaveCommandClass
        implements ZWaveBasicCommands, ZWaveCommandClassDynamicState {

    @XStreamOmitField
    private final static Logger logger = LoggerFactory.getLogger(ZWaveBinaryToggleSwitchCommandClass.class);

    private static final int SWITCH_TOGGLE_SET = 1;
    private static final int SWITCH_TOGGLE_GET = 2;
    private static final int SWITCH_TOGGLE_REPORT = 3;

    @XStreamOmitField
    private boolean dynamicDone = false;

    private boolean isGetSupported = true;

    /**
     * Creates a new instance of the ZWaveUserCodeCommandClass class.
     *
     * @param node the node this command class belongs to
     * @param controller the controller to use
     * @param endpoint the endpoint this Command class belongs to
     */
    public ZWaveBinaryToggleSwitchCommandClass(ZWaveNode node, ZWaveController controller, ZWaveEndpoint endpoint) {
        super(node, controller, endpoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommandClass getCommandClass() {
        return CommandClass.SWITCH_TOGGLE_BINARY;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ZWaveByteMessageException
     */
    @Override
    public void handleApplicationCommandRequest(ByteMessage serialMessage, int offset, int endpoint)
            throws ZWaveByteMessageException {
        logger.debug("NODE {}: Received binary toggle switch command (v{})", this.getNode().getNodeId(),
                this.getVersion());
        int command = serialMessage.getMessagePayloadByte(offset);
        switch (command) {
            case SWITCH_TOGGLE_REPORT:
                processSwitchBinaryToggleReport(serialMessage, offset, endpoint);

                dynamicDone = true;
                break;
            default:
                logger.warn(String.format("NODE %d: Unsupported Command %d for command class %s (0x%02X).",
                        this.getNode().getNodeId(), command, this.getCommandClass().getLabel(),
                        this.getCommandClass().getKey()));
        }
    }

    /**
     * Processes a report message.
     *
     * @param serialMessage the incoming message to process.
     * @param offset the offset position from which to start message processing.
     * @param endpoint the endpoint or instance number this message is meant for.
     * @throws ZWaveByteMessageException
     */
    protected void processSwitchBinaryToggleReport(ByteMessage serialMessage, int offset, int endpoint)
            throws ZWaveByteMessageException {
        int value = serialMessage.getMessagePayloadByte(offset + 1);
        logger.debug(String.format("NODE %d: Switch binary toggle report, value = 0x%02X", this.getNode().getNodeId(),
                value));
        ZWaveCommandClassValueEvent zEvent = new ZWaveCommandClassValueEvent(this.getNode().getNodeId(), endpoint,
                this.getCommandClass(), value);
        this.getController().notifyEventListeners(zEvent);
    }

    @Override
    public ByteMessage getValueMessage() {
        if (isGetSupported == false) {
            logger.debug("NODE {}: Node doesn't support get requests", this.getNode().getNodeId());
            return null;
        }

        logger.debug("NODE {}: Creating new message for application command SWITCH_TOGGLE_GET",
                this.getNode().getNodeId());
        ByteMessage result = new ByteMessage(this.getNode().getNodeId(), MessageClass.SendData,
                ByteMessageType.Request, MessageClass.ApplicationCommandHandler, ByteMessagePriority.Get);
        byte[] newPayload = { (byte) this.getNode().getNodeId(), 2, (byte) getCommandClass().getKey(),
                (byte) SWITCH_TOGGLE_GET };
        result.setMessagePayload(newPayload);
        return result;
    }

    @Override
    public ByteMessage setValueMessage(int value) {
        logger.debug("NODE {}: Creating new message for application command SWITCH_TOGGLE_SET",
                this.getNode().getNodeId());
        ByteMessage result = new ByteMessage(this.getNode().getNodeId(), MessageClass.SendData,
                ByteMessageType.Request, MessageClass.SendData, ByteMessagePriority.Set);
        byte[] newPayload = { (byte) this.getNode().getNodeId(), 2, (byte) getCommandClass().getKey(),
                (byte) SWITCH_TOGGLE_SET };
        result.setMessagePayload(newPayload);
        return result;
    }

    @Override
    public Collection<ByteMessage> getDynamicValues(boolean refresh) {
        ArrayList<ByteMessage> result = new ArrayList<ByteMessage>();

        if (refresh == true || dynamicDone == false) {
            result.add(getValueMessage());
        }
        return result;
    }

    @Override
    public boolean setOptions(Map<String, String> options) {
        if ("false".equals(options.get("getSupported"))) {
            isGetSupported = false;
        }

        return true;
    }
}
