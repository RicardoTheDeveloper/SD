package src;

import java.util.Objects;
import java.util.Set;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class PageInfo implements Serializable{
    private String url;
    private String title;
    private String snippet;
    private Set<String> uniqueWords = new HashSet<>(); 
    private Map<String, Integer> wordCount = new HashMap<>(); // New field to store word counts

    

    public PageInfo(String url, String title, String snippet,Set<String> uniqueWords,Map<String,Integer> WordCount)  {
        this.url = url;
        this.title = title;
        this.snippet = snippet;
        this.uniqueWords = uniqueWords;
        this.wordCount = WordCount;
        
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

    public void updateWordCount(String word, int count) {
        wordCount.put(word, count);
    }

    public int getWordCount(String word) {
        return wordCount.getOrDefault(word, 0);
    }

    public void showWordCount() {
        System.out.println("Word Count:");
        for (Map.Entry<String, Integer> entry : wordCount.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
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
