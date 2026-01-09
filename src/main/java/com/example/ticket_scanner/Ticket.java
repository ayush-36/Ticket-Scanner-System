package com.example.ticket_scanner;

import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "tickets")
public class Ticket {

    @Id
    private String referenceId;
    @Setter
    private boolean scanned;
    private String name;
    private String email;

    public Ticket() {}

    public Ticket(String referenceId, boolean scanned) {
        this.referenceId = referenceId;
        this.scanned = scanned;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setreferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public boolean isScanned() {
        return scanned;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
