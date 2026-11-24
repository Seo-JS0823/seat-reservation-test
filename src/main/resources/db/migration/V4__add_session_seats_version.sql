ALTER TABLE seat_reservation.session_seats
ADD COLUMN version BIGINT NOT NULL DEFAULT 0;