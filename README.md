# Agenda Quimioterapia

Aplicacion de escritorio Swing/FlatLaf para gestionar pacientes oncologicos y agenda de quimioterapia.

## Requisitos

- Java 8 o superior
- Maven no es obligatorio para desplegar en Windows: el proyecto incluye Maven Wrapper (`mvnw.cmd`).

## Ejecutar en desarrollo

```bash
./mvnw -DskipTests package dependency:copy-dependencies -DincludeScope=runtime -DoutputDirectory=target/dependency
java -cp "target/agenda-quimioterapia-1.0.0.jar:target/dependency/*" com.oncologia.agenda.AppLauncher
```

Usuarios precargados:

- `enfermera` / `1234` - rol ENFERMERIA
- `drlopez` / `1234` - rol MEDICO

La base H2 embebida se crea automaticamente en `data/`.

## Despliegue Windows solicitado

En Windows, desde la raiz del proyecto, ejecutar:

```bat
scripts\deploy-windows.bat
```

El script usa `mvnw.cmd` automaticamente si esta disponible, por lo que solo necesita Java 8+ instalado.

El script compila la aplicacion y la copia en:

```text
C:\Users\metas.asistenciales\Desktop\cursor\agenda-quimioterapia
```

Luego iniciar con:

```bat
C:\Users\metas.asistenciales\Desktop\cursor\agenda-quimioterapia\AgendaQuimioterapia.exe
```

Tambien se incluye `run-agenda.bat` como alternativa.

## Funcionalidades principales

- Login simple con roles.
- ABM de pacientes para enfermeria con DNI unico, protocolo, alergias y alertas clinicas.
- Agenda semanal y diaria con horario laboral configurable.
- Validaciones de superposicion por cama/butaca, turno duplicado por paciente/dia y 15 minutos de limpieza.
- Reprogramacion segun permisos: enfermeria todos, medico solo propios.
- PDF de agenda semanal y reporte operativo con pendientes y ocupacion.
- Modo claro/oscuro, tooltips e iconos.
