CREATE TABLE seat_reservation.seat_holds (
  hold_id UUID NOT NULL,
  seat_id BIGINT NOT NULL,
  user_id UUID NOT NULL,
  status VARCHAR(20) NOT NULL,
  held_at TIMESTAMP NOT NULL,
  expires_at TIMESTAMP NOT NULL,
  ended_at TIMESTAMP,
  
  CONSTRAINT pk_seat_holds PRIMARY KEY (hold_id),
  CONSTRAINT fk_seat_holds_session_seat
    FOREIGN KEY (seat_id) REFERENCES seat_reservation.session_seats (seat_id)
);

CREATE UNIQUE INDEX uk_seat_holds_live_seat
ON seat_reservation.seat_holds (seat_id)
WHERE status IN ('ACTIVE', 'PAYMENT_PENDING');