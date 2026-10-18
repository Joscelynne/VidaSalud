-- Migración aditiva e idempotente: copia registros de appointments a vidasalud_appointments
-- No modifica ni elimina la tabla appointments original
-- El WHERE NOT EXISTS evita duplicados si se ejecuta múltiples veces

INSERT INTO vidasalud_appointments 
    (id, patient_name, doctor_name, specialty, date_time, status, room_number, notes, created_at, updated_at)
SELECT 
    id, 
    patient_name, 
    doctor_name, 
    specialty, 
    date_time, 
    status, 
    room_number, 
    COALESCE(notes, ''),
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
FROM appointments a
WHERE NOT EXISTS (
    SELECT 1 FROM vidasalud_appointments va WHERE va.id = a.id
);
