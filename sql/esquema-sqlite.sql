-- Laboratorio embebido: la misma tabla campeon, pero en SQLite (archivo lab/lol.db)
-- Lo aplica el propio programa con "laboratorio crear", no hace falta ningún servidor.
-- IF NOT EXISTS: si el archivo ya tenía la tabla, se queda como estaba (con sus datos).

CREATE TABLE IF NOT EXISTS campeon (
    id_campeon  TEXT     PRIMARY KEY NOT NULL,
    nombre      TEXT     NOT NULL,
    rol         TEXT     NOT NULL,
    region      TEXT     NOT NULL,
    dificultad  INTEGER  NOT NULL,

    CONSTRAINT campeon_id_no_vacio     CHECK (trim(id_campeon) <> ''),
    CONSTRAINT campeon_nombre_no_vacio CHECK (trim(nombre) <> ''),
    CONSTRAINT campeon_region_no_vacia CHECK (trim(region) <> ''),
    CONSTRAINT campeon_rol_valido      CHECK (rol IN ('top', 'jungla', 'mid', 'adc', 'support')),
    CONSTRAINT campeon_dificultad      CHECK (dificultad BETWEEN 1 AND 3)
) STRICT;

-- Diferencias con PostgreSQL:
--   * SQLite no tiene VARCHAR(n) ni SMALLINT de verdad: uso TEXT e INTEGER.
--   * STRICT hace que SQLite compruebe los tipos (sin él, dejaría meter un texto en dificultad).
--   * trim() en vez de btrim().
