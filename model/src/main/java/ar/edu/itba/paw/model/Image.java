package ar.edu.itba.paw.model;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name="image")
public class Image {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "image_id_seq")
    @SequenceGenerator(sequenceName = "image_id_seq", name = "image_id_seq", allocationSize = 1)
    private long id;

    @Column(nullable = false)
    private String filename;
    @Column(name = "content_type", nullable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private int sizeBytes;

    @Column(name = "data", nullable = false)
    private byte[] data;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /* Default*/ Image() {
        //Just for Hibernate
    }

    public Image(String filename, String contentType, int sizeBytes, byte[] data) {
        this(filename, contentType, sizeBytes, data, null, LocalDateTime.now());
    }

    public Image(String filename, String contentType, int sizeBytes, byte[] data, User owner) {
        this(filename, contentType, sizeBytes, data, owner, LocalDateTime.now());
    }

    public Image(String filename, String contentType, int sizeBytes, byte[] data, User owner, LocalDateTime createdAt) {
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.data = data;
        this.owner = owner;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public Image(long id, String filename, String contentType, int sizeBytes, byte[] data) {
        this(id, filename, contentType, sizeBytes, data, null, LocalDateTime.now());
    }

    public Image(long id, String filename, String contentType, int sizeBytes, byte[] data, User owner, LocalDateTime createdAt) {
        this.id = id;
        this.filename = filename;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.data = data;
        this.owner = owner;
        this.createdAt = createdAt == null ? LocalDateTime.now() : createdAt;
    }

    public long getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public String getContentType() {
        return contentType;
    }

    public int getSizeBytes() {
        return sizeBytes;
    }

    public byte[] getData() {
        return data;
    }

    public User getOwner() {
        return owner;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public void setSizeBytes(int sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public void setOwner(User owner) {
        this.owner = owner;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

}
