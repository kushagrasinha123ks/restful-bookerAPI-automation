package com.kushagra.booker.models;

/** A typed JSON payload. Malformed-input tests intentionally use maps or raw JSON instead. */
public record Booking(String firstname, String lastname, int totalprice, boolean depositpaid,
                      BookingDates bookingdates, String additionalneeds) {
    public static Builder builder() { return new Builder(); }

    public static final class Builder {
        private String firstname;
        private String lastname;
        private int totalprice;
        private boolean depositpaid;
        private BookingDates bookingdates;
        private String additionalneeds;

        public Builder firstname(String value) { firstname = value; return this; }
        public Builder lastname(String value) { lastname = value; return this; }
        public Builder totalprice(int value) { totalprice = value; return this; }
        public Builder depositpaid(boolean value) { depositpaid = value; return this; }
        public Builder bookingdates(BookingDates value) { bookingdates = value; return this; }
        public Builder additionalneeds(String value) { additionalneeds = value; return this; }
        public Booking build() { return new Booking(firstname, lastname, totalprice, depositpaid, bookingdates, additionalneeds); }
    }
}
