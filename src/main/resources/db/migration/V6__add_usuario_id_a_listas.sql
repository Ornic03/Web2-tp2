-- Mantener listas históricas del TP2; NO modificar V1-V4.
-- Cuenta sin contraseña válida: los datos legacy quedan conservados pero no se
-- pueden consultar desde un registro nuevo (salvo un ADMIN creado por operador).
INSERT INTO usuarios (email, password_hash, rol)
SELECT 'legacy@local.invalid', '!acceso-deshabilitado', 'USER'
WHERE EXISTS (SELECT 1 FROM listas)
ON CONFLICT (email) DO NOTHING;

ALTER TABLE listas ADD COLUMN usuario_id BIGINT;
UPDATE listas
SET usuario_id = (SELECT id FROM usuarios WHERE email = 'legacy@local.invalid')
WHERE usuario_id IS NULL;
ALTER TABLE listas ALTER COLUMN usuario_id SET NOT NULL;
ALTER TABLE listas ADD CONSTRAINT fk_listas_usuario
    FOREIGN KEY (usuario_id) REFERENCES usuarios(id) ON DELETE RESTRICT;
CREATE INDEX idx_listas_usuario_id ON listas(usuario_id);
