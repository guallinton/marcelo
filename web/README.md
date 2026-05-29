# Agenda Quimioterapia — Versión web

Aplicación de agenda oncológica en HTML, CSS y JavaScript. Calendario con [FullCalendar](https://github.com/fullcalendar/fullcalendar).

## Ruta correcta en Windows

La carpeta **`web` no suele estar** en:

```text
C:\Users\metas.asistenciales\Desktop\cursor\web   ← incorrecto (suele no existir)
```

Use una de estas rutas según su caso:

| Situación | Carpeta donde ejecutar |
|-----------|-------------------------|
| Después de `scripts\deploy-windows.bat` | `C:\Users\metas.asistenciales\Desktop\cursor\agenda-quimioterapia\web` |
| Repositorio clonado (código fuente) | `...\marcelo\web` (o el nombre de su carpeta del repo) |

Compruebe que exista el archivo `index.html` en esa carpeta antes de arrancar el servidor.

## Cómo ejecutarla

**Opción A — Servidor local (recomendado, FullCalendar por CDN):**

```bat
cd C:\Users\metas.asistenciales\Desktop\cursor\agenda-quimioterapia\web
python -m http.server 8080
```

Luego: **http://localhost:8080**

**Opción B — Sin servidor:** doble clic en `index.html` (puede fallar la carga de FullCalendar si el navegador bloquea CDN; en ese caso use la opción A).

**Linux / macOS (desde el repo):**

```bash
cd web
python3 -m http.server 8080
```

## Usuarios de demostración

| Rol        | Usuario    | Contraseña |
|------------|------------|------------|
| Enfermería | enfermera  | 1234       |
| Médico     | drlopez    | 1234       |

## Funcionalidades

- Vista semanal y diaria (FullCalendar timeGrid).
- Filtro por médico, detalle de turno, conflictos de cama.
- Persistencia en `localStorage` (`agenda_oncologia_web_v1`).

## Descargar solo la web desde GitHub

Si no tiene el repo en el PC, descargue el ZIP de la rama del proyecto y entre en la carpeta `web` del ZIP extraído.
