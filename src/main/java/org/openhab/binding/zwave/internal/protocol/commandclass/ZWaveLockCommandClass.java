/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.commandclass;

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

/**
 * Handles the Lock command class.
 *
 * @author Chris Jackson
 * @author Dave Badia
 */
@XStreamAlias("lockCommandClass")
public class ZWaveLockCommandClass extends ZWaveCommandClass implements ZWaveGetCommands, ZWaveSetCommands {

    private final static Logger logger = LoggerFactory.getLogger(ZWaveLockCommandClass.class);

    private static final int LOCK_SET = 0x01;
    /**
     * Request the state of the lock, ie {@link #LOCK_REPORT}
     */
    private static final int LOCK_GET = 0x02;
    /**
     * Details about the state of the lock (locked, unlocked)
     */
    private static final int LOCK_REPORT = 0x03;

    /**
     * Creates a new instance of the ZWaveAlarmCommandClass class.
     *
     * @param node the node this command class belongs to
     * @param controller the controller to use
     * @param endpoint the endpoint this Command class belongs to
     */
    public ZWaveLockCommandClass(ZWaveNode node, ZWaveController controller, ZWaveEndpoint endpoint) {
        super(node, controller, endpoint);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CommandClass getCommandClass() {
        return CommandClass.LOCK;
    }

    /**
     * {@inheritDoc}
     *
     * @throws ZWaveByteMessageException
     */
    @Override
    public void handleApplicationCommandRequest(ByteMessage serialMessage, int offset, int endpoint)
            throws ZWaveByteMessageException {
        logger.debug("NODE {}: Received Lock Request", this.getNode().getNodeId());
        int command = serialMessage.getMessagePayloadByte(offset);
        switch (command) {
            case LOCK_REPORT:
                int lockState = serialMessage.getMessagePayloadByte(offset + 1);
                logger.debug("NODE {}: Lock report - lockState={}", this.getNode().getNodeId(), lockState);

                ZWaveCommandClassValueEvent zEvent = new ZWaveCommandClassValueEvent(this.getNode().getNodeId(),
                        endpoint, CommandClass.LOCK, lockState);
                this.getController().notifyEventListeners(zEvent);
                break;
            default:
                logger.warn(String.format("Unsupported Command 0x%02X for command class %s (0x%02X).", command,
                        this.getCommandClass().getLabel(), this.getCommandClass().getKey()));
                break;
        }
    }

    /**
     * Gets a SerialMessage with the LOCK_GET command
     *
     * @return the serial message
     */
    @Override
    public ByteMessage getValueMessage() {
        logger.debug("NODE {}: Creating new message for application command LOCK_GET", this.getNode().getNodeId());
        ByteMessage result = new ByteMessage(this.getNode().getNodeId(), MessageClass.SendData,
                ByteMessageType.Request, MessageClass.ApplicationCommandHandler, ByteMessagePriority.Get);
        byte[] newPayload = { (byte) this.getNode().getNodeId(), 2, (byte) getCommandClass().getKey(),
                (byte) LOCK_GET, };
        result.setMessagePayload(newPayload);
        return result;
    }

    @Override
    public ByteMessage setValueMessage(int value) {
        logger.debug("NODE {}: Creating new message for application command LOCK_SET", this.getNode().getNodeId());

        ByteMessage result = new ByteMessage(this.getNode().getNodeId(), MessageClass.SendData,
                ByteMessageType.Request, MessageClass.SendData, ByteMessagePriority.Set);
        byte[] newPayload = { (byte) this.getNode().getNodeId(), 3, (byte) getCommandClass().getKey(), (byte) LOCK_SET,
                (byte) value };
        result.setMessagePayload(newPayload);
        return result;
    }
}
