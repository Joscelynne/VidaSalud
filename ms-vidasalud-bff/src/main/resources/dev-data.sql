INSERT INTO vidasalud_appointments
    (id, patient_name, doctor_name, specialty, date_time, status, room_number, notes)
VALUES
    ('APT-101', 'Carlos Mendoza', 'Dra. María Paz', 'Medicina General', '2026-09-09T17:00:00', 'PENDING', 'Box 204', 'Chequeo preventivo anual'),
    ('APT-102', 'Lorena Silva', 'Dr. Roberto Gómez', 'Cardiología', '2026-09-09T17:30:00', 'CONFIRMED', 'Box 108', 'Electrocardiograma de control'),
    ('APT-103', 'Felipe Araya', 'Dra. Camila Morales', 'Pediatría', '2026-09-10T11:00:00', 'COMPLETED', 'Box 301', 'Control de niño sano')
ON CONFLICT (id) DO NOTHING;

INSERT INTO vidasalud_notifications
    (id, event_id, type, title, message, appointment_id, patient_name, status, is_read)
VALUES
    ('NOTIF-001', 'EVT-001', 'APPOINTMENT_CREATED', 'Nueva Cita Agendada', 'Atención APT-101 agendada para Carlos Mendoza en Medicina General', 'APT-101', 'Carlos Mendoza', 'PENDING', false),
    ('NOTIF-002', 'EVT-002', 'APPOINTMENT_CONFIRMED', 'Cita Confirmada', 'Atención APT-102 confirmada para Lorena Silva en Cardiología', 'APT-102', 'Lorena Silva', 'CONFIRMED', true)
ON CONFLICT (id) DO NOTHING;

