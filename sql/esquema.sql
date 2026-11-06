-- Almacén de partidas del lol
-- Aplicar sobre la base "lol"
-- Volver a aplicarlo deja las tablas VACÍAS: es la preparación desde cero.

DROP TABLE IF EXISTS participante, partida, resumen_importacion, campeon CASCADE;

CREATE TABLE campeon (
                         id_campeon  VARCHAR(10)  PRIMARY KEY,
                         nombre      VARCHAR(60)  NOT NULL,
                         rol         VARCHAR(10)  NOT NULL,
                         region      VARCHAR(60)  NOT NULL,
                         dificultad  SMALLINT     NOT NULL,

                         CONSTRAINT campeon_id_no_vacio     CHECK (btrim(id_campeon) <> ''),
                         CONSTRAINT campeon_nombre_no_vacio CHECK (btrim(nombre) <> ''),
                         CONSTRAINT campeon_region_no_vacia CHECK (btrim(region) <> ''),
                         CONSTRAINT campeon_rol_valido      CHECK (rol IN ('top', 'jungla', 'mid', 'adc', 'support')),
                         CONSTRAINT campeon_dificultad      CHECK (dificultad BETWEEN 1 AND 3)
);

CREATE TABLE partida (
                         id            INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                         servidor      VARCHAR(10)  NOT NULL,
                         id_partida    VARCHAR(20)  NOT NULL,
                         fecha         DATE         NOT NULL,
                         duracion_min  SMALLINT     NOT NULL,
                         ganador       VARCHAR(10)  NOT NULL,

                         CONSTRAINT partida_identidad          UNIQUE (servidor, id_partida),
                         CONSTRAINT partida_servidor_valido    CHECK (servidor IN ('EUW', 'EUNE', 'NA', 'KR')),
                         CONSTRAINT partida_id_no_vacio        CHECK (btrim(id_partida) <> ''),
                         CONSTRAINT partida_duracion           CHECK (duracion_min BETWEEN 1 AND 120),
                         CONSTRAINT partida_ganador_valido     CHECK (ganador IN ('azul', 'rojo'))
);

CREATE TABLE participante (
                              partida_id   INTEGER      NOT NULL,
                              equipo       VARCHAR(10)  NOT NULL,
                              posicion     SMALLINT     NOT NULL,
                              jugador      VARCHAR(30)  NOT NULL,
                              campeon_id   VARCHAR(10)  NOT NULL,
                              kills        SMALLINT     NOT NULL,
                              deaths       SMALLINT     NOT NULL,
                              assists      SMALLINT     NOT NULL,

                              CONSTRAINT participante_pk PRIMARY KEY (partida_id, equipo, posicion),

    -- Borrar una partida borra sus participantes.
                              CONSTRAINT participante_partida_fk FOREIGN KEY (partida_id)
                                  REFERENCES partida (id) ON DELETE CASCADE,

    -- No se puede borrar un campeón que aparece en partidas.
                              CONSTRAINT participante_campeon_fk FOREIGN KEY (campeon_id)
                                  REFERENCES campeon (id_campeon) ON DELETE RESTRICT,

    -- Un campeón no se repite en la misma partida.
                              CONSTRAINT participante_campeon_unico UNIQUE (partida_id, campeon_id),

                              CONSTRAINT participante_equipo_valido CHECK (equipo IN ('azul', 'rojo')),
                              CONSTRAINT participante_posicion      CHECK (posicion BETWEEN 1 AND 5),
                              CONSTRAINT participante_jugador       CHECK (btrim(jugador) <> ''),
                              CONSTRAINT participante_kills         CHECK (kills   BETWEEN 0 AND 99),
                              CONSTRAINT participante_deaths        CHECK (deaths  BETWEEN 0 AND 99),
                              CONSTRAINT participante_assists       CHECK (assists BETWEEN 0 AND 99)
);

-- Para la consulta "partidas en las que se jugó un campeón"
CREATE INDEX participante_por_campeon ON participante (campeon_id);

-- Resumen de cada importación de partidas.
CREATE TABLE resumen_importacion (
                                     id          INTEGER      GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                     momento     TIMESTAMPTZ  NOT NULL DEFAULT now(),
                                     procesadas  INTEGER      NOT NULL,
                                     nuevas      INTEGER      NOT NULL,
                                     duplicadas  INTEGER      NOT NULL,
                                     conflictos  INTEGER      NOT NULL,
                                     rechazadas  INTEGER      NOT NULL,

                                     CONSTRAINT resumen_no_negativos CHECK (
                                         procesadas >= 0 AND nuevas >= 0 AND duplicadas >= 0 AND conflictos >= 0 AND rechazadas >= 0),
                                     CONSTRAINT resumen_cuadra CHECK (
                                         procesadas = nuevas + duplicadas + conflictos + rechazadas)
);