package src;

import java.io.Serializable;

public class TerminationMessage implements Serializable {
    private final int threadId;

    public TerminationMessage(int threadId) {
        this.threadId = threadId;
    }

    public int getThreadId() {
        return threadId;
    }
}
