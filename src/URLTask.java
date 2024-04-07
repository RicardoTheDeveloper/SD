package src;


import java.io.Serializable;

/**
 * Represents a URL task with a priority.
 * Implements the Serializable interface to support object serialization.
 * Implements the Comparable interface to enable comparison based on priority.
 */
public class URLTask implements Serializable, Comparable<URLTask> {

    private String url;
    private int priority;

    /**
     * Constructs a URLTask object with the specified URL and priority.
     *
     * @param url      the URL associated with the task
     * @param priority the priority of the task
     */
    public URLTask(String url, int priority) {
        this.url = url;
        this.priority = priority;
    }

    /**
     * Returns the URL associated with the task.
     *
     * @return the URL of the task
     */
    public String getUrl() {
        return url;
    }

    /**
     * Compares this URLTask with another URLTask based on their priorities.
     *
     * @param other the URLTask to compare with
     * @return a negative integer, zero, or a positive integer if this URLTask has a lower, equal, or higher priority than the other URLTask
     */
    @Override
    public int compareTo(URLTask other) {
        return Integer.compare(this.priority, other.priority);
    }

    /**
     * Returns the priority of the task.
     *
     * @return the priority of the task
     */
    public int getPriority() {
        return priority;
    }
    
}
