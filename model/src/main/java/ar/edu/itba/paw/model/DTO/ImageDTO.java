package ar.edu.itba.paw.model.DTO;

import javax.persistence.*;

public class ImageDTO {

    private long id;
    private String filename;
    private String contentType;
    private int sizeBytes;
    private byte[] data;

    public ImageDTO(String filename, String contentType, int sizeBytes, byte[] data) {
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.data = data;
    }

    public String getFilename() {
        return filename;}
    public String getContentType() {
        return contentType;
    }
    public int getSizeBytes() {
        return sizeBytes;
    }
    public byte[] getData() {
        return data;
    }
}
