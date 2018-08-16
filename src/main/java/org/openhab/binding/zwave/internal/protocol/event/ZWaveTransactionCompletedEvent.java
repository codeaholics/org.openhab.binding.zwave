/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal.protocol.event;

import org.openhab.binding.zwave.internal.protocol.ByteMessage;

/**
 * ZWave Transaction Completed Event. Indicated that a transaction (a sequence of messages with an expected reply) has
 * completed.
 * 
 * @author Chris Jackson
 * @author Jan-Willem Spuij
 */
public class ZWaveTransactionCompletedEvent extends ZWaveEvent {

    private final ByteMessage completedMessage;
    private final boolean state;

    /**
     * Constructor. Creates a new instance of the ZWaveTransactionCompletedEvent class.
     * The 'state' flag is provided by the message handler when the message is processed and its value is defined by the
     * message class.
     * 
     * @param completedMessage the original {@link ByteMessage} that has been completed.
     * @param state a flag indicating success / failure of the transaction processing
     */
    public ZWaveTransactionCompletedEvent(ByteMessage completedMessage, boolean state) {
        super(completedMessage.getMessageNode());

        this.completedMessage = completedMessage;
        this.state = state;
    }

    /**
     * Gets the original {@link ByteMessage} that has been completed.
     * 
     * @return the original message.
     */
    public ByteMessage getCompletedMessage() {
        return completedMessage;
    }

    /**
     * Returns the processing state of this transaction
     * 
     * @return
     */
    public boolean getState() {
        return state;
    }
}
