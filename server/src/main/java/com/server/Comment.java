package com.server;

import java.time.ZonedDateTime;

public class Comment {

    private int coordinateId;
    private ZonedDateTime timestamp;
    private String content;

    public Comment(int coordinateId, ZonedDateTime timestamp, String content) {
        this.coordinateId = coordinateId;
        this.timestamp = timestamp;
        this.content = content;
    }

    public Comment() {

    }

    long timestampAsLong() {
        return timestamp.toInstant().toEpochMilli();
    }

    public int getCoordinateId() {
        return coordinateId;
    }

    public void setCoordinateId(int coordinateId) {
        this.coordinateId = coordinateId;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
