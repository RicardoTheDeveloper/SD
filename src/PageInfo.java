package src;

import java.util.Objects;
import java.util.Set;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PageInfo implements Serializable{

    private static final long serialVersionUID = 1L; // Recommended for Serializable classes


    private String url;
    private String title;
    private String snippet;
    private Set<String> uniqueWords;
  
    private Set<String> outboundLinks = new HashSet<>(); // URLs found on the page
    private Set<String> inboundLinks = new HashSet<>(); // URLs of other pages that link to this page

    

    public PageInfo(String url, String title, String snippet, Set<String> uniqueWords) {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.uniqueWords = uniqueWords != null ? uniqueWords : new HashSet<>();
        this.outboundLinks = new HashSet<>();
        this.inboundLinks = new HashSet<>();
    }

    // Getters
    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getSnippet() {
        return snippet;
    }

    public Set<String> getUniqueWords(){
        return uniqueWords;
    }

    // Setters
    public void setUrl(String url) {
        this.url = url;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public void setUniqueWords(Set<String> uniqueWords){
        this.uniqueWords = uniqueWords;
    }

    

    public void setOutboundLinks(Set<String> outboundLinks) {
        this.outboundLinks = outboundLinks;
    }

    public Set<String> getOutboundLinks() {
        return outboundLinks;
    }

    // Setter and Getter for inboundLinks
    public void setInboundLinks(Set<String> inboundLinks) {
        this.inboundLinks = inboundLinks;
    }

    public Set<String> getInboundLinks() {
        return inboundLinks;
    }

    // Add a single inbound link
    public void addInboundLink(String link) {
        this.inboundLinks.add(link);
    }

    // Add multiple inbound links
    public void addInboundLinks(Set<String> links) {
        this.inboundLinks.addAll(links);
    }

    

    

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageInfo pageInfo = (PageInfo) o;
        return Objects.equals(url, pageInfo.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url);
    }


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
