CREATE TABLE vidasalud_appointments (
    id               VARCHAR(100) PRIMARY KEY,
    patient_name     VARCHAR(200) NOT NULL,
    doctor_name      VARCHAR(200) NOT NULL,
    specialty        VARCHAR(120) NOT NULL,
    date_time        VARCHAR(40) NOT NULL,
    status           VARCHAR(20) NOT NULL,
    room_number      VARCHAR(80),
    notes            TEXT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT vidasalud_appointments_status_check
        CHECK (status IN ('PENDING', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED'))
);

CREATE INDEX vidasalud_appointments_status_idx ON vidasalud_appointments (status);
