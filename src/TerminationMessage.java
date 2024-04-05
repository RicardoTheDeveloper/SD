package src;

import java.io.Serializable;

/**
 * The TerminationMessage class represents a message indicating the termination of a thread.
 * It implements the Serializable interface to allow for object serialization.
 */
public class TerminationMessage implements Serializable {
    private final int threadId;

    /**
     * Constructs a TerminationMessage object with the specified thread ID.
     *
     * @param threadId the ID of the thread that has terminated
     */
    public TerminationMessage(int threadId) {
        this.threadId = threadId;
    }

    /**
     * Returns the ID of the thread that has terminated.
     *
     * @return the thread ID
     */
    public int getThreadId() {
        return threadId;
    }
}
