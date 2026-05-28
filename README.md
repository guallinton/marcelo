# Agenda Quimioterapia

Aplicacion de escritorio Swing/FlatLaf para gestionar pacientes oncologicos y agenda de quimioterapia.

Version actual: **1.6.0 - Persistencia Excel**.

## Requisitos

- Java 17 o superior
- Maven no es obligatorio para desplegar en Windows: el proyecto incluye Maven Wrapper (`mvnw.cmd`).

## Ejecutar en desarrollo

```bash
./mvnw -DskipTests package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency
java -cp "target/agenda-quimioterapia-1.6.0.jar:target/dependency/*" com.oncologia.agenda.AppLauncher
```

Usuarios precargados:

- `enfermera` / `1234` - rol ENFERMERIA
- `drlopez` / `1234` - rol MEDICO

La base H2 embebida se crea automaticamente en `data/` dentro de la carpeta desde la que se ejecuta la app.

## Despliegue Windows solicitado

En Windows, desde la raiz del proyecto, ejecutar:

```bat
scripts\deploy-windows.bat
```

El script usa `mvnw.cmd` automaticamente si esta disponible, por lo que solo necesita Java 17+ instalado.

El script compila la aplicacion y la copia en:

```text
C:\Users\metas.asistenciales\Desktop\cursor\agenda-quimioterapia
```

Luego iniciar con:

```bat
C:\Users\metas.asistenciales\Desktop\cursor\agenda-quimioterapia\AgendaQuimioterapia.exe
```

Tambien se incluye `run-agenda.bat` como alternativa.

## Ejecucion en macOS y Linux

El ZIP portable tambien incluye un lanzador para macOS/Linux:

```bash
./run-agenda.sh
```

Si el sistema no permite ejecutarlo, dar permisos una sola vez:

```bash
chmod +x run-agenda.sh
./run-agenda.sh
```

Para desplegar desde el codigo fuente en macOS/Linux:

```bash
scripts/deploy-unix.sh
```

Por defecto copia la aplicacion a `~/Desktop/cursor/agenda-quimioterapia`. Tambien se puede pasar una carpeta destino:

```bash
scripts/deploy-unix.sh "$HOME/Aplicaciones"
```

## Paquetes portables

- `agenda-quimioterapia-portable.zip`: paquete universal con `AgendaQuimioterapia.exe`, `run-agenda.bat` y `run-agenda.sh`.
- `agenda-quimioterapia-multiplataforma-sin-exe.zip`: paquete universal sin `.exe`, util si la red o el antivirus bloquean ejecutables.

## Funcionalidades principales

- Login simple con roles.
- ABM de pacientes para enfermeria con CI unica validada, prestador, medico, foto, protocolo, alergias y alertas clinicas.
- Agenda semanal y diaria con horario laboral configurable.
- Validaciones de superposicion por cama/butaca, turno duplicado por paciente/dia y 15 minutos de limpieza.
- Reprogramacion segun permisos: enfermeria todos, medico solo propios.
- PDF de agenda semanal y reporte operativo con pendientes y ocupacion.
- Modo claro/oscuro, tooltips, cabecera con version y acciones simples.
- Paquete portable para Windows, macOS y Linux.
- Agenda con bloques ocupados por toda la duracion del turno y paleta turquesa/lila/coral.
- Pacientes persistidos en base local H2 con exportacion/importacion Excel (`.xlsx`).

## Versionado

El proyecto sigue versionado semantico:

- `MAJOR`: cambios incompatibles o de arquitectura.
- `MINOR`: nuevas funcionalidades o mejoras visibles.
- `PATCH`: correcciones puntuales.

Ver `CHANGELOG.md` para el detalle de cambios por version.
