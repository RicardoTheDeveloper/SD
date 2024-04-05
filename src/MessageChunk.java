package src;

import java.io.Serializable;


/**
 * Represents a chunk of a message, used for breaking down large messages into smaller parts.
 * This class is serializable to enable sending over network connections.
 */

public class MessageChunk implements Serializable {
    private static final long serialVersionUID = 1L;
    private int index;
    private int totalChunks;
    private byte[] chunkData;
    private String messageId; // Identificador único para a mensagem
    private int downloaderNackPort; // New field
    private int threadID; 

    /**
     * Constructs a new MessageChunk.
     * 
     * @param index The index of this chunk in the sequence.
     * @param totalChunks The total number of chunks in the message.
     * @param chunkData The data contained in this chunk.
     * @param messageId A unique identifier for the entire message.
     * @param downloaderNackPort The port number for sending NACK messages.
     * @param threadID The identifier of the thread processing this chunk.
     */
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