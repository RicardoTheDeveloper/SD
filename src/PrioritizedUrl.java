package src;

public class PrioritizedUrl implements Comparable<PrioritizedUrl> {
    private final String url;
    private final int priority;

    public PrioritizedUrl(String url, int priority) {
        this.url = url;
        this.priority = priority;
    }

    public String getUrl() {
        return url;
    }

    public int getPriority() {
        return priority;
    }

    @Override
    public int compareTo(PrioritizedUrl o) {
        // Higher priorities should come first
        return Integer.compare(o.priority, this.priority);
    }
}

