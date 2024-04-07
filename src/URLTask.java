package src;


import java.io.Serializable;

public class URLTask implements Serializable, Comparable<URLTask> {

    private String url;
    private int priority; // Lower numbers indicate higher priority

    public URLTask(String url, int priority) {
        this.url = url;
        this.priority = priority;
    }

    public String getUrl() {
        return url;
    }

    // Standard getters and setters

    @Override
    public int compareTo(URLTask other) {
        return Integer.compare(this.priority, other.priority);
    }

    public int getPriority() {
        return priority;
    }
    
}
