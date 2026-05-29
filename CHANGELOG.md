# Changelog

## 1.9.0 - Protocolos internacionales y casco

- Alta de medicos desde el formulario de paciente (`+ Nuevo medico`).
- Protocolos con referencia NCCN/ESMO/ASCO, tiempo de infusion y ocupacion probable de butaca.
- Checkbox de casco de enfriamiento de cuero cabelludo (scalp cooling).
- Duracion sugerida de turnos segun protocolo del paciente; alertas si usa casco.

## 1.8.0 - Agenda clinica visual

- Se agrega sidebar visual de medicos con busqueda, iniciales, color identificador y filtro directo de agenda.
- Se rediseña la grilla para reducir ruido: los slots disponibles quedan vacios, sin texto repetitivo.
- Se mejoran las cards de turno con paciente, hora, duracion y medico.
- Se agrega borde de color por medico y colores por estado libre/ocupado/continuacion/seleccion.
- Se mejora la lectura operativa de la agenda semanal para administrativos de salud.

## 1.7.0 - Sesion y base local

- Se reemplaza el flujo visible de importacion por carga explicita desde la base local H2.
- Se refuerza la unicidad: solo se permite un paciente por CI, con mensaje claro si ya existe.
- Se agrega boton "Cargar base" en Pacientes.
- Se agrega cierre de sesion y cambio de rol desde la barra principal.
- Se agrega cambio de contrasena persistente en base local.
- Se hace mas visible el rol Medico desde el login y desde la cabecera de sesion.

## 1.6.0 - Persistencia Excel

- Se refuerza el guardado de pacientes en la base local H2 y se muestra estado explicito al guardar.
- Se agrega exportacion de pacientes a Excel `.xlsx`.
- Se agrega importacion de pacientes desde Excel `.xlsx`, actualizando por CI si ya existe.
- Se documenta que la base local se crea en la carpeta `data/` del directorio de ejecucion.

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
