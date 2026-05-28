# Changelog

## 1.5.0 - Agenda visual

- La grilla de agenda marca como ocupado cada bloque horario cubierto por la duracion del turno.
- Los turnos de 60 minutos o mas se muestran en todos los bloques correspondientes, con continuidad visual.
- Se reemplaza la seleccion gris por tonos diferenciados para libre/ocupado/seleccionado.
- Se armoniza la paleta visual con colores turquesa, lila y coral inspirados en el icono de referencia.

## 1.4.0 - Ficha paciente CI

- Se reemplaza DNI por CI en la interfaz y reportes.
- Se agrega validacion de CI uruguaya con digito verificador.
- Se cambia "Obra social" por "Prestador".
- Se agrega medico asignado a la ficha del paciente.
- Se agrega pestana de foto con imagen generica de persona y carga de JPG/PNG.
- Se elimina el icono de pregunta del modal de paciente.

## 1.3.0 - Multiplataforma

- Se agregan lanzadores para macOS/Linux (`run-agenda.sh`) junto al `.exe` y `.bat` de Windows.
- Se agrega script de despliegue Unix (`scripts/deploy-unix.sh`) para copiar la app a `~/Desktop/cursor/agenda-quimioterapia`.
- Se agrega ZIP alternativo sin `.exe` para macOS/Linux o redes que bloquean ejecutables.
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
