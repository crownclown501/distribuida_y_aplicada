package com.pda.practica2.model;

import java.io.Serializable;
import java.util.Date;

public class FileInfo implements Serializable {
    private String fileName;
    private long fileSize;
    private String uploadedBy;
    private Date uploadDate;
    private String fileType; // mp3, mp4, etc.

    public FileInfo(String fileName, long fileSize, String uploadedBy) {
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.uploadedBy = uploadedBy;
        this.uploadDate = new Date(); // Fecha y hora actual
        
        // Determinar el tipo de archivo basado en la extensión
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            this.fileType = fileName.substring(dotIndex + 1).toLowerCase();
        } else {
            this.fileType = "unknown";
        }
    }

    public String getFileName() {
        return fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public String getReadableFileSize() {
        if (fileSize < 1024) {
            return fileSize + " B";
        } else if (fileSize < 1024 * 1024) {
            return String.format("%.2f KB", fileSize / 1024.0);
        } else if (fileSize < 1024 * 1024 * 1024) {
            return String.format("%.2f MB", fileSize / (1024.0 * 1024));
        } else {
            return String.format("%.2f GB", fileSize / (1024.0 * 1024 * 1024));
        }
    }

    public String getUploadedBy() {
        return uploadedBy;
    }

    public Date getUploadDate() {
        return uploadDate;
    }

    public String getFileType() {
        return fileType;
    }

    @Override
    public String toString() {
        return fileName + " (" + getReadableFileSize() + ") - Subido por: " + uploadedBy;
    }
}