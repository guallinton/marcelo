# Agenda Quimioterapia — Versión web

Aplicación de agenda oncológica en HTML, CSS y JavaScript (sin dependencias de compilación). El foco funcional está en la **visualización de la agenda** semanal y diaria.

## Cómo ejecutarla

1. Abra `web/index.html` en un navegador moderno (Chrome, Firefox, Edge, Safari), **o**
2. Sirva la carpeta con un servidor estático local:

```bash
cd web
python3 -m http.server 8080
```

Luego visite `http://localhost:8080`.

## Usuarios de demostración

| Rol        | Usuario    | Contraseña |
|------------|------------|------------|
| Enfermería | enfermera  | 1234       |
| Médico     | drlopez    | 1234       |

## Funcionalidades

- Agenda **semanal** (7 días desde el lunes de la semana seleccionada) y **diaria** (columnas por cama/butaca).
- Tarjetas de turno con duración real (bloques de 30 min), colores por médico y detección de **conflictos** de cama (incluye 15 min de limpieza).
- Barra lateral de médicos con búsqueda y filtro.
- Panel de detalle, alta/edición/eliminación de turnos (según rol).
- Persistencia en **localStorage** del navegador (`agenda_oncologia_web_v1`).

## Estructura

```
web/
  index.html
  css/app.css
  js/config.js
  js/ci-validator.js
  js/storage.js
  js/agenda.js
  js/app.js
```

## Notas

- Los datos no se sincronizan con la aplicación de escritorio Java; es un entorno web independiente con datos de ejemplo.
- Para reiniciar la demo, borre en las herramientas de desarrollador la clave `agenda_oncologia_web_v1` en localStorage y recargue la página.
