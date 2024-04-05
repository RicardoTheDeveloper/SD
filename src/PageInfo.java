package src;

import java.util.Objects;
import java.util.Set;
import java.io.Serializable;
import java.util.HashSet;

/**
 * Represents information about a web page.
 */
public class PageInfo implements Serializable{

    private static final long serialVersionUID = 1L;
    private String url;
    private String title;
    private String snippet;
    private Set<String> uniqueWords;
    private Set<String> outboundLinks = new HashSet<>();
    private Set<String> inboundLinks = new HashSet<>();

    /**
     * Constructs a PageInfo object with the specified URL, title, snippet, and unique words.
     * 
     * @param url The URL of the web page.
     * @param title The title of the web page.
     * @param snippet A short description or snippet of the web page.
     * @param uniqueWords A set of unique words found in the web page.
     */
    public PageInfo(String url, String title, String snippet, Set<String> uniqueWords) {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.uniqueWords = uniqueWords != null ? uniqueWords : new HashSet<>();
        this.outboundLinks = new HashSet<>();
        this.inboundLinks = new HashSet<>();
    }

    /**
     * Returns the URL of the web page.
     * 
     * @return The URL of the web page.
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns the title of the web page.
     * 
     * @return The title of the web page.
     */
    public String getTitle() {
        return title;
    }

    /**
     * Returns a short description or snippet of the web page.
     * 
     * @return A short description or snippet of the web page.
     */
    public String getSnippet() {
        return snippet;
    }

    /**
     * Returns a set of unique words found in the web page.
     * 
     * @return A set of unique words found in the web page.
     */
    public Set<String> getUniqueWords(){
        return uniqueWords;
    }

    /**
     * Sets the URL of the web page.
     * 
     * @param url The URL of the web page.
     */
    public void setUrl(String url) {
        this.url = url;
    }

    /**
     * Sets the title of the web page.
     * 
     * @param title The title of the web page.
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * Sets a short description or snippet of the web page.
     * 
     * @param snippet A short description or snippet of the web page.
     */
    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    /**
     * Sets the set of unique words found in the web page.
     * 
     * @param uniqueWords A set of unique words found in the web page.
     */
    public void setUniqueWords(Set<String> uniqueWords){
        this.uniqueWords = uniqueWords;
    }

    /**
     * Sets the set of outbound links from the web page.
     * 
     * @param outboundLinks A set of outbound links from the web page.
     */
    public void setOutboundLinks(Set<String> outboundLinks) {
        this.outboundLinks = outboundLinks;
    }

    /**
     * Returns the set of outbound links from the web page.
     * 
     * @return A set of outbound links from the web page.
     */
    public Set<String> getOutboundLinks() {
        return outboundLinks;
    }

    /**
     * Sets the set of inbound links to the web page.
     * 
     * @param inboundLinks A set of inbound links to the web page.
     */
    public void setInboundLinks(Set<String> inboundLinks) {
        this.inboundLinks = inboundLinks;
    }

    /**
     * Returns the set of inbound links to the web page.
     * 
     * @return A set of inbound links to the web page.
     */
    public Set<String> getInboundLinks() {
        return inboundLinks;
    }

    /**
     * Adds an inbound link to the web page.
     * 
     * @param link The inbound link to be added.
     */
    public void addInboundLink(String link) {
        this.inboundLinks.add(link);
    }

    /**
     * Adds a set of inbound links to the web page.
     * 
     * @param links The set of inbound links to be added.
     */
    public void addInboundLinks(Set<String> links) {
        this.inboundLinks.addAll(links);
    }

    /**
     * Checks if this PageInfo object is equal to another object.
     * 
     * @param o The object to compare with.
     * @return true if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageInfo pageInfo = (PageInfo) o;
        return Objects.equals(url, pageInfo.url);
    }

    /**
     * Returns the hash code value for this PageInfo object.
     * 
     * @return The hash code value for this PageInfo object.
     */
    @Override
    public int hashCode() {
        return Objects.hash(url);
    }

    /**
     * Returns a string representation of this PageInfo object.
     * 
     * @return A string representation of this PageInfo object.
     */
    @Override
    public String toString() {
        return "PageInfo{" +
                "url='" + url + '\'' +
                ", title='" + title + '\'' +
                ", outBoundLinks='" + outboundLinks + '\'' +
                ", inboundLinks=" + inboundLinks +
                '}';
    }

}
