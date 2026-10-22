CREATE TABLE vidasalud_notifications (
    id VARCHAR(100) PRIMARY KEY,
    event_id VARCHAR(100),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    appointment_id VARCHAR(100),
    patient_name VARCHAR(200),
    status VARCHAR(50),
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX vidasalud_notifications_created_at_idx ON vidasalud_notifications (created_at DESC);
