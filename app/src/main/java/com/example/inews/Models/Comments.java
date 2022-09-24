package com.example.inews.Models;

public class Comments {

    String comment, publisher, date, user;

    private Comments(){}

    public Comments(String comment, String publisher, String date, String user) {
        this.comment = comment;
        this.publisher = publisher;
        this.date = date;
        this.user = user;
    }

    public String getComment() {
        return comment;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getDate() {
        return date;
    }

    public String getUser() {
        return user;
    }
}
