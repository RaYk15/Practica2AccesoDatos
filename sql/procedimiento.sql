-- Procedimiento almacenado renombrar_campeon
--
-- Aplicar después de aplicar el esquema.sql
--
-- Cambia el nombre visible de un campeón. Su identidad (id_campeon) no cambia.
-- NO hace COMMIT, eso lo hace la transacción

CREATE OR REPLACE PROCEDURE renombrar_campeon(p_id VARCHAR, p_nombre VARCHAR)
    LANGUAGE plpgsql
AS $$
BEGIN
    IF p_nombre IS NULL OR btrim(p_nombre) = '' THEN
        RAISE EXCEPTION 'El nombre nuevo no puede estar vacío'
            USING ERRCODE = '22023';                 -- invalid_parameter_value
    END IF;

    UPDATE campeon
    SET nombre = btrim(p_nombre)
    WHERE id_campeon = p_id;

    IF NOT FOUND THEN                                -- el UPDATE no afectó a ninguna fila
        RAISE EXCEPTION 'No existe el campeón %', p_id
            USING ERRCODE = 'P0002';                 -- no_data_found
    END IF;
END;
$$;
