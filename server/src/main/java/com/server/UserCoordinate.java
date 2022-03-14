package com.server;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class UserCoordinate {

    private String nick;
    private Double latitude;
    private Double longitude;
    private ZonedDateTime timestamp;
    
    public UserCoordinate(String nick, Double latitude, Double longitude, ZonedDateTime timestamp) {
        this.nick = nick;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
    }

    long timestampAsLong() {
        return timestamp.toInstant().toEpochMilli();
    }


    void setTimestamp(long epoch) {
        timestamp = ZonedDateTime.ofInstant(Instant.ofEpochMilli(epoch), ZoneOffset.UTC);
    }

    @Override
    public String toString() {
        return "UserCoordinate [latitude=" + latitude + ", longitude=" + longitude + ", nick=" + nick
                + ", timestampInLocalDateTime=" + timestamp + "]";
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

    

    
  
}
