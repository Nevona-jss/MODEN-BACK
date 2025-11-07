package com.moden.modenapi.common.dto;

public record UploadResponse(
        String url,         // public URL: /uploads/<folder>/<date>/<filename>
        String path,        // disk path (relative): <folder>/<date>/<filename>
        String filename,    // generated filename
        long size,
        String contentType
) {}
