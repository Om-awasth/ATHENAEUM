package com.library.model;

/**
 * Domain entity representing a hold/reservation on an unavailable catalog item.
 * [Concept: Encapsulation]
 */
public class Reservation {

    private int id;
    private int itemId;
    private String itemTitle;
    private int personId;
    private String memberName;
    private String memberEmail;
    private String reservationDate;
    private String status; // WAITING, NOTIFIED, FULFILLED, CANCELLED
    private int queuePosition;

    public Reservation() {
    }

    public Reservation(int id, int itemId, String itemTitle, int personId,
                       String memberName, String memberEmail,
                       String reservationDate, String status, int queuePosition) {
        this.id = id;
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.personId = personId;
        this.memberName = memberName;
        this.memberEmail = memberEmail;
        this.reservationDate = reservationDate;
        this.status = status;
        this.queuePosition = queuePosition;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getItemId() {
        return itemId;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public String getItemTitle() {
        return itemTitle;
    }

    public void setItemTitle(String itemTitle) {
        this.itemTitle = itemTitle;
    }

    public int getPersonId() {
        return personId;
    }

    public void setPersonId(int personId) {
        this.personId = personId;
    }

    public String getMemberName() {
        return memberName;
    }

    public void setMemberName(String memberName) {
        this.memberName = memberName;
    }

    public String getMemberEmail() {
        return memberEmail;
    }

    public void setMemberEmail(String memberEmail) {
        this.memberEmail = memberEmail;
    }

    public String getReservationDate() {
        return reservationDate;
    }

    public void setReservationDate(String reservationDate) {
        this.reservationDate = reservationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getQueuePosition() {
        return queuePosition;
    }

    public void setQueuePosition(int queuePosition) {
        this.queuePosition = queuePosition;
    }
}
