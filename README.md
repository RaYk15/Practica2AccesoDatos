# P2 - Almacén de partidas de LoL con PostgreSQL y SQLite

Continuación de la P1. Los campeones (CSV) y las partidas (JSON) ahora se guardan en PostgreSQL usando JDBC.
Aparte hay un laboratorio pequeño con SQLite para comparar una base embebida con una de servidor.

## Versiones

| Qué | Versión |
|---|---|
| Java | 21 |
| Maven | 3.9 o superior |
| PostgreSQL (Docker) | imagen `postgres:18.6` |
| Driver PostgreSQL | `org.postgresql:postgresql` 42.7.14 |
| Driver SQLite | `org.xerial:sqlite-jdbc` 3.53.4.0 |
| Lectura de archivos | `org.json` 20260814 y `commons-csv` 1.14.1 |

## Puesta en marcha

Todos los comandos se lanzan desde la carpeta del proyecto (en Windows, con PowerShell).

1. Copiar los ejemplos de configuración y poner la misma contraseña en los dos:

```
copy .env.example .env
copy config\bd.example.properties config\bd.properties
```

`.env` lo usa Docker para crear la base y `config/bd.properties` lo usa Java para conectarse.
Ninguno de los dos se sube a Git. La contraseña solo se aplica la primera vez que se crea el contenedor.

2. Arrancar PostgreSQL. Solo escucha en `127.0.0.1:5433`, no se puede entrar desde fuera del equipo:

```
docker compose up -d
```

3. Crear las tablas y el procedimiento. Volver a aplicar `esquema.sql` deja las tablas vacías:

```
$OutputEncoding = [System.Text.UTF8Encoding]::new()
Get-Content .\sql\esquema.sql -Raw | docker exec -i p2-lol-postgres psql -U lol -d lol
Get-Content .\sql\procedimiento.sql -Raw | docker exec -i p2-lol-postgres psql -U lol -d lol
```

`psql -f .\sql\esquema.sql` no funciona porque `psql` se ejecuta dentro del contenedor y ahí no existe ese archivo.

4. Compilar y ejecutar:

```
mvn clean package
java -jar target/p2-lol.jar
```

Sin argumentos se abre el menú. Si las tildes salen mal en la consola, antes hay que poner `chcp 65001`.

## Comandos

También se puede ejecutar un comando y que termine, que es lo que uso para las pruebas.
Los textos con espacios o apóstrofo van entre comillas dobles, por ejemplo `"Kai'Sa"`.

| Comando | Qué hace |
|---|---|
| `conexion` | Comprueba la conexión y enseña la versión de PostgreSQL |
| `campeon ver <id>` | Busca un campeón por su id |
| `campeon listar [rol\|todos] [nombre\|dificultad\|id]` | Lista filtrando por rol, con orden fijo |
| `campeon crear/modificar <id> <nombre> <rol> <region> <dificultad>` | Alta y modificación |
| `campeon borrar <id>` | Borra (falla si el campeón está en alguna partida) |
| `campeon renombrar <id> <nombre>` | Llama al procedimiento `renombrar_campeon` |
| `campeon renombrar-revertir <id> <nombre>` | Llama al procedimiento dentro de una transacción y hace rollback |
| `partida ver <servidor> <idPartida>` | Busca una partida con sus 10 participantes |
| `partida con-campeon <idCampeon>` | Partidas de un campeón (JOIN) |
| `partida recuento` | Cuántas partidas tiene cada campeón |
| `importar [--entrada=CARPETA] [--fallo]` | Importa sin duplicar. `--fallo` provoca un error antes del commit |
| `resumenes` | Resúmenes de las importaciones guardadas |
| `laboratorio crear/insertar/listar/modificar/borrar` | Laboratorio SQLite en `lab/lol.db` |

Al importar, cada registro sale como nuevo, duplicado (igual que el guardado), conflicto (misma clave pero distinto,
no se sobrescribe) o rechazado (no cumple las reglas). Las partidas nuevas y el resumen se guardan en una sola
transacción: o se guarda todo o nada.

## Dónde se comprueba cada regla

Casi todas las reglas están dos veces: en Java, para dar un mensaje claro y clasificar el registro,
y en la base, para que no se pueda guardar nada mal aunque se entre por otro lado (por ejemplo desde `psql`).

| Regla | Java | Base de datos |
|---|---|---|
| Campos obligatorios y no vacíos | Validadores | `NOT NULL` y `CHECK (btrim(...) <> '')` |
| Rol, servidor y ganador permitidos | Validadores | `CHECK (... IN (...))` |
| Rangos (dificultad, duración, kills/deaths/assists) | `Reglas.rango` | `CHECK (... BETWEEN ...)` |
| Tipos (número, fecha) | Validadores | Tipo de la columna |
| Un campeón o una partida no se repite | Importador (duplicado o conflicto) | `PRIMARY KEY` y `UNIQUE (servidor, id_partida)` |
| 5 jugadores por equipo | `ValidadorPartida` (exactamente 5) | Como mucho 5: `PRIMARY KEY (partida_id, equipo, posicion)` y `CHECK (posicion BETWEEN 1 AND 5)` |
| Un campeón solo una vez por partida | `ValidadorPartida` | `UNIQUE (partida_id, campeon_id)` |
| El campeón de un participante existe | `ValidadorPartida` | `FOREIGN KEY` a `campeon` |
| No borrar un campeón que se ha jugado | No | `ON DELETE RESTRICT` |
| Los números del resumen cuadran | `Contador` | `CHECK (procesadas = nuevas + ...)` |
| Renombrar con nombre vacío o id inexistente | No | Procedimiento `renombrar_campeon` |

## Embebido (SQLite) y servidor (PostgreSQL)

| | SQLite | PostgreSQL |
|---|---|---|
| Dónde está | Un archivo (`lab/lol.db`) y el motor va dentro del driver | Un servidor aparte, en Docker |
| Conexión | `jdbc:sqlite:lab/lol.db`, sin usuario ni contraseña | Host, puerto, usuario y contraseña |
| Instalación | Ninguna, solo la dependencia de Maven | Docker, `.env` y aplicar los scripts |
| Varios usuarios a la vez | Pensado para un solo programa | Sí, por red |
| Tipos | Flexibles, por eso uso `STRICT` | Estrictos (`VARCHAR(n)`, `SMALLINT`, `DATE`) |
| Procedimientos almacenados | No tiene | Sí |
| Errores | Mensaje propio, sin SQLState | SQLState estándar (`23505`, `23503`...) |

SQLite me vale para guardar datos de un programa sin montar nada. Para varios usuarios, reglas complejas o
procedimientos, mejor PostgreSQL.

## JDBC directo: ventajas e inconvenientes

Ventajas:
- Escribo el SQL yo, así que sé exactamente qué consulta se lanza.
- Con `PreparedStatement` los datos van como parámetros: un apóstrofo como en `Kai'Sa` no rompe nada y no hay inyección SQL.
- Las transacciones las controlo yo con `setAutoCommit(false)`, `commit()` y `rollback()`.
- Es la misma API para los dos motores, solo cambia la URL.

Inconvenientes:
- Mucho código repetido: pasar cada fila del `ResultSet` a un objeto se hace a mano.
- El SQL va en texto, así que un error en una consulta no se ve al compilar, solo al ejecutar.
- Cada motor da los errores a su manera, y hay que traducirlos (yo lo hago con el SQLState).
- Las conexiones se abren y cierran a mano. Aquí no uso pool porque es un programa pequeño.

## Limitación: commit incierto

Si la conexión se corta justo después de mandar el `commit()` y antes de recibir la respuesta, Java da error
pero no hay forma de saber si el servidor llegó a guardar el lote o no.
En este programa no es grave: si se vuelve a importar, las partidas que sí se guardaron salen como duplicadas
y solo se guardan las que faltan. Lo único que puede quedar es un resumen de más con esas partidas como duplicadas.
