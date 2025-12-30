package com.alkahfprogrammer.warungku.data.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.UUID;

@Entity(tableName = "icons")
public class Icon {
    @PrimaryKey(autoGenerate = true)
    public int id;
    public String uuid;
    public byte[] iconData;
    public String mediaType;
    public String role;

    public Icon(byte[] iconData, String mediaType, String role) {
        this.uuid = UUID.randomUUID().toString();
        this.iconData = iconData;
        this.mediaType = mediaType;
        this.role = role;
    }
}
