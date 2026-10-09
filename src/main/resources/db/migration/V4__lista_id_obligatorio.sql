-- 1. Crear la lista por defecto sólo si todavía no existe.
INSERT INTO listas (nombre)
SELECT 'Sin clasificar'
WHERE NOT EXISTS (SELECT 1 FROM listas WHERE nombre = 'Sin clasificar');

-- 2. Asignar los registros anteriores, conservando sus otros datos.
UPDATE favoritos
SET lista_id = (SELECT MIN(id) FROM listas WHERE nombre = 'Sin clasificar')
WHERE lista_id IS NULL;

-- 3. Recién después exigir una lista para todos los favoritos.
ALTER TABLE favoritos ALTER COLUMN lista_id SET NOT NULL;
