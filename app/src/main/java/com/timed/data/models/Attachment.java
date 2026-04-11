package com.timed.data.models;

import com.google.gson.annotations.SerializedName;
import java.io.Serializable;

/**
 * Represents an attachment for an event
 */
public class Attachment implements Serializable {
    @SerializedName("name")
    private String name;

    @SerializedName("file_path")
    private String filePath;

    @SerializedName("file_type")
    private String fileType; // e.g., "pdf", "image", "document"

    @SerializedName("file_size")
    private long fileSize; // in bytes

    @SerializedName("upload_time")
    private long uploadTime; // timestamp

    public Attachment() {}

    public Attachment(String name, String filePath, String fileType, long fileSize, long uploadTime) {
        this.name = name;
        this.filePath = filePath;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.uploadTime = uploadTime;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(long uploadTime) {
        this.uploadTime = uploadTime;
    }
}

