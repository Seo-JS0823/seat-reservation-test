CREATE SCHEMA IF NOT EXISTS seat_reservation;

CREATE TABLE seat_reservation.performances (
  performance_id BIGINT NOT NULL,
  title VARCHAR(255) NOT NULL,
  
  CONSTRAINT pk_performance PRIMARY KEY (performance_id)
);

CREATE TABLE seat_reservation.performance_sessions (
  session_id BIGINT NOT NULL,
  performance_id BIGINT NOT NULL,
  start_at TIMESTAMP NOT NULL,
  
  CONSTRAINT pk_performance_session PRIMARY KEY (session_id),
  CONSTRAINT fk_performance
    FOREIGN KEY (performance_id) REFERENCES seat_reservation.performances (performance_id)
);