package com.server;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;

public class UserCoordinate {

    private String nick;
    private Double latitude;
    private Double longitude;
    private ZonedDateTime timestamp;
    private String description;
    private ArrayList<Comment> comments;
    private int coordinateID;

    public UserCoordinate(String nick, Double latitude, Double longitude, ZonedDateTime timestamp, String description) {
        this.nick = nick;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.description = description;
        this.comments = new ArrayList<>();
    }

    public UserCoordinate(String nick, Double latitude, Double longitude, ZonedDateTime timestamp, String description,
            int coordinateID) {
        this.nick = nick;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.description = description;
        this.coordinateID = coordinateID;
        this.comments = new ArrayList<>();
    }

    public UserCoordinate() {

    }

    long timestampAsLong() {
        return timestamp.toInstant().toEpochMilli();
    }

    public ArrayList<Comment> getComments() {
        return comments;
    }

    public void setComments(ArrayList<Comment> comments) {
        this.comments = comments;
    }

    void setTimestamp(long epoch) {
        timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    }

    public void addCommentToCoordinate(Comment comment) {
        this.comments.add(comment);
    }

    @Override
    public String toString() {
        return "UserCoordinate [comments=" + comments + ", coordinateID=" + coordinateID + ", description="
                + description + ", latitude=" + latitude + ", longitude=" + longitude + ", nick=" + nick
                + ", timestamp=" + timestamp + "]";
    }

    public String timestampToString() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX");
        String timestampAsString = timestamp.format(formatter);
        return timestampAsString;
    }

    public String getNick() {
        return nick;
    }

    public void setNick(String nick) {
        this.nick = nick;
    }

    public Double getLatitude() {
        return latitude;
    }

    public int getCoordinateID() {
        return coordinateID;
    }

    public void setCoordinateID(int coordinateID) {
        this.coordinateID = coordinateID;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

}
