# Changelog

## 1.3.0 - Multiplataforma

- Se agregan lanzadores para macOS/Linux (`run-agenda.sh`) junto al `.exe` y `.bat` de Windows.
- Se agrega script de despliegue Unix (`scripts/deploy-unix.sh`) para copiar la app a `~/Desktop/cursor/agenda-quimioterapia`.
- Se regeneran los paquetes portables con `agenda-quimioterapia-1.3.0.jar`.
- Se mantiene Java 17 como requisito de ejecucion.

## 1.2.0 - Java 17

- Se actualiza la compilacion y ejecucion requerida a Java 17.
- Se actualiza H2 a la linea 2.x para runtime moderno.
- Se usa una base embebida separada `agenda_oncologia_java17` para evitar abrir archivos H2 antiguos incompatibles.
- Se regeneran los paquetes portables para Windows con `agenda-quimioterapia-1.2.0.jar`.

## 1.1.0 - UX sencilla

- Se agrega version visible en login, ventana principal y dialogo "Acerca".
- Se mejora la UI con cabecera moderna, tarjeta de login, paneles con borde suave y botones redondeados.
- Se simplifica la barra superior con acciones principales y estado de sesion.
- Se mejora el estado inferior mostrando rango de agenda y cantidad de turnos.
- Se mantiene compatibilidad con Java 8.

## 1.0.0 - Version inicial

- Login por roles.
- Gestion de pacientes.
- Agenda diaria/semanal.
- Validaciones de turnos, camas y limpieza.
- Reportes PDF.
- Paquete portable para Windows.
