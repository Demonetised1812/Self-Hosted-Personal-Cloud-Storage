package com.example.personal_drive;

import java.time.Instant;

// main purpose is to just hold data
public record FileInfo(
        String name,
        String path,
        String parentFolder,
        boolean directory,
        long size,
        Instant createdAt
) {
}