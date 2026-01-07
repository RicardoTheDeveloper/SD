package src;

import java.io.Serializable;

public class MessageChunk implements Serializable {
    private static final long serialVersionUID = 1L;
    private int index;
    private int totalChunks;
    private byte[] chunkData;
    private String messageId; // Identificador único para a mensagem
    private int downloaderNackPort; // New field
    private int threadID; 

    public MessageChunk(int index, int totalChunks, byte[] chunkData, String messageId,int downloaderNackPort,int threadID) {
        this.index = index;
        this.totalChunks = totalChunks;
        this.chunkData = chunkData;
        this.messageId = messageId;
        this.downloaderNackPort = downloaderNackPort;
        this.threadID = threadID;
    }

    // Getters
    public int getIndex() { return index; }
    public int getTotalChunks() { return totalChunks; }
    public byte[] getChunkData() { return chunkData; }
    public String getMessageId() { return messageId; } // Getter para o messageId
    public int getDownloaderNackPort() {return downloaderNackPort;}
    public int getThreadID() {return threadID;}
}
