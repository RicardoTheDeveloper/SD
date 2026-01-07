package src;

import java.io.Serializable;
import java.util.Set;

public class NACKMessage implements Serializable {
    private static final long serialVersionUID = 1L; 

    private String messageId;
    private Set<Integer> missingChunks;

    public NACKMessage(String messageId, Set<Integer> missingChunks) {
        this.messageId = messageId;
        this.missingChunks = missingChunks;
    }

    // Getters
    public String getMessageId() {
        return messageId;
    }

    public Set<Integer> getMissingChunks() {
        return missingChunks;
    }

    
}

