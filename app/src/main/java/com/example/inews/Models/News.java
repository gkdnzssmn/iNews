package com.example.inews.Models;

public class News {
    String title, contain, link, date, publisher, user;
    int rating;
    boolean trusted;

    private News(){}

    public News(String title, String contain, String link, String date, String publisher, String user, int rating, boolean trusted){
        this.title = title;
        this.contain = contain;
        this.link = link;
        this.date = date;
        this.publisher = publisher;
        this.user = user;
        this.rating = rating;
        this.trusted = trusted;
    }

    public String getTitle() {
        return title;
    }

    public String getContain() {
        return contain;
    }

    public String getLink() {
        return link;
    }

    public String getDate() {
        return date;
    }

    public String getPublisher() {
        return publisher;
    }

    public String getUser() {
        return user;
    }

    public int getRating() {
        return rating;
    }

    public boolean isTrusted() {
        return trusted;
    }
}
