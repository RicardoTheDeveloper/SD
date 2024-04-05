package src;

import java.io.Serializable;
import java.util.Set;


/**
 * Represents a Negative Acknowledgement (NACK) message. 
 * This message is sent to indicate which chunks of a larger message were not received 
 * and need to be resent. The class is serializable for easy transmission over a network.
 */

public class NACKMessage implements Serializable {
    private static final long serialVersionUID = 1L; 

    private String messageId;
    private Set<Integer> missingChunks;

    /**
     * Constructs a NACKMessage.
     * 
     * @param messageId The unique identifier of the original message that this NACK is addressing.
     * @param missingChunks A set of integers representing the indices of chunks that were not received.
     */
    public NACKMessage(String messageId, Set<Integer> missingChunks) {
        this.messageId = messageId;
        this.missingChunks = missingChunks;
    }

    /**
     * Returns the message identifier associated with this NACK message.
     * 
     * @return The message identifier as a String.
     */
    public String getMessageId() {
        return messageId;
    }

    /**
     * Returns the set of indices of the missing chunks.
     * 
     * @return A Set of integers representing the indices of missing chunks.
     */
    public Set<Integer> getMissingChunks() {
        return missingChunks;
    }

    
}