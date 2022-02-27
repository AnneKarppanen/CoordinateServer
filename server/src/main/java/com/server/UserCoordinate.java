package com.server;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class UserCoordinate {

    public String nick;
    public String latitude;
    public String longitude;
    public ZonedDateTime timestamp;
    
    public UserCoordinate(String nick, String latitude, String longitude, ZonedDateTime timestamp) {
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

    public String getLatitude() {
        return latitude;
    }

    public void setLatitude(String latitude) {
        this.latitude = latitude;
    }

    public String getLongitude() {
        return longitude;
    }

    public void setLongitude(String longitude) {
        this.longitude = longitude;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp;
    }

    

    
  
}
