CREATE TABLE seat_reservation.session_seats (
  seat_id BIGINT NOT NULL,
  session_id BIGINT NOT NULL,
  seat_number INTEGER NOT NULL,
  grade VARCHAR(20) NOT NULL,
  price BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL,
  current_hold_id UUID,
  current_reservation_id UUID,
  
  CONSTRAINT pk_session_seat PRIMARY KEY (seat_id),
  CONSTRAINT fk_session_seat_session
    FOREIGN KEY (session_id) REFERENCES seat_reservation.performance_sessions (session_id),
  CONSTRAINT ck_session_seat_price CHECK (price > 0)
);