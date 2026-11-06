# Pruebas de aceptación P2

Las pruebas se hacen en orden, empezando con la base vacía (T01), porque cada una parte del estado que deja la anterior.
Todos los comandos se lanzan desde la carpeta del proyecto. Después de cada operación compruebo el resultado con una
lectura nueva: cada comando es una ejecución distinta del programa, así que abre su propia conexión y lee lo que de
verdad hay guardado en la base.

| Prueba | Qué se prueba | Resultado |
|---|---|---|
| T01 | Preparación desde cero y conexión | OK |
| T02 | Fallo de conexión | OK |
| T03 | Primera importación | OK |
| T04 | Repetir la importación sin duplicar | OK |
| T05 | Conflicto: se conserva lo anterior | OK |
| T06 | CRUD de un campeón con apóstrofo | OK |
| T07 | Modificar y borrar algo que no existe (0 filas) | OK |
| T08 | Clave repetida y referencia inválida | OK |
| T09 | Consultas: identidad, listado filtrado, JOIN y recuento | OK |
| T10 | Lote con fallo forzado: rollback | OK |
| T11 | Lote correcto: commit | OK |
| T12 | Procedimiento almacenado | OK |
| Extra | Laboratorio SQLite | OK |

## T01 - Preparación desde cero y conexión

Estado inicial: contenedor arrancado (`docker compose up -d`).

```
$OutputEncoding = [System.Text.UTF8Encoding]::new()
Get-Content .\sql\esquema.sql -Raw | docker exec -i p2-lol-postgres psql -U lol -d lol
Get-Content .\sql\procedimiento.sql -Raw | docker exec -i p2-lol-postgres psql -U lol -d lol
java -jar target/p2-lol.jar conexion
java -jar target/p2-lol.jar campeon listar
java -jar target/p2-lol.jar resumenes
```

Esperado: se crean las 4 tablas y el procedimiento, la conexión funciona y las tablas están vacías.

Obtenido:

```
DROP TABLE
CREATE TABLE
CREATE TABLE
CREATE TABLE
CREATE INDEX
CREATE TABLE
CREATE PROCEDURE

Conectado: PostgreSQL 18.4 on x86_64-pc-linux-gnu, compiled by gcc (Ubuntu 7.5.0-3ubuntu1~18.04) 7.5.0, 64-bit

No hay campeones

No hay resúmenes de importación
```

## T02 - Fallo de conexión

Estado inicial: en `config/bd.properties` pongo una contraseña equivocada (`contrasena=otra_clave_mal`).

```
java -jar target/p2-lol.jar conexion
```

Esperado: un mensaje que se entienda, sin la contraseña y sin la traza de Java.

Obtenido:

```
ERROR: Usuario o contraseña incorrectos para el usuario 'lol'. Revisa config/bd.properties y el .env de Docker.
```

Después vuelvo a poner la contraseña buena.

## T03 - Primera importación

Estado inicial: base vacía (T01).

```
java -jar target/p2-lol.jar importar --entrada=pruebas/casos/base
java -jar target/p2-lol.jar partida ver EUW P001
java -jar target/p2-lol.jar resumenes
```

Esperado: 10 campeones y 2 partidas nuevos. La partida se lee con sus 10 participantes y hay un resumen.

Obtenido:

```
Importando desde pruebas/casos/base
  Campeones: 10 procesados -> 10 nuevos, 0 duplicados, 0 conflictos, 0 rechazados
  Partidas:  2 procesados -> 2 nuevos, 0 duplicados, 0 conflictos, 0 rechazados
Lote confirmado (commit): partidas nuevas y resumen guardados.

EUW/P001 | 2026-10-01 | 23 min | gana rojo
  Equipo azul
    Txema      DAR   4/ 5/ 9   KDA 2.60
    Lucía      NUN   6/ 9/ 5   KDA 1.22
    Íñigo      YAS   0/ 5/ 4   KDA 0.80
    Marta      KAI   1/ 4/ 5   KDA 1.50
    Aitor      THR   6/ 8/ 1   KDA 0.88
  Equipo rojo (ganador)
    Nerea      GAR   7/ 0/18   KDA 25.00
    Unai       LEE  11/ 2/14   KDA 12.50
    Sofía      AHR   5/ 5/12   KDA 3.40
    Jon        JIN   9/ 0/12   KDA 21.00
    Ane        LUX   9/ 2/13   KDA 11.00

Resúmenes de importación de partidas (nuevas/duplicadas/conflictos/rechazadas):
  R1 | 2026-11-06 22:37:48 | 2 procesadas | 2/0/0/0
```

## T04 - Repetir la importación sin duplicar

Estado inicial: el de T03.

```
java -jar target/p2-lol.jar importar --entrada=pruebas/casos/base
java -jar target/p2-lol.jar resumenes
```

Esperado: todo sale como duplicado y no se inserta nada nuevo.

Obtenido:

```
Importando desde pruebas/casos/base
  Campeones: 10 procesados -> 0 nuevos, 10 duplicados, 0 conflictos, 0 rechazados
  Partidas:  2 procesados -> 0 nuevos, 2 duplicados, 0 conflictos, 0 rechazados
Lote confirmado (commit): partidas nuevas y resumen guardados.

Resúmenes de importación de partidas (nuevas/duplicadas/conflictos/rechazadas):
  R1 | 2026-11-06 22:37:48 | 2 procesadas | 2/0/0/0
  R2 | 2026-11-06 22:37:49 | 2 procesadas | 0/2/0/0
```

## T05 - Conflicto: se conserva lo anterior

Estado inicial: el de T04. En `pruebas/casos/conflicto` está AHR con dificultad 3 (en la base tiene 2) y la partida
EUW/P001 con otros datos.

```
java -jar target/p2-lol.jar importar --entrada=pruebas/casos/conflicto
java -jar target/p2-lol.jar campeon ver AHR
java -jar target/p2-lol.jar partida ver EUW P001
```

Esperado: los dos salen como conflicto y en la base se quedan los datos de antes (AHR con dificultad 2 y la partida igual
que en T03).

Obtenido:

```
Importando desde pruebas/casos/conflicto
  Campeones: 1 procesados -> 0 nuevos, 0 duplicados, 1 conflictos, 0 rechazados
  Partidas:  1 procesados -> 0 nuevos, 0 duplicados, 1 conflictos, 0 rechazados
Incidencias:
  campeones.csv, registro 1, AHR, CONFLICTO: ya existe con otro contenido; se conserva el registro anterior
  partidas.json, registro 1, EUW/P001, CONFLICTO: ya existe con otro contenido; se conserva el registro anterior
Lote confirmado (commit): partidas nuevas y resumen guardados.

AHR | Ahri | mid | Jonia | dificultad 2

EUW/P001 | 2026-10-01 | 23 min | gana rojo
  Equipo azul
    Txema      DAR   4/ 5/ 9   KDA 2.60
    (... igual que en T03)
```

## T06 - CRUD de un campeón con apóstrofo

Estado inicial: el de T05. No existe el campeón TST.

```
java -jar target/p2-lol.jar campeon crear TST "Kha'Zix" support Vacío 2
java -jar target/p2-lol.jar campeon ver TST
java -jar target/p2-lol.jar campeon modificar TST "Kha'Zix" support Vacío 3
java -jar target/p2-lol.jar campeon ver TST
java -jar target/p2-lol.jar campeon ver "AHR' OR '1'='1"
java -jar target/p2-lol.jar campeon borrar TST
java -jar target/p2-lol.jar campeon ver TST
```

Esperado: el apóstrofo se guarda y se lee como un dato normal, porque va como parámetro del `PreparedStatement`.
El intento de inyección SQL se trata como un id más y no encuentra nada.

Obtenido:

```
Creado: TST | Kha'Zix | support | Vacío | dificultad 2

TST | Kha'Zix | support | Vacío | dificultad 2

Modificado (1 fila): TST

TST | Kha'Zix | support | Vacío | dificultad 3

No existe el campeón AHR' OR '1'='1

Borrado (1 fila): TST

No existe el campeón TST
```

## T07 - Modificar y borrar algo que no existe

Estado inicial: el de T06. No existe el campeón XXX.

```
java -jar target/p2-lol.jar campeon modificar XXX Nadie mid Jonia 1
java -jar target/p2-lol.jar campeon borrar XXX
```

Esperado: no es un error de la base, simplemente el `UPDATE` y el `DELETE` afectan a 0 filas, y el programa lo dice.

Obtenido:

```
No existe el campeón XXX: no se ha modificado nada (0 filas afectadas)

No existe el campeón XXX: no se ha borrado nada (0 filas afectadas)
```

## T08 - Clave repetida y referencia inválida

Estado inicial: el de T07. AHR existe y aparece en partidas.

```
java -jar target/p2-lol.jar campeon crear AHR Otra mid Jonia 1
java -jar target/p2-lol.jar campeon borrar AHR
java -jar target/p2-lol.jar campeon ver AHR
docker exec -it p2-lol-postgres psql -U lol -d lol -c "UPDATE participante SET campeon_id = 'XYZ' WHERE partida_id = 1 AND equipo = 'azul' AND posicion = 1"
```

Esperado: es la propia base la que lo rechaza. La clave primaria impide repetir AHR y la clave foránea impide borrarlo
o apuntar a un campeón que no existe (lo pruebo directamente con `psql`, porque el programa ya lo filtra antes en Java).
AHR sigue igual.

Obtenido:

```
ERROR: Ya existe el campeón AHR (clave repetida, la base lo rechaza).

ERROR: No se puede guardar o borrar el campeón AHR (aparece en partidas): rompería una referencia entre tablas (clave foránea).

AHR | Ahri | mid | Jonia | dificultad 2

ERROR:  insert or update on table "participante" violates foreign key constraint "participante_campeon_fk"
DETAIL:  Key (campeon_id)=(XYZ) is not present in table "campeon".
```

## T09 - Consultas

Estado inicial: el de T08 (10 campeones y 2 partidas).

```
java -jar target/p2-lol.jar campeon ver AHR
java -jar target/p2-lol.jar campeon listar mid dificultad
java -jar target/p2-lol.jar campeon listar todos "id; DROP TABLE campeon"
java -jar target/p2-lol.jar partida con-campeon AHR
java -jar target/p2-lol.jar partida con-campeon ZZZ
java -jar target/p2-lol.jar partida recuento
java -jar target/p2-lol.jar partida ver EUW P999
```

Esperado: búsqueda por identidad, listado filtrado por rol con orden estable, JOIN de campeón con sus partidas y
recuento. Un orden que no está en la lista blanca se rechaza. Si no hay resultados, sale un mensaje y no un error.

Obtenido:

```
AHR | Ahri | mid | Jonia | dificultad 2

2 campeones con rol mid, ordenados por dificultad:
  AHR | Ahri | mid | Jonia | dificultad 2
  YAS | Yasuo | mid | Jonia | dificultad 3

ERROR: Orden 'id; DROP TABLE campeon' no válido: usa nombre, dificultad o id

2 partidas con AHR:
  EUW/P001 | 2026-10-01 | Sofía (rojo) | 5/5/12 | victoria
  EUW/P002 | 2026-10-02 | Íñigo (azul) | 4/8/6 | derrota

Ninguna partida con ZZZ

Partidas por campeón:
  AHR  Ahri               2
  DAR  Darius             2
  GAR  Garen              2
  JIN  Jinx               2
  KAI  Kai'Sa             2
  LEE  Lee Sin            2
  LUX  Lux                2
  NUN  Nunu y Willump     2
  THR  Thresh             2
  YAS  Yasuo              2

No existe la partida EUW/P999
```

## T10 - Lote con fallo forzado: rollback

Estado inicial: el de T09. Hay 3 resúmenes (R1 a R3, el R3 es de la importación del conflicto en T05).
En `pruebas/casos/loteNuevo` hay dos partidas nuevas, EUW/P003 y EUW/P004.

```
java -jar target/p2-lol.jar importar --entrada=pruebas/casos/loteNuevo --fallo
java -jar target/p2-lol.jar partida ver EUW P003
java -jar target/p2-lol.jar partida ver EUW P004
java -jar target/p2-lol.jar resumenes
```

Esperado: el fallo salta después de insertar la segunda partida y se hace rollback. Desde otra conexión no aparece
ninguna de las dos partidas ni un resumen nuevo, y lo anterior sigue intacto.

Obtenido:

```
Importando desde pruebas/casos/loteNuevo (con fallo provocado antes del commit)
ERROR: No se ha guardado el lote: se ha hecho ROLLBACK, así que no se ha guardado ninguna partida nueva ni su resumen (causa: fallo provocado tras insertar la 2.ª partida nueva)

No existe la partida EUW/P003

No existe la partida EUW/P004

Resúmenes de importación de partidas (nuevas/duplicadas/conflictos/rechazadas):
  R1 | 2026-11-06 22:37:48 | 2 procesadas | 2/0/0/0
  R2 | 2026-11-06 22:37:49 | 2 procesadas | 0/2/0/0
  R3 | 2026-11-06 22:37:50 | 1 procesadas | 0/0/1/0
```

## T11 - Lote correcto: commit

Estado inicial: el de T10 (sin P003 ni P004).

```
java -jar target/p2-lol.jar importar --entrada=pruebas/casos/loteNuevo
java -jar target/p2-lol.jar partida ver EUW P003
java -jar target/p2-lol.jar partida con-campeon AHR
java -jar target/p2-lol.jar resumenes
```

Esperado: las dos partidas y el resumen se guardan juntos con un solo commit, y se ven desde otra conexión.

Obtenido:

```
Importando desde pruebas/casos/loteNuevo
  Campeones: 10 procesados -> 0 nuevos, 10 duplicados, 0 conflictos, 0 rechazados
  Partidas:  2 procesados -> 2 nuevos, 0 duplicados, 0 conflictos, 0 rechazados
Lote confirmado (commit): partidas nuevas y resumen guardados.

EUW/P003 | 2026-10-20 | 23 min | gana rojo
  Equipo azul
    Txema      DAR   4/ 5/ 9   KDA 2.60
    Lucía      NUN   6/ 9/ 5   KDA 1.22
    Íñigo      YAS   0/ 5/ 4   KDA 0.80
    Marta      KAI   1/ 4/ 5   KDA 1.50
    Aitor      THR   6/ 8/ 1   KDA 0.88
  Equipo rojo (ganador)
    Nerea      GAR   7/ 0/18   KDA 25.00
    Unai       LEE  11/ 2/14   KDA 12.50
    Sofía      AHR   5/ 5/12   KDA 3.40
    Jon        JIN   9/ 0/12   KDA 21.00
    Ane        LUX   9/ 2/13   KDA 11.00

4 partidas con AHR:
  EUW/P001 | 2026-10-01 | Sofía (rojo) | 5/5/12 | victoria
  EUW/P002 | 2026-10-02 | Íñigo (azul) | 4/8/6 | derrota
  EUW/P003 | 2026-10-20 | Sofía (rojo) | 5/5/12 | victoria
  EUW/P004 | 2026-10-21 | Íñigo (azul) | 4/8/6 | derrota

Resúmenes de importación de partidas (nuevas/duplicadas/conflictos/rechazadas):
  R1 | 2026-11-06 22:37:48 | 2 procesadas | 2/0/0/0
  R2 | 2026-11-06 22:37:49 | 2 procesadas | 0/2/0/0
  R3 | 2026-11-06 22:37:50 | 1 procesadas | 0/0/1/0
  R4 | 2026-11-06 22:37:58 | 2 procesadas | 2/0/0/0
```

## T12 - Procedimiento almacenado

Estado inicial: el de T11. KAI se llama Kai'Sa.

```
java -jar target/p2-lol.jar campeon renombrar KAI "Kai'Sa, Hija del Vacío"
java -jar target/p2-lol.jar campeon ver KAI
java -jar target/p2-lol.jar campeon renombrar XXX Nadie
java -jar target/p2-lol.jar campeon renombrar KAI "   "
java -jar target/p2-lol.jar campeon renombrar-revertir KAI "Nombre temporal"
java -jar target/p2-lol.jar campeon ver KAI
java -jar target/p2-lol.jar campeon renombrar KAI "Kai'Sa"
```

Esperado: `CALL renombrar_campeon(?, ?)` con parámetros cambia el nombre. Con un id que no existe o un nombre vacío el
procedimiento lanza un error y no cambia nada. Llamado dentro de una transacción que se revierte, el cambio se ve
dentro pero no después. Al final dejo el nombre como estaba.

Obtenido:

```
Procedimiento ejecutado y confirmado. Lectura nueva: KAI | Kai'Sa, Hija del Vacío | adc | Vacío, Runaterra | dificultad 2

KAI | Kai'Sa, Hija del Vacío | adc | Vacío, Runaterra | dificultad 2

ERROR: No existe el campeón XXX: el procedimiento no ha cambiado nada.

ERROR: El procedimiento rechaza el nombre: no puede estar vacío.

  Antes:                                 "Kai'Sa, Hija del Vacío"
  Dentro de la transacción (sin commit): "Nombre temporal"
  Después del rollback (lectura nueva):  "Kai'Sa, Hija del Vacío"

KAI | Kai'Sa, Hija del Vacío | adc | Vacío, Runaterra | dificultad 2

Procedimiento ejecutado y confirmado. Lectura nueva: KAI | Kai'Sa | adc | Vacío, Runaterra | dificultad 2
```

## Extra - Laboratorio SQLite

Estado inicial: no existe la carpeta `lab/`. No hace falta Docker.

```
java -jar target/p2-lol.jar laboratorio listar
java -jar target/p2-lol.jar laboratorio crear
java -jar target/p2-lol.jar laboratorio insertar TST "Kha'Zix" support Vacío 2
java -jar target/p2-lol.jar laboratorio insertar TST Otra mid Jonia 1
java -jar target/p2-lol.jar laboratorio insertar ZZZ Mal mid Jonia 5
java -jar target/p2-lol.jar laboratorio listar
java -jar target/p2-lol.jar laboratorio modificar TST "Kha'Zix" support Vacío 3
java -jar target/p2-lol.jar laboratorio insertar AHR Ahri mid Jonia 2
java -jar target/p2-lol.jar laboratorio borrar TST
java -jar target/p2-lol.jar laboratorio crear
java -jar target/p2-lol.jar laboratorio listar
```

Esperado: se crea el archivo, se inserta, se consulta, se modifica y se borra. SQLite también rechaza la clave repetida
y la dificultad fuera de rango. Al volver a abrir el archivo en otra ejecución, los datos siguen ahí.

Obtenido:

```
ERROR: no existe lab/lol.db. Ejecuta antes 'laboratorio crear'.

Creado el archivo lab/lol.db con la tabla campeon.
Campeones en la tabla: 0

Insertado: TST

ERROR SQLite: [SQLITE_CONSTRAINT_PRIMARYKEY] A PRIMARY KEY constraint failed (UNIQUE constraint failed: campeon.id_campeon)

ERROR SQLite: [SQLITE_CONSTRAINT_CHECK] A CHECK constraint failed (CHECK constraint failed: campeon_dificultad)

Campeones en lab/lol.db:
  TST | Kha'Zix | support | Vacío | dificultad 2

Modificado (1 fila): TST

Insertado: AHR

Borrado (1 fila): TST

El archivo lab/lol.db ya existía: la tabla se queda como estaba.
Campeones en la tabla: 1

Campeones en lab/lol.db:
  AHR | Ahri | mid | Jonia | dificultad 2
```
