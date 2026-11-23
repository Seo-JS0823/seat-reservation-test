ALTER TABLE seat_reservation.session_seats
ADD CONSTRAINT uk_session_seat_session_number
UNIQUE (session_id, seat_number);