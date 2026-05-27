package com.oncologia.agenda.controller;

import com.oncologia.agenda.model.Appointment;
import com.oncologia.agenda.model.ChemoProtocol;
import com.oncologia.agenda.model.Patient;
import com.oncologia.agenda.model.Role;
import com.oncologia.agenda.model.ScheduleConfig;
import com.oncologia.agenda.model.User;
import com.oncologia.agenda.service.PatientService;
import com.oncologia.agenda.service.ReportService;
import com.oncologia.agenda.service.ScheduleService;
import com.oncologia.agenda.service.ValidationException;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SelectionMode;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.RowConstraints;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.kordamp.ikonli.fontawesome5.FontAwesomeSolid;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainController {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private final User currentUser;
    private final ThemeManager themeManager;
    private final PatientService patientService = new PatientService();
    private final ScheduleService scheduleService = new ScheduleService();
    private final ReportService reportService = new ReportService();

    private final TableView<Patient> patientTable = new TableView<>();
    private final TableView<Appointment> appointmentTable = new TableView<>();
    private final GridPane calendarGrid = new GridPane();
    private final VBox detailBox = new VBox(8);
    private final TextField patientSearch = new TextField();
    private final DatePicker datePicker = new DatePicker(LocalDate.now());
    private final ComboBox<String> viewMode = new ComboBox<>();
    private final ComboBox<User> doctorFilter = new ComboBox<>();
    private final Label status = new Label("Listo");

    private Stage stage;
    private List<Appointment> currentAppointments = new ArrayList<>();

    public MainController(User currentUser, ThemeManager themeManager) {
        this.currentUser = currentUser;
        this.themeManager = themeManager;
    }

    public void show(Stage stage) {
        this.stage = stage;
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-shell");
        root.setTop(buildToolbar());
        root.setCenter(buildContent());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1280, 820);
        themeManager.apply(scene);
        stage.setScene(scene);
        stage.setTitle("Agenda Oncologica - " + currentUser.getFullName() + " (" + currentUser.getRole().getLabel() + ")");
        stage.centerOnScreen();
        stage.show();
        refreshAll();
    }

    private Node buildToolbar() {
        Button previous = iconButton("Anterior", FontAwesomeSolid.CHEVRON_LEFT);
        previous.setTooltip(new Tooltip("Ir al dia o semana anterior"));
        previous.setOnAction(event -> moveDate(-1));

        Button next = iconButton("Siguiente", FontAwesomeSolid.CHEVRON_RIGHT);
        next.setTooltip(new Tooltip("Ir al dia o semana siguiente"));
        next.setOnAction(event -> moveDate(1));

        Button today = new Button("Hoy", new FontIcon(FontAwesomeSolid.CALENDAR_DAY));
        today.setTooltip(new Tooltip("Volver a la fecha actual"));
        today.setOnAction(event -> {
            datePicker.setValue(LocalDate.now());
            refreshAgenda();
        });

        datePicker.setTooltip(new Tooltip("Seleccione fecha base para la agenda"));
        datePicker.setOnAction(event -> refreshAgenda());

        viewMode.setItems(FXCollections.observableArrayList("Semanal", "Diaria"));
        viewMode.setValue("Semanal");
        viewMode.setTooltip(new Tooltip("Cambiar entre vista semanal y diaria"));
        viewMode.setOnAction(event -> refreshAgenda());

        doctorFilter.setTooltip(new Tooltip("Filtrar turnos por medico"));
        doctorFilter.setOnAction(event -> refreshAgenda());

        Button config = new Button("Horarios", new FontIcon(FontAwesomeSolid.COG));
        config.setTooltip(new Tooltip("Configurar horario laboral, bloque y cantidad de camas"));
        config.setDisable(currentUser.getRole() != Role.ENFERMERIA);
        config.setOnAction(event -> openConfigDialog());

        Button exportPdf = new Button("PDF semanal", new FontIcon(FontAwesomeSolid.FILE_PDF));
        exportPdf.setTooltip(new Tooltip("Exportar agenda semanal a PDF"));
        exportPdf.setOnAction(event -> exportWeeklyPdf());

        Button reports = new Button("Reportes", new FontIcon(FontAwesomeSolid.CHART_BAR));
        reports.setTooltip(new Tooltip("Generar reporte de pendientes y ocupacion"));
        reports.setOnAction(event -> exportOperationalReport());

        Button theme = new Button("Tema", new FontIcon(FontAwesomeSolid.ADJUST));
        theme.setTooltip(new Tooltip("Alternar modo claro/oscuro"));
        theme.setOnAction(event -> themeManager.toggle(stage.getScene()));

        Label user = new Label(currentUser.getFullName() + " - " + currentUser.getRole().getLabel());
        user.getStyleClass().add("muted");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox toolbar = new HBox(8, previous, next, today, datePicker, viewMode, new Label("Medico:"), doctorFilter,
                spacer, user, config, exportPdf, reports, theme);
        toolbar.getStyleClass().add("toolbar");
        toolbar.setAlignment(Pos.CENTER_LEFT);
        return toolbar;
    }

    private Node buildContent() {
        SplitPane splitPane = new SplitPane(buildPatientPanel(), buildCalendarPanel(), buildRightPanel());
        splitPane.setDividerPositions(0.22, 0.74);
        return splitPane;
    }

    private Node buildPatientPanel() {
        patientSearch.setPromptText("Buscar por DNI o nombre");
        patientSearch.setTooltip(new Tooltip("Filtra pacientes por nombre, apellido o DNI"));
        patientSearch.textProperty().addListener((obs, old, value) -> refreshPatients());

        patientTable.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        patientTable.getSelectionModel().selectedItemProperty().addListener((obs, old, patient) -> showPatientDetail(patient));

        TableColumn<Patient, String> name = new TableColumn<>("Paciente");
        name.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFullName()));
        name.setPrefWidth(145);

        TableColumn<Patient, String> dni = new TableColumn<>("DNI");
        dni.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDni()));
        dni.setPrefWidth(80);

        TableColumn<Patient, String> protocol = new TableColumn<>("Protocolo");
        protocol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getProtocol().getLabel()));
        protocol.setPrefWidth(120);
        patientTable.getColumns().setAll(name, dni, protocol);

        Button add = new Button("Nuevo", new FontIcon(FontAwesomeSolid.USER_PLUS));
        add.setTooltip(new Tooltip("Crear paciente"));
        add.setDisable(currentUser.getRole() != Role.ENFERMERIA);
        add.setOnAction(event -> openPatientDialog(null));

        Button edit = new Button("Editar", new FontIcon(FontAwesomeSolid.USER_EDIT));
        edit.setTooltip(new Tooltip("Editar paciente seleccionado"));
        edit.setDisable(currentUser.getRole() != Role.ENFERMERIA);
        edit.setOnAction(event -> {
            Patient patient = patientTable.getSelectionModel().getSelectedItem();
            if (patient != null) {
                openPatientDialog(patient);
            }
        });

        Button delete = new Button("Eliminar", new FontIcon(FontAwesomeSolid.TRASH));
        delete.setTooltip(new Tooltip("Eliminar paciente sin turnos asociados"));
        delete.setDisable(currentUser.getRole() != Role.ENFERMERIA);
        delete.setOnAction(event -> deleteSelectedPatient());

        VBox panel = new VBox(10, title("Pacientes"), patientSearch, patientTable, new HBox(8, add, edit, delete));
        panel.getStyleClass().add("panel");
        VBox.setVgrow(patientTable, Priority.ALWAYS);
        return panel;
    }

    private Node buildCalendarPanel() {
        calendarGrid.setGridLinesVisible(false);
        ScrollPane scrollPane = new ScrollPane(calendarGrid);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        VBox panel = new VBox(10, title("Agenda"), scrollPane);
        panel.getStyleClass().add("panel");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        return panel;
    }

    private Node buildRightPanel() {
        configureAppointmentTable();

        Button newAppointment = new Button("Crear turno", new FontIcon(FontAwesomeSolid.PLUS_CIRCLE));
        newAppointment.setTooltip(new Tooltip("Crear turno para el paciente seleccionado"));
        newAppointment.setOnAction(event -> openAppointmentDialog(null, datePicker.getValue().atTime(scheduleService.getConfig().getWorkStart()), 1));

        Button reschedule = new Button("Reprogramar", new FontIcon(FontAwesomeSolid.EXCHANGE_ALT));
        reschedule.setTooltip(new Tooltip("Reprogramar turno seleccionado"));
        reschedule.setOnAction(event -> {
            Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();
            if (appointment != null) {
                openAppointmentDialog(appointment, appointment.getStart(), appointment.getBedChair());
            }
        });

        Button delete = new Button("Eliminar turno", new FontIcon(FontAwesomeSolid.TRASH_ALT));
        delete.setTooltip(new Tooltip("Solo enfermeria puede eliminar turnos"));
        delete.setDisable(currentUser.getRole() != Role.ENFERMERIA);
        delete.setOnAction(event -> deleteSelectedAppointment());

        VBox panel = new VBox(10, title("Detalles"), detailBox, new Separator(), title("Turnos filtrados"), appointmentTable,
                new HBox(8, newAppointment, reschedule, delete));
        panel.getStyleClass().add("panel");
        VBox.setVgrow(appointmentTable, Priority.ALWAYS);
        showPatientDetail(null);
        return panel;
    }

    private void configureAppointmentTable() {
        TableColumn<Appointment, String> date = new TableColumn<>("Fecha");
        date.setCellValueFactory(data -> new SimpleStringProperty(DATE.format(data.getValue().getStart().toLocalDate())));
        date.setPrefWidth(80);

        TableColumn<Appointment, String> hour = new TableColumn<>("Hora");
        hour.setCellValueFactory(data -> new SimpleStringProperty(TIME.format(data.getValue().getStart())));
        hour.setPrefWidth(55);

        TableColumn<Appointment, String> patient = new TableColumn<>("Paciente");
        patient.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getPatient().getFullName()));
        patient.setPrefWidth(130);

        TableColumn<Appointment, String> doctor = new TableColumn<>("Medico");
        doctor.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDoctor().getFullName()));
        doctor.setPrefWidth(100);

        TableColumn<Appointment, String> bed = new TableColumn<>("Cama");
        bed.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getBedChair())));
        bed.setPrefWidth(55);
        appointmentTable.getColumns().setAll(date, hour, patient, doctor, bed);
        appointmentTable.getSelectionModel().selectedItemProperty().addListener((obs, old, appointment) -> showAppointmentDetail(appointment));
    }

    private Node buildStatusBar() {
        HBox box = new HBox(status);
        box.setPadding(new Insets(6, 12, 6, 12));
        return box;
    }

    private Label title(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        return label;
    }

    private Button iconButton(String tooltip, FontAwesomeSolid icon) {
        Button button = new Button("", new FontIcon(icon));
        button.setTooltip(new Tooltip(tooltip));
        return button;
    }

    private void moveDate(int direction) {
        int amount = "Semanal".equals(viewMode.getValue()) ? 7 : 1;
        datePicker.setValue(datePicker.getValue().plusDays((long) direction * amount));
        refreshAgenda();
    }

    private void refreshAll() {
        refreshDoctors();
        refreshPatients();
        refreshAgenda();
    }

    private void refreshDoctors() {
        User all = new User(0, "todos", "", "Todos", Role.MEDICO);
        ObservableList<User> doctors = FXCollections.observableArrayList();
        doctors.add(all);
        doctors.addAll(scheduleService.findDoctors());
        doctorFilter.setItems(doctors);
        doctorFilter.setValue(all);
    }

    private void refreshPatients() {
        Patient selected = patientTable.getSelectionModel().getSelectedItem();
        patientTable.setItems(FXCollections.observableArrayList(patientService.search(patientSearch.getText())));
        if (selected != null) {
            patientTable.getItems().stream()
                    .filter(patient -> patient.getId() == selected.getId())
                    .findFirst()
                    .ifPresent(patient -> patientTable.getSelectionModel().select(patient));
        }
    }

    private void refreshAgenda() {
        LocalDate base = datePicker.getValue() == null ? LocalDate.now() : datePicker.getValue();
        LocalDate from = "Semanal".equals(viewMode.getValue()) ? startOfWeek(base) : base;
        LocalDate to = "Semanal".equals(viewMode.getValue()) ? from.plusDays(6) : base;
        currentAppointments = scheduleService.appointmentsBetween(from, to).stream()
                .filter(this::matchesDoctorFilter)
                .sorted(Comparator.comparing(Appointment::getStart))
                .toList();
        buildCalendar(from);
        appointmentTable.setItems(FXCollections.observableArrayList(currentAppointments));
        status.setText("Agenda actualizada: " + DATE.format(from) + " - " + DATE.format(to));
    }

    private boolean matchesDoctorFilter(Appointment appointment) {
        User filter = doctorFilter.getValue();
        return filter == null || filter.getId() == 0 || appointment.getDoctor().getId() == filter.getId();
    }

    private LocalDate startOfWeek(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    private void buildCalendar(LocalDate from) {
        calendarGrid.getChildren().clear();
        calendarGrid.getColumnConstraints().clear();
        calendarGrid.getRowConstraints().clear();
        ScheduleConfig config = scheduleService.getConfig();
        if ("Semanal".equals(viewMode.getValue())) {
            buildWeeklyCalendar(from, config);
        } else {
            buildDailyCalendar(from, config);
        }
    }

    private void buildWeeklyCalendar(LocalDate weekStart, ScheduleConfig config) {
        addHeader("", 0, 0);
        for (int day = 0; day < 7; day++) {
            LocalDate date = weekStart.plusDays(day);
            addHeader(date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, new Locale("es")) + " " + DATE.format(date), day + 1, 0);
        }
        int row = 1;
        for (LocalTime time = config.getWorkStart(); time.isBefore(config.getWorkEnd()); time = time.plusMinutes(config.getSlotMinutes())) {
            addHeader(TIME.format(time), 0, row);
            for (int day = 0; day < 7; day++) {
                LocalDate date = weekStart.plusDays(day);
                addCalendarCell(date.atTime(time), 0, day + 1, row, appointmentsAt(date, time));
            }
            row++;
        }
        setupGridSizing(8, row);
    }

    private void buildDailyCalendar(LocalDate date, ScheduleConfig config) {
        addHeader("", 0, 0);
        for (int bed = 1; bed <= config.getBedCount(); bed++) {
            addHeader("Cama/Butaca " + bed, bed, 0);
        }
        int row = 1;
        for (LocalTime time = config.getWorkStart(); time.isBefore(config.getWorkEnd()); time = time.plusMinutes(config.getSlotMinutes())) {
            addHeader(TIME.format(time), 0, row);
            for (int bed = 1; bed <= config.getBedCount(); bed++) {
                int selectedBed = bed;
                List<Appointment> appointments = currentAppointments.stream()
                        .filter(a -> a.getBedChair() == selectedBed && a.getStart().toLocalDate().equals(date) && a.getStart().toLocalTime().equals(time))
                        .toList();
                addCalendarCell(date.atTime(time), selectedBed, bed, row, appointments);
            }
            row++;
        }
        setupGridSizing(config.getBedCount() + 1, row);
    }

    private List<Appointment> appointmentsAt(LocalDate date, LocalTime time) {
        return currentAppointments.stream()
                .filter(a -> a.getStart().toLocalDate().equals(date) && a.getStart().toLocalTime().equals(time))
                .toList();
    }

    private void addHeader(String text, int column, int row) {
        Label label = new Label(text);
        label.getStyleClass().add("section-title");
        label.setPadding(new Insets(6));
        label.setMaxWidth(Double.MAX_VALUE);
        calendarGrid.add(label, column, row);
    }

    private void addCalendarCell(LocalDateTime start, int bed, int column, int row, List<Appointment> appointments) {
        VBox cell = new VBox(4);
        cell.getStyleClass().add("calendar-cell");
        cell.setMinHeight(58);
        cell.setMaxWidth(Double.MAX_VALUE);
        if (appointments.isEmpty()) {
            cell.getStyleClass().add("calendar-cell-empty");
            cell.setTooltip(new Tooltip("Crear turno en " + DATE.format(start.toLocalDate()) + " " + TIME.format(start)));
            cell.setOnMouseClicked(event -> openAppointmentDialog(null, start, bed == 0 ? 1 : bed));
        } else {
            for (Appointment appointment : appointments) {
                Label card = new Label("%s\n%s - %d min\nMed: %s | Cama %d".formatted(
                        appointment.getPatient().getFullName(),
                        appointment.getPatient().getProtocol().getLabel(),
                        appointment.getDurationMinutes(),
                        appointment.getDoctor().getFullName(),
                        appointment.getBedChair()
                ));
                card.getStyleClass().add("appointment-card");
                card.setStyle("-fx-background-color: " + appointment.getPatient().getProtocol().getColor() + "; -fx-text-fill: white;");
                card.setTooltip(new Tooltip("Click para ver/reprogramar turno"));
                card.setOnMouseClicked(event -> {
                    appointmentTable.getSelectionModel().select(appointment);
                    showAppointmentDetail(appointment);
                    if (event.getClickCount() == 2) {
                        openAppointmentDialog(appointment, appointment.getStart(), appointment.getBedChair());
                    }
                });
                cell.getChildren().add(card);
            }
        }
        calendarGrid.add(cell, column, row);
    }

    private void setupGridSizing(int columns, int rows) {
        for (int column = 0; column < columns; column++) {
            ColumnConstraints constraints = new ColumnConstraints();
            constraints.setPercentWidth(column == 0 ? 8 : 92.0 / (columns - 1));
            constraints.setHgrow(Priority.ALWAYS);
            calendarGrid.getColumnConstraints().add(constraints);
        }
        for (int row = 0; row < rows; row++) {
            RowConstraints constraints = new RowConstraints();
            constraints.setMinHeight(row == 0 ? 36 : 62);
            calendarGrid.getRowConstraints().add(constraints);
        }
    }

    private void showPatientDetail(Patient patient) {
        detailBox.getChildren().clear();
        if (patient == null) {
            detailBox.getChildren().add(new Label("Seleccione un paciente o turno."));
            return;
        }
        detailBox.getChildren().addAll(
                title(patient.getFullName()),
                new Label("DNI: " + patient.getDni()),
                new Label("Obra social: " + nullSafe(patient.getInsurance())),
                new Label("Diagnostico: " + nullSafe(patient.getDiagnosis())),
                new Label("Protocolo: " + patient.getProtocol().getLabel()),
                new Label("Alergias: " + nullSafe(patient.getAllergies())),
                new Label("Contacto emergencia: " + nullSafe(patient.getEmergencyContact())),
                riskLabel(patient)
        );
    }

    private void showAppointmentDetail(Appointment appointment) {
        if (appointment == null) {
            return;
        }
        detailBox.getChildren().clear();
        detailBox.getChildren().addAll(
                title("Turno de " + appointment.getPatient().getFullName()),
                new Label("Fecha: " + DATE.format(appointment.getStart().toLocalDate())),
                new Label("Hora: " + TIME.format(appointment.getStart()) + " a " + TIME.format(appointment.getEnd())),
                new Label("Limpieza hasta: " + TIME.format(appointment.getEndWithCleaning())),
                new Label("Medico: " + appointment.getDoctor().getFullName()),
                new Label("Protocolo: " + appointment.getPatient().getProtocol().getLabel()),
                new Label("Cama/Butaca: " + appointment.getBedChair()),
                riskLabel(appointment.getPatient())
        );
    }

    private Label riskLabel(Patient patient) {
        Label risk = new Label("Alerta clinica: " + patient.getRiskText());
        if (patient.isFever() || patient.isNeutropenic()) {
            risk.getStyleClass().add("danger");
            risk.setTooltip(new Tooltip("Se recomienda evaluar reprogramacion antes de confirmar el turno."));
        } else {
            risk.getStyleClass().add("muted");
        }
        return risk;
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private void openPatientDialog(Patient existing) {
        PatientDialog dialog = new PatientDialog(stage, existing);
        dialog.showAndWait().ifPresent(patient -> {
            try {
                patientService.save(patient);
                refreshPatients();
                status.setText("Paciente guardado.");
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    private void deleteSelectedPatient() {
        Patient patient = patientTable.getSelectionModel().getSelectedItem();
        if (patient == null || !confirm("Eliminar paciente", "Desea eliminar a " + patient.getFullName() + "?")) {
            return;
        }
        try {
            patientService.delete(patient);
            refreshPatients();
            status.setText("Paciente eliminado.");
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void openAppointmentDialog(Appointment existing, LocalDateTime defaultStart, int defaultBed) {
        if (existing != null && !canModify(existing)) {
            showError("No tiene permisos para modificar este turno.");
            return;
        }
        Patient selectedPatient = patientTable.getSelectionModel().getSelectedItem();
        AppointmentDialog dialog = new AppointmentDialog(stage, existing, selectedPatient, defaultStart, defaultBed,
                patientService.search(""), scheduleService.findDoctors(), scheduleService.getConfig(), currentUser);
        dialog.showAndWait().ifPresent(appointment -> {
            try {
                if (existing == null) {
                    scheduleService.saveAppointment(appointment, currentUser);
                } else {
                    scheduleService.reschedule(appointment, currentUser);
                }
                refreshAgenda();
                status.setText(existing == null ? "Turno creado." : "Turno reprogramado.");
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    private boolean canModify(Appointment appointment) {
        return currentUser.getRole() == Role.ENFERMERIA || appointment.getCreatedBy().getId() == currentUser.getId();
    }

    private void deleteSelectedAppointment() {
        Appointment appointment = appointmentTable.getSelectionModel().getSelectedItem();
        if (appointment == null || !confirm("Eliminar turno", "Desea eliminar el turno seleccionado?")) {
            return;
        }
        try {
            scheduleService.deleteAppointment(appointment, currentUser);
            refreshAgenda();
            status.setText("Turno eliminado.");
        } catch (RuntimeException e) {
            showError(e.getMessage());
        }
    }

    private void openConfigDialog() {
        ConfigDialog dialog = new ConfigDialog(stage, scheduleService.getConfig());
        dialog.showAndWait().ifPresent(config -> {
            try {
                scheduleService.saveConfig(config);
                refreshAgenda();
                status.setText("Configuracion guardada.");
            } catch (RuntimeException e) {
                showError(e.getMessage());
            }
        });
    }

    private void exportWeeklyPdf() {
        LocalDate weekStart = startOfWeek(datePicker.getValue() == null ? LocalDate.now() : datePicker.getValue());
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar agenda semanal");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("agenda-semanal-" + weekStart + ".pdf");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            reportService.exportWeeklyAgenda(file.toPath(), weekStart, scheduleService.appointmentsBetween(weekStart, weekStart.plusDays(6)));
            status.setText("PDF semanal exportado: " + file.getAbsolutePath());
        } catch (ValidationException e) {
            showError(e.getMessage());
        }
    }

    private void exportOperationalReport() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(7);
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar reporte operativo");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("reporte-operativo-" + from + ".pdf");
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }
        try {
            reportService.exportOperationalReport(file.toPath(), from, to,
                    scheduleService.pendingPatientsNextSevenDays(), scheduleService.dailyOccupancy(from, to));
            status.setText("Reporte exportado: " + file.getAbsolutePath());
        } catch (ValidationException e) {
            showError(e.getMessage());
        }
    }

    private boolean confirm(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, message, ButtonType.CANCEL, ButtonType.OK);
        alert.setTitle(title);
        alert.initOwner(stage);
        return alert.showAndWait().filter(ButtonType.OK::equals).isPresent();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message == null ? "Operacion no disponible" : message, ButtonType.OK);
        alert.setTitle("Atencion");
        alert.initOwner(stage);
        alert.showAndWait();
    }

    private static class PatientDialog extends Dialog<Patient> {
        PatientDialog(Stage owner, Patient existing) {
            setTitle(existing == null ? "Nuevo paciente" : "Editar paciente");
            initOwner(owner);
            initModality(Modality.WINDOW_MODAL);

            TextField firstName = new TextField(existing == null ? "" : existing.getFirstName());
            TextField lastName = new TextField(existing == null ? "" : existing.getLastName());
            TextField dni = new TextField(existing == null ? "" : existing.getDni());
            TextField insurance = new TextField(existing == null ? "" : nullToEmpty(existing.getInsurance()));
            TextArea diagnosis = area(existing == null ? "" : nullToEmpty(existing.getDiagnosis()));
            ComboBox<ChemoProtocol> protocol = new ComboBox<>(FXCollections.observableArrayList(ChemoProtocol.values()));
            protocol.setValue(existing == null ? ChemoProtocol.OTRO : existing.getProtocol());
            TextArea allergies = area(existing == null ? "" : nullToEmpty(existing.getAllergies()));
            TextField emergency = new TextField(existing == null ? "" : nullToEmpty(existing.getEmergencyContact()));
            CheckBox neutropenic = new CheckBox("Neutropenico");
            neutropenic.setSelected(existing != null && existing.isNeutropenic());
            CheckBox fever = new CheckBox("Fiebre");
            fever.setSelected(existing != null && existing.isFever());

            GridPane form = formGrid();
            addRow(form, 0, "Nombre", firstName);
            addRow(form, 1, "Apellido", lastName);
            addRow(form, 2, "DNI unico", dni);
            addRow(form, 3, "Obra social", insurance);
            addRow(form, 4, "Diagnostico", diagnosis);
            addRow(form, 5, "Protocolo", protocol);
            addRow(form, 6, "Alergias", allergies);
            addRow(form, 7, "Contacto emergencia", emergency);
            form.add(new Label("Alertas"), 0, 8);
            form.add(new HBox(12, neutropenic, fever), 1, 8);

            getDialogPane().setContent(form);
            getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            setResultConverter(button -> {
                if (button != ButtonType.OK) {
                    return null;
                }
                Patient patient = existing == null ? new Patient() : existing;
                patient.setFirstName(firstName.getText().trim());
                patient.setLastName(lastName.getText().trim());
                patient.setDni(dni.getText().trim());
                patient.setInsurance(insurance.getText().trim());
                patient.setDiagnosis(diagnosis.getText().trim());
                patient.setProtocol(protocol.getValue());
                patient.setAllergies(allergies.getText().trim());
                patient.setEmergencyContact(emergency.getText().trim());
                patient.setNeutropenic(neutropenic.isSelected());
                patient.setFever(fever.isSelected());
                return patient;
            });
        }
    }

    private static class AppointmentDialog extends Dialog<Appointment> {
        AppointmentDialog(Stage owner, Appointment existing, Patient selectedPatient, LocalDateTime defaultStart, int defaultBed,
                          List<Patient> patients, List<User> doctors, ScheduleConfig config, User currentUser) {
            setTitle(existing == null ? "Nuevo turno" : "Reprogramar turno");
            initOwner(owner);
            initModality(Modality.WINDOW_MODAL);

            ComboBox<Patient> patient = new ComboBox<>(FXCollections.observableArrayList(patients));
            patient.setValue(existing != null ? existing.getPatient() : selectedPatient);
            patient.setTooltip(new Tooltip("Seleccione paciente; la duracion puede tomarse del protocolo"));

            ComboBox<User> doctor = new ComboBox<>(FXCollections.observableArrayList(doctors));
            doctor.setValue(existing != null ? existing.getDoctor() : defaultDoctor(doctors, currentUser));

            LocalDateTime start = existing != null ? existing.getStart() : defaultStart;
            DatePicker date = new DatePicker(start.toLocalDate());
            Spinner<Integer> hour = new Spinner<>(0, 23, start.getHour());
            Spinner<Integer> minute = new Spinner<>(0, 59, start.getMinute(), 15);
            Spinner<Integer> duration = new Spinner<>(15, 480, existing != null ? existing.getDurationMinutes() : defaultDuration(patient.getValue()), 15);
            Spinner<Integer> bed = new Spinner<>(1, config.getBedCount(), existing != null ? existing.getBedChair() : defaultBed);

            CheckBox automaticDuration = new CheckBox("Usar duracion del protocolo");
            automaticDuration.setSelected(existing == null);
            automaticDuration.setOnAction(event -> {
                if (automaticDuration.isSelected()) {
                    duration.getValueFactory().setValue(defaultDuration(patient.getValue()));
                }
            });
            patient.valueProperty().addListener((obs, old, value) -> {
                if (automaticDuration.isSelected()) {
                    duration.getValueFactory().setValue(defaultDuration(value));
                }
            });

            Label warning = new Label();
            warning.getStyleClass().add("danger");
            patient.valueProperty().addListener((obs, old, value) -> updateWarning(warning, value));
            updateWarning(warning, patient.getValue());

            GridPane form = formGrid();
            addRow(form, 0, "Paciente", patient);
            addRow(form, 1, "Medico", doctor);
            addRow(form, 2, "Fecha", date);
            addRow(form, 3, "Hora", new HBox(8, hour, new Label(":"), minute));
            addRow(form, 4, "Duracion", new HBox(8, duration, automaticDuration));
            addRow(form, 5, "Cama/Butaca", bed);
            form.add(warning, 0, 6, 2, 1);

            getDialogPane().setContent(form);
            ButtonType save = new ButtonType(existing == null ? "Crear" : "Reprogramar", ButtonBar.ButtonData.OK_DONE);
            getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, save);
            setResultConverter(button -> {
                if (button != save) {
                    return null;
                }
                Appointment appointment = existing == null ? new Appointment() : existing;
                appointment.setPatient(patient.getValue());
                appointment.setDoctor(doctor.getValue());
                if (appointment.getCreatedBy() == null) {
                    appointment.setCreatedBy(currentUser);
                }
                appointment.setStart(LocalDateTime.of(date.getValue(), LocalTime.of(hour.getValue(), minute.getValue())));
                appointment.setDurationMinutes(duration.getValue());
                appointment.setBedChair(bed.getValue());
                return appointment;
            });
        }

        private static User defaultDoctor(List<User> doctors, User currentUser) {
            if (currentUser.getRole() == Role.MEDICO) {
                return currentUser;
            }
            return doctors.isEmpty() ? null : doctors.get(0);
        }

        private static int defaultDuration(Patient patient) {
            return patient == null || patient.getProtocol() == null ? 60 : patient.getProtocol().getDefaultDurationMinutes();
        }

        private static void updateWarning(Label warning, Patient patient) {
            if (patient != null && (patient.isFever() || patient.isNeutropenic())) {
                warning.setText("Advertencia roja: paciente " + patient.getRiskText().toLowerCase(Locale.ROOT) + ". Sugerir reprogramar o validar con el equipo.");
            } else {
                warning.setText("");
            }
        }
    }

    private static class ConfigDialog extends Dialog<ScheduleConfig> {
        ConfigDialog(Stage owner, ScheduleConfig existing) {
            setTitle("Configuracion de agenda");
            initOwner(owner);
            initModality(Modality.WINDOW_MODAL);

            Spinner<Integer> startHour = new Spinner<>(0, 23, existing.getWorkStart().getHour());
            Spinner<Integer> endHour = new Spinner<>(1, 23, existing.getWorkEnd().getHour());
            ComboBox<Integer> slot = new ComboBox<>(FXCollections.observableArrayList(30, 60));
            slot.setValue(existing.getSlotMinutes());
            Spinner<Integer> beds = new Spinner<>(1, 30, existing.getBedCount());

            GridPane form = formGrid();
            addRow(form, 0, "Inicio jornada", new HBox(8, startHour, new Label(":00")));
            addRow(form, 1, "Fin jornada", new HBox(8, endHour, new Label(":00")));
            addRow(form, 2, "Bloque minutos", slot);
            addRow(form, 3, "Camas/butacas", beds);

            getDialogPane().setContent(form);
            getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
            setResultConverter(button -> {
                if (button != ButtonType.OK) {
                    return null;
                }
                ScheduleConfig config = new ScheduleConfig();
                config.setWorkStart(LocalTime.of(startHour.getValue(), 0));
                config.setWorkEnd(LocalTime.of(endHour.getValue(), 0));
                config.setSlotMinutes(slot.getValue());
                config.setBedCount(beds.getValue());
                return config;
            });
        }
    }

    private static GridPane formGrid() {
        GridPane form = new GridPane();
        form.setHgap(10);
        form.setVgap(10);
        form.setPadding(new Insets(12));
        ColumnConstraints labels = new ColumnConstraints();
        labels.setMinWidth(150);
        ColumnConstraints fields = new ColumnConstraints();
        fields.setHgrow(Priority.ALWAYS);
        form.getColumnConstraints().addAll(labels, fields);
        return form;
    }

    private static void addRow(GridPane form, int row, String label, Node field) {
        Label labelNode = new Label(label);
        labelNode.setTooltip(new Tooltip(label));
        form.add(labelNode, 0, row);
        form.add(field, 1, row);
        if (field instanceof Region region) {
            region.setMaxWidth(Double.MAX_VALUE);
        }
    }

    private static TextArea area(String value) {
        TextArea area = new TextArea(value);
        area.setPrefRowCount(2);
        return area;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
