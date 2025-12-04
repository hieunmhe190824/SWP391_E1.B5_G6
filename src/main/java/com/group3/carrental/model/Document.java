package com.group3.carrental.model;

import java.io.Serializable;
import java.util.Date;

public class Document implements Serializable {
    private int id;
    private int userId;
    private String type; // CCCD, GPLX, etc.
    private String url;
    private String status; // pending, approved, rejected
    private Date submittedAt;

    public Document() {}

    public Document(int id, int userId, String type, String url,
                    String status, Date submittedAt) {
        this.id = id;
        this.userId = userId;
        this.type = type;
        this.url = url;
        this.status = status;
        this.submittedAt = submittedAt;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(Date submittedAt) { this.submittedAt = submittedAt; }
}
