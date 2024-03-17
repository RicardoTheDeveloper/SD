package src;

import java.util.Objects;
import java.util.Set;
import java.io.Serializable;
import java.util.HashSet;

public class PageInfo implements Serializable{
    private String url;
    private String title;
    private String snippet;
    private Set<String> uniqueWords = new HashSet<>(); 

    

    public PageInfo(String url, String title, String snippet,Set<String> uniqueWords)  {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.uniqueWords = uniqueWords;
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
}
