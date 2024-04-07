package src;

import java.io.Serializable;

public class URLIndexingDepthControl implements Serializable,  Comparable<URLIndexingDepthControl> {
    private static final long serialVersionUID = 1L; // Choose any long value for the UID
    private final String url;
    private final int depth;
    private final long timestamp;

    public URLIndexingDepthControl(String url, int depth) {
        this.url = url;
        this.depth = depth;
        this.timestamp = System.currentTimeMillis(); // Set timestamp here
    }

    public String getUrl() { return url; }
    public int getDepth() { return depth; }
    public long getTimestamp() { return timestamp; }

    @Override
    public int compareTo(URLIndexingDepthControl other) {
        int depthCompare = Integer.compare(this.depth, other.depth);
        if (depthCompare != 0) {
            return depthCompare;
        }
        return Long.compare(this.timestamp, other.timestamp);
    }
}


