package com.oncologia.agenda.controller;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.oncologia.agenda.AppInfo;
import com.oncologia.agenda.model.Appointment;
import com.oncologia.agenda.model.ChemoProtocol;
import com.oncologia.agenda.model.Patient;
import com.oncologia.agenda.model.Role;
import com.oncologia.agenda.model.ScheduleConfig;
import com.oncologia.agenda.model.User;
import com.oncologia.agenda.service.PatientService;
import com.oncologia.agenda.service.ReportService;
import com.oncologia.agenda.service.ScheduleService;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerDateModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.io.File;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SwingMainFrame extends JFrame {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final Color SURFACE = new Color(245, 248, 252);
    private static final Color CARD_BORDER = new Color(218, 226, 238);
    private static final Color PRIMARY = new Color(31, 111, 235);
    private static final Color MUTED = new Color(88, 103, 124);

    private final User currentUser;
    private final PatientService patientService = new PatientService();
    private final ScheduleService scheduleService = new ScheduleService();
    private final ReportService reportService = new ReportService();

    private final JTextField searchField = new JTextField();
    private final PatientTableModel patientModel = new PatientTableModel();
    private final AppointmentTableModel appointmentModel = new AppointmentTableModel();
    private final CalendarTableModel calendarModel = new CalendarTableModel();
    private final JTable patientTable = new JTable(patientModel);
    private final JTable appointmentTable = new JTable(appointmentModel);
    private final JTable calendarTable = new JTable(calendarModel);
    private final JTextArea detailArea = new JTextArea();
    private final JLabel status = new JLabel("Listo");
    private final JSpinner dateSpinner = new JSpinner(new SpinnerDateModel(new Date(), null, null, Calendar.DAY_OF_MONTH));
    private final JComboBox<String> viewMode = new JComboBox<String>(new String[]{"Semanal", "Diaria"});
    private final JComboBox<User> doctorFilter = new JComboBox<User>();
    private boolean darkMode;

    public SwingMainFrame(User currentUser) {
        super(AppInfo.displayName() + " - " + currentUser.getFullName());
        this.currentUser = currentUser;
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1220, 760));
        setLayout(new BorderLayout());
        getContentPane().setBackground(SURFACE);
        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
        status.setBorder(new EmptyBorder(6, 12, 6, 12));
        add(status, BorderLayout.SOUTH);
        configureTables();
        refreshDoctors();
        refreshPatients();
        refreshAgenda();
        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildTopPanel() {
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(SURFACE);
        top.add(buildHeaderPanel(), BorderLayout.NORTH);
        top.add(buildToolbar(), BorderLayout.SOUTH);
        return top;
    }

    private JPanel buildHeaderPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBorder(new EmptyBorder(16, 18, 12, 18));
        header.setBackground(SURFACE);

        JLabel title = new JLabel(AppInfo.displayName());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 24f));
        JLabel subtitle = new JLabel("Gestion simple de pacientes, camas y turnos de quimioterapia");
        subtitle.setForeground(MUTED);

        JPanel titleBox = new JPanel(new BorderLayout(0, 4));
        titleBox.setOpaque(false);
        titleBox.add(title, BorderLayout.NORTH);
        titleBox.add(subtitle, BorderLayout.SOUTH);

        JPanel session = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        session.setOpaque(false);
        JLabel role = new JLabel(currentUser.getRole().getLabel());
        role.setOpaque(true);
        role.setForeground(Color.WHITE);
        role.setBackground(currentUser.getRole() == Role.ENFERMERIA ? PRIMARY : new Color(85, 112, 133));
        role.setBorder(new EmptyBorder(6, 12, 6, 12));
        JLabel user = new JLabel(currentUser.getFullName());
        user.setForeground(MUTED);
        session.add(user);
        session.add(role);

        header.add(titleBox, BorderLayout.WEST);
        header.add(session, BorderLayout.EAST);
        return header;
    }

    private JToolBar buildToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(new MatteBorder(1, 0, 1, 0, CARD_BORDER));
        toolbar.setBackground(Color.WHITE);
        JButton previous = new JButton("<");
        styleToolbarButton(previous);
        previous.setToolTipText("Ir al dia o semana anterior");
        previous.addActionListener(e -> moveDate(-1));
        JButton next = new JButton(">");
        styleToolbarButton(next);
        next.setToolTipText("Ir al dia o semana siguiente");
        next.addActionListener(e -> moveDate(1));
        JButton today = new JButton("Hoy");
        styleToolbarButton(today);
        today.setToolTipText("Volver a la fecha actual");
        today.addActionListener(e -> {
            dateSpinner.setValue(new Date());
            refreshAgenda();
        });

        dateSpinner.setEditor(new JSpinner.DateEditor(dateSpinner, "dd/MM/yyyy"));
        dateSpinner.setToolTipText("Fecha base de la agenda");
        dateSpinner.addChangeListener(e -> refreshAgenda());
        viewMode.setToolTipText("Vista semanal o diaria");
        viewMode.addActionListener(e -> refreshAgenda());
        doctorFilter.setToolTipText("Filtrar turnos por medico");
        doctorFilter.addActionListener(e -> refreshAgenda());

        JButton config = new JButton("Horarios");
        styleToolbarButton(config);
        config.setToolTipText("Configurar horario laboral, bloques y camas/butacas");
        config.setEnabled(currentUser.getRole() == Role.ENFERMERIA);
        config.addActionListener(e -> openConfigDialog());
        JButton pdf = new JButton("PDF semanal");
        styleToolbarButton(pdf);
        pdf.setToolTipText("Exportar agenda semanal a PDF");
        pdf.addActionListener(e -> exportWeeklyPdf());
        JButton reports = new JButton("Reportes");
        styleToolbarButton(reports);
        reports.setToolTipText("Exportar pendientes y ocupacion a PDF");
        reports.addActionListener(e -> exportOperationalReport());
        JButton theme = new JButton("Tema");
        styleToolbarButton(theme);
        theme.setToolTipText("Alternar modo claro/oscuro");
        theme.addActionListener(e -> toggleTheme());
        JButton about = new JButton("Acerca");
        styleToolbarButton(about);
        about.setToolTipText("Ver version y datos de la aplicacion");
        about.addActionListener(e -> showAbout());

        toolbar.add(previous);
        toolbar.add(next);
        toolbar.add(today);
        toolbar.add(dateSpinner);
        toolbar.add(viewMode);
        toolbar.add(new JLabel("  Medico: "));
        toolbar.add(doctorFilter);
        toolbar.addSeparator();
        toolbar.add(config);
        toolbar.add(pdf);
        toolbar.add(reports);
        toolbar.add(theme);
        toolbar.add(about);
        toolbar.addSeparator();
        toolbar.add(Box.createHorizontalGlue());
        return toolbar;
    }

    private Component buildContent() {
        JSplitPane centerRight = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildCalendarPanel(), buildRightPanel());
        centerRight.setResizeWeight(0.73);
        centerRight.setContinuousLayout(true);
        JSplitPane all = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildPatientPanel(), centerRight);
        all.setResizeWeight(0.24);
        all.setContinuousLayout(true);
        all.setBorder(new EmptyBorder(12, 12, 12, 12));
        return all;
    }

    private Component buildPatientPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        decoratePanel(panel, "Pacientes");
        searchField.setToolTipText("Buscar por DNI, nombre o apellido");
        searchField.putClientProperty("JTextField.placeholderText", "Buscar paciente por DNI o nombre");
        searchField.getDocument().addDocumentListener(new SimpleDocumentListener() {
            @Override
            public void update() {
                refreshPatients();
            }
        });
        JPanel buttons = new JPanel(new GridLayout(1, 3, 6, 6));
        JButton add = new JButton("Nuevo");
        JButton edit = new JButton("Editar");
        JButton delete = new JButton("Eliminar");
        stylePrimaryButton(add);
        styleSecondaryButton(edit);
        styleSecondaryButton(delete);
        add.setEnabled(currentUser.getRole() == Role.ENFERMERIA);
        edit.setEnabled(currentUser.getRole() == Role.ENFERMERIA);
        delete.setEnabled(currentUser.getRole() == Role.ENFERMERIA);
        add.addActionListener(e -> editPatient(null));
        edit.addActionListener(e -> {
            Patient selected = selectedPatient();
            if (selected != null) {
                editPatient(selected);
            }
        });
        delete.addActionListener(e -> deletePatient());
        buttons.add(add);
        buttons.add(edit);
        buttons.add(delete);
        panel.add(searchField, BorderLayout.NORTH);
        panel.add(new JScrollPane(patientTable), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private Component buildCalendarPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        decoratePanel(panel, "Agenda");
        panel.add(new JScrollPane(calendarTable), BorderLayout.CENTER);
        return panel;
    }

    private Component buildRightPanel() {
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        decoratePanel(panel, "Detalles y turnos");
        detailArea.setEditable(false);
        detailArea.setLineWrap(true);
        detailArea.setWrapStyleWord(true);
        detailArea.setBorder(new EmptyBorder(8, 8, 8, 8));
        JPanel top = new JPanel(new BorderLayout());
        top.add(new JScrollPane(detailArea), BorderLayout.CENTER);
        top.setPreferredSize(new Dimension(360, 210));
        JPanel buttons = new JPanel(new GridLayout(1, 3, 6, 6));
        JButton create = new JButton("Crear turno");
        JButton reschedule = new JButton("Reprogramar");
        JButton delete = new JButton("Eliminar");
        stylePrimaryButton(create);
        styleSecondaryButton(reschedule);
        styleSecondaryButton(delete);
        delete.setEnabled(currentUser.getRole() == Role.ENFERMERIA);
        create.addActionListener(e -> openAppointmentDialog(null, selectedCalendarStart(), selectedCalendarBed()));
        reschedule.addActionListener(e -> {
            Appointment selected = selectedAppointment();
            if (selected != null) {
                openAppointmentDialog(selected, selected.getStart(), selected.getBedChair());
            }
        });
        delete.addActionListener(e -> deleteAppointment());
        buttons.add(create);
        buttons.add(reschedule);
        buttons.add(delete);
        panel.add(top, BorderLayout.NORTH);
        panel.add(new JScrollPane(appointmentTable), BorderLayout.CENTER);
        panel.add(buttons, BorderLayout.SOUTH);
        return panel;
    }

    private void configureTables() {
        patientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        applyTableStyle(patientTable);
        patientTable.getSelectionModel().addListSelectionListener(e -> showPatientDetail(selectedPatient()));
        appointmentTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        applyTableStyle(appointmentTable);
        appointmentTable.getSelectionModel().addListSelectionListener(e -> showAppointmentDetail(selectedAppointment()));
        calendarTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        applyTableStyle(calendarTable);
        calendarTable.setCellSelectionEnabled(true);
        calendarTable.setRowHeight(72);
        calendarTable.setDefaultRenderer(Object.class, new CalendarRenderer());
        calendarTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Appointment appointment = calendarModel.appointmentAt(calendarTable.getSelectedRow(), calendarTable.getSelectedColumn());
                    if (appointment != null) {
                        openAppointmentDialog(appointment, appointment.getStart(), appointment.getBedChair());
                    } else {
                        openAppointmentDialog(null, selectedCalendarStart(), selectedCalendarBed());
                    }
                }
            }
        });
    }

    private void decoratePanel(JPanel panel, String title) {
        panel.setBackground(Color.WHITE);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER),
                BorderFactory.createCompoundBorder(
                        BorderFactory.createTitledBorder(title),
                        new EmptyBorder(8, 8, 8, 8)
                )
        ));
    }

    private void applyTableStyle(JTable table) {
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(Font.BOLD));
    }

    private void styleToolbarButton(JButton button) {
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setFocusable(false);
    }

    private void stylePrimaryButton(JButton button) {
        button.putClientProperty("JButton.buttonType", "roundRect");
        button.setFont(button.getFont().deriveFont(Font.BOLD));
        button.setBackground(PRIMARY);
        button.setForeground(Color.WHITE);
    }

    private void styleSecondaryButton(JButton button) {
        button.putClientProperty("JButton.buttonType", "roundRect");
    }

    private void refreshDoctors() {
        DefaultComboBoxModel<User> model = new DefaultComboBoxModel<User>();
        model.addElement(new User(0, "todos", "", "Todos", Role.MEDICO));
        for (User doctor : scheduleService.findDoctors()) {
            model.addElement(doctor);
        }
        doctorFilter.setModel(model);
    }

    private void refreshPatients() {
        Patient selected = selectedPatient();
        patientModel.setPatients(patientService.search(searchField.getText()));
        if (selected != null) {
            selectPatientById(selected.getId());
        }
    }

    private void refreshAgenda() {
        LocalDate base = selectedDate();
        LocalDate from = isWeekly() ? base.with(DayOfWeek.MONDAY) : base;
        LocalDate to = isWeekly() ? from.plusDays(6) : base;
        List<Appointment> appointments = scheduleService.appointmentsBetween(from, to);
        User doctor = (User) doctorFilter.getSelectedItem();
        if (doctor != null && doctor.getId() != 0) {
            List<Appointment> filtered = new ArrayList<Appointment>();
            for (Appointment appointment : appointments) {
                if (appointment.getDoctor().getId() == doctor.getId()) {
                    filtered.add(appointment);
                }
            }
            appointments = filtered;
        }
        calendarModel.setData(from, isWeekly(), scheduleService.getConfig(), appointments);
        appointmentModel.setAppointments(appointments);
        status.setText("Agenda actualizada: " + DATE.format(from) + " - " + DATE.format(to)
                + " | " + appointments.size() + " turno(s)");
    }

    private void moveDate(int direction) {
        LocalDate date = selectedDate().plusDays(isWeekly() ? 7L * direction : direction);
        dateSpinner.setValue(Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant()));
    }

    private boolean isWeekly() {
        return "Semanal".equals(viewMode.getSelectedItem());
    }

    private LocalDate selectedDate() {
        Date date = (Date) dateSpinner.getValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    private Patient selectedPatient() {
        int row = patientTable.getSelectedRow();
        return row >= 0 ? patientModel.getPatient(patientTable.convertRowIndexToModel(row)) : null;
    }

    private Appointment selectedAppointment() {
        int row = appointmentTable.getSelectedRow();
        return row >= 0 ? appointmentModel.getAppointment(appointmentTable.convertRowIndexToModel(row)) : null;
    }

    private void selectPatientById(long id) {
        for (int i = 0; i < patientModel.getRowCount(); i++) {
            if (patientModel.getPatient(i).getId() == id) {
                patientTable.setRowSelectionInterval(i, i);
                return;
            }
        }
    }

    private LocalDateTime selectedCalendarStart() {
        LocalDateTime start = calendarModel.startAt(calendarTable.getSelectedRow(), calendarTable.getSelectedColumn());
        return start == null ? selectedDate().atTime(scheduleService.getConfig().getWorkStart()) : start;
    }

    private int selectedCalendarBed() {
        int bed = calendarModel.bedAt(calendarTable.getSelectedColumn());
        return bed < 1 ? 1 : bed;
    }

    private void showPatientDetail(Patient patient) {
        if (patient == null) {
            detailArea.setText("Seleccione un paciente o turno.");
            return;
        }
        detailArea.setText("Paciente: " + patient.getFullName()
                + "\nDNI: " + patient.getDni()
                + "\nObra social: " + safe(patient.getInsurance())
                + "\nDiagnostico: " + safe(patient.getDiagnosis())
                + "\nProtocolo: " + patient.getProtocol().getLabel()
                + "\nAlergias: " + safe(patient.getAllergies())
                + "\nContacto emergencia: " + safe(patient.getEmergencyContact())
                + "\nAlerta clinica: " + patient.getRiskText());
    }

    private void showAppointmentDetail(Appointment appointment) {
        if (appointment == null) {
            return;
        }
        detailArea.setText("Turno: " + appointment.getPatient().getFullName()
                + "\nFecha: " + DATE.format(appointment.getStart().toLocalDate())
                + "\nHora: " + TIME.format(appointment.getStart()) + " a " + TIME.format(appointment.getEnd())
                + "\nLimpieza hasta: " + TIME.format(appointment.getEndWithCleaning())
                + "\nMedico: " + appointment.getDoctor().getFullName()
                + "\nProtocolo: " + appointment.getPatient().getProtocol().getLabel()
                + "\nCama/Butaca: " + appointment.getBedChair()
                + "\nAlerta clinica: " + appointment.getPatient().getRiskText());
    }

    private void editPatient(Patient existing) {
        Patient result = PatientDialog.showDialog(this, existing);
        if (result == null) {
            return;
        }
        try {
            patientService.save(result);
            refreshPatients();
            status.setText("Paciente guardado.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void deletePatient() {
        Patient patient = selectedPatient();
        if (patient == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Eliminar a " + patient.getFullName() + "?", "Confirmar", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            patientService.delete(patient);
            refreshPatients();
            status.setText("Paciente eliminado.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void openAppointmentDialog(Appointment existing, LocalDateTime start, int bed) {
        if (existing != null && currentUser.getRole() != Role.ENFERMERIA && existing.getCreatedBy().getId() != currentUser.getId()) {
            showError("El medico solo puede reprogramar turnos propios.");
            return;
        }
        Appointment result = AppointmentDialog.showDialog(
                this,
                existing,
                selectedPatient(),
                start,
                bed,
                patientService.search(""),
                scheduleService.findDoctors(),
                scheduleService.getConfig(),
                currentUser
        );
        if (result == null) {
            return;
        }
        try {
            if (existing == null) {
                scheduleService.saveAppointment(result, currentUser);
            } else {
                scheduleService.reschedule(result, currentUser);
            }
            refreshAgenda();
            status.setText(existing == null ? "Turno creado." : "Turno reprogramado.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void deleteAppointment() {
        Appointment appointment = selectedAppointment();
        if (appointment == null) {
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Eliminar el turno seleccionado?", "Confirmar", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        try {
            scheduleService.deleteAppointment(appointment, currentUser);
            refreshAgenda();
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void openConfigDialog() {
        ScheduleConfig config = ConfigDialog.showDialog(this, scheduleService.getConfig());
        if (config == null) {
            return;
        }
        try {
            scheduleService.saveConfig(config);
            refreshAgenda();
            status.setText("Configuracion guardada.");
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void exportWeeklyPdf() {
        LocalDate weekStart = selectedDate().with(DayOfWeek.MONDAY);
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("agenda-semanal-" + weekStart + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            reportService.exportWeeklyAgenda(chooser.getSelectedFile().toPath(), weekStart, scheduleService.appointmentsBetween(weekStart, weekStart.plusDays(6)));
            status.setText("PDF generado: " + chooser.getSelectedFile().getAbsolutePath());
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void exportOperationalReport() {
        LocalDate from = LocalDate.now();
        LocalDate to = from.plusDays(7);
        JFileChooser chooser = new JFileChooser();
        chooser.setSelectedFile(new File("reporte-operativo-" + from + ".pdf"));
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            reportService.exportOperationalReport(chooser.getSelectedFile().toPath(), from, to,
                    scheduleService.pendingPatientsNextSevenDays(), scheduleService.dailyOccupancy(from, to));
            status.setText("Reporte generado: " + chooser.getSelectedFile().getAbsolutePath());
        } catch (RuntimeException ex) {
            showError(ex.getMessage());
        }
    }

    private void toggleTheme() {
        try {
            darkMode = !darkMode;
            UIManager.setLookAndFeel(darkMode ? new FlatDarkLaf() : new FlatLightLaf());
            SwingUtilities.updateComponentTreeUI(this);
            status.setText("Tema " + (darkMode ? "oscuro" : "claro") + " aplicado.");
        } catch (Exception ex) {
            showError("No se pudo cambiar el tema.");
        }
    }

    private void showAbout() {
        JOptionPane.showMessageDialog(
                this,
                AppInfo.displayName()
                        + "\nRelease: " + AppInfo.RELEASE_NAME
                        + "\nJava compatible: 8 o superior"
                        + "\nBase de datos: H2 embebida"
                        + "\nInterfaz: Swing + FlatLaf",
                "Acerca de " + AppInfo.NAME,
                JOptionPane.INFORMATION_MESSAGE
        );
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message == null ? "Operacion no disponible" : message, "Atencion", JOptionPane.ERROR_MESSAGE);
    }

    private String safe(String value) {
        return value == null || value.trim().isEmpty() ? "-" : value;
    }

    private static class PatientTableModel extends AbstractTableModel {
        private final String[] columns = {"Paciente", "DNI", "Protocolo", "Alerta"};
        private List<Patient> patients = new ArrayList<Patient>();

        public void setPatients(List<Patient> patients) {
            this.patients = patients;
            fireTableDataChanged();
        }

        public Patient getPatient(int row) {
            return patients.get(row);
        }

        @Override
        public int getRowCount() {
            return patients.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Patient patient = patients.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return patient.getFullName();
                case 1:
                    return patient.getDni();
                case 2:
                    return patient.getProtocol().getLabel();
                case 3:
                    return patient.getRiskText();
                default:
                    return "";
            }
        }
    }

    private static class AppointmentTableModel extends AbstractTableModel {
        private final String[] columns = {"Fecha", "Hora", "Paciente", "Medico", "Cama"};
        private List<Appointment> appointments = new ArrayList<Appointment>();

        public void setAppointments(List<Appointment> appointments) {
            this.appointments = appointments;
            fireTableDataChanged();
        }

        public Appointment getAppointment(int row) {
            return appointments.get(row);
        }

        @Override
        public int getRowCount() {
            return appointments.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            Appointment appointment = appointments.get(rowIndex);
            switch (columnIndex) {
                case 0:
                    return DATE.format(appointment.getStart().toLocalDate());
                case 1:
                    return TIME.format(appointment.getStart());
                case 2:
                    return appointment.getPatient().getFullName();
                case 3:
                    return appointment.getDoctor().getFullName();
                case 4:
                    return appointment.getBedChair();
                default:
                    return "";
            }
        }
    }

    private static class CalendarTableModel extends AbstractTableModel {
        private LocalDate baseDate = LocalDate.now();
        private boolean weekly = true;
        private ScheduleConfig config = new ScheduleConfig();
        private List<Appointment> appointments = new ArrayList<Appointment>();
        private List<LocalTime> slots = new ArrayList<LocalTime>();

        public void setData(LocalDate baseDate, boolean weekly, ScheduleConfig config, List<Appointment> appointments) {
            this.baseDate = baseDate;
            this.weekly = weekly;
            this.config = config;
            this.appointments = appointments;
            this.slots = new ArrayList<LocalTime>();
            for (LocalTime time = config.getWorkStart(); time.isBefore(config.getWorkEnd()); time = time.plusMinutes(config.getSlotMinutes())) {
                slots.add(time);
            }
            fireTableStructureChanged();
        }

        @Override
        public int getRowCount() {
            return slots.size();
        }

        @Override
        public int getColumnCount() {
            return weekly ? 8 : config.getBedCount() + 1;
        }

        @Override
        public String getColumnName(int column) {
            if (column == 0) {
                return "Hora";
            }
            if (weekly) {
                LocalDate date = baseDate.plusDays(column - 1);
                return date.getDayOfWeek().getDisplayName(java.time.format.TextStyle.SHORT, new Locale("es")) + " " + DATE.format(date);
            }
            return "Cama/Butaca " + column;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            if (columnIndex == 0) {
                return TIME.format(slots.get(rowIndex));
            }
            Appointment appointment = appointmentAt(rowIndex, columnIndex);
            if (appointment == null) {
                return "Libre";
            }
            return appointment.getPatient().getFullName() + "\n" + appointment.getPatient().getProtocol().getLabel()
                    + "\n" + appointment.getDoctor().getFullName() + " | Cama " + appointment.getBedChair();
        }

        public Appointment appointmentAt(int row, int column) {
            if (row < 0 || column <= 0 || row >= slots.size()) {
                return null;
            }
            LocalDateTime start = startAt(row, column);
            LocalTime slotEnd = start.toLocalTime().plusMinutes(config.getSlotMinutes());
            for (Appointment appointment : appointments) {
                boolean sameDate = appointment.getStart().toLocalDate().equals(start.toLocalDate());
                boolean inSlot = !appointment.getStart().toLocalTime().isBefore(start.toLocalTime())
                        && appointment.getStart().toLocalTime().isBefore(slotEnd);
                boolean sameBed = weekly || appointment.getBedChair() == bedAt(column);
                if (sameDate && inSlot && sameBed) {
                    return appointment;
                }
            }
            return null;
        }

        public LocalDateTime startAt(int row, int column) {
            if (row < 0 || row >= slots.size()) {
                return null;
            }
            LocalDate date = weekly ? baseDate.plusDays(Math.max(0, column - 1)) : baseDate;
            return date.atTime(slots.get(row));
        }

        public int bedAt(int column) {
            return weekly ? 1 : column;
        }
    }

    private static class CalendarRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(column == 0 ? SwingConstants.CENTER : SwingConstants.LEFT);
            String text = value == null ? "" : value.toString();
            setText("<html>" + text.replace("\n", "<br>") + "</html>");
            if (!isSelected && column > 0) {
                if ("Libre".equals(text)) {
                    component.setBackground(new Color(236, 248, 241));
                } else {
                    component.setBackground(new Color(224, 235, 255));
                }
            }
            return component;
        }
    }

    private abstract static class SimpleDocumentListener implements javax.swing.event.DocumentListener {
        public abstract void update();

        public void insertUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }

        public void removeUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }

        public void changedUpdate(javax.swing.event.DocumentEvent e) {
            update();
        }
    }

    private static class PatientDialog {
        static Patient showDialog(JFrame parent, Patient existing) {
            JTextField firstName = new JTextField(existing == null ? "" : existing.getFirstName(), 24);
            JTextField lastName = new JTextField(existing == null ? "" : existing.getLastName(), 24);
            JTextField dni = new JTextField(existing == null ? "" : existing.getDni(), 24);
            JTextField insurance = new JTextField(existing == null ? "" : empty(existing.getInsurance()), 24);
            JTextArea diagnosis = new JTextArea(existing == null ? "" : empty(existing.getDiagnosis()), 3, 24);
            JComboBox<ChemoProtocol> protocol = new JComboBox<ChemoProtocol>(ChemoProtocol.values());
            protocol.setSelectedItem(existing == null ? ChemoProtocol.OTRO : existing.getProtocol());
            JTextArea allergies = new JTextArea(existing == null ? "" : empty(existing.getAllergies()), 3, 24);
            JTextField emergency = new JTextField(existing == null ? "" : empty(existing.getEmergencyContact()), 24);
            JCheckBox neutropenic = new JCheckBox("Neutropenico", existing != null && existing.isNeutropenic());
            JCheckBox fever = new JCheckBox("Fiebre", existing != null && existing.isFever());
            JPanel form = formPanel();
            addRow(form, 0, "Nombre", firstName);
            addRow(form, 1, "Apellido", lastName);
            addRow(form, 2, "DNI unico", dni);
            addRow(form, 3, "Obra social", insurance);
            addRow(form, 4, "Diagnostico", new JScrollPane(diagnosis));
            addRow(form, 5, "Protocolo", protocol);
            addRow(form, 6, "Alergias", new JScrollPane(allergies));
            addRow(form, 7, "Contacto emergencia", emergency);
            addRow(form, 8, "Alertas", new JPanel(new GridLayout(1, 2)) {{
                add(neutropenic);
                add(fever);
            }});
            int option = JOptionPane.showConfirmDialog(parent, form, existing == null ? "Nuevo paciente" : "Editar paciente", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) {
                return null;
            }
            Patient patient = existing == null ? new Patient() : existing;
            patient.setFirstName(firstName.getText().trim());
            patient.setLastName(lastName.getText().trim());
            patient.setDni(dni.getText().trim());
            patient.setInsurance(insurance.getText().trim());
            patient.setDiagnosis(diagnosis.getText().trim());
            patient.setProtocol((ChemoProtocol) protocol.getSelectedItem());
            patient.setAllergies(allergies.getText().trim());
            patient.setEmergencyContact(emergency.getText().trim());
            patient.setNeutropenic(neutropenic.isSelected());
            patient.setFever(fever.isSelected());
            return patient;
        }
    }

    private static class AppointmentDialog {
        static Appointment showDialog(JFrame parent, Appointment existing, Patient selectedPatient, LocalDateTime defaultStart, int defaultBed,
                                      List<Patient> patients, List<User> doctors, ScheduleConfig config, User currentUser) {
            JComboBox<Patient> patient = new JComboBox<Patient>(patients.toArray(new Patient[patients.size()]));
            JComboBox<User> doctor = new JComboBox<User>(doctors.toArray(new User[doctors.size()]));
            patient.setSelectedItem(existing == null ? selectedPatient : findPatient(patients, existing.getPatient()));
            doctor.setSelectedItem(existing == null ? defaultDoctor(doctors, currentUser) : findUser(doctors, existing.getDoctor()));
            LocalDateTime start = existing == null ? defaultStart : existing.getStart();
            JSpinner date = new JSpinner(new SpinnerDateModel(Date.from(start.atZone(ZoneId.systemDefault()).toInstant()), null, null, Calendar.DAY_OF_MONTH));
            date.setEditor(new JSpinner.DateEditor(date, "dd/MM/yyyy"));
            JSpinner hour = new JSpinner(new SpinnerNumberModel(start.getHour(), 0, 23, 1));
            JSpinner minute = new JSpinner(new SpinnerNumberModel(start.getMinute(), 0, 59, 15));
            JSpinner duration = new JSpinner(new SpinnerNumberModel(existing == null ? defaultDuration((Patient) patient.getSelectedItem()) : existing.getDurationMinutes(), 15, 480, 15));
            JSpinner bed = new JSpinner(new SpinnerNumberModel(existing == null ? defaultBed : existing.getBedChair(), 1, config.getBedCount(), 1));
            JCheckBox automatic = new JCheckBox("Duracion automatica por protocolo", existing == null);
            JLabel warning = new JLabel();
            warning.setForeground(Color.RED);
            patient.addActionListener(e -> {
                Patient selected = (Patient) patient.getSelectedItem();
                if (automatic.isSelected()) {
                    duration.setValue(defaultDuration(selected));
                }
                warning.setText(selected != null && (selected.isFever() || selected.isNeutropenic())
                        ? "Advertencia roja: sugerir reprogramar o validar con el equipo."
                        : "");
            });
            patient.getActionListeners()[0].actionPerformed(null);
            JPanel form = formPanel();
            addRow(form, 0, "Paciente", patient);
            addRow(form, 1, "Medico", doctor);
            addRow(form, 2, "Fecha", date);
            addRow(form, 3, "Hora", new JPanel(new GridLayout(1, 3)) {{
                add(hour);
                add(new JLabel(":"));
                add(minute);
            }});
            addRow(form, 4, "Duracion", duration);
            addRow(form, 5, "Cama/Butaca", bed);
            addRow(form, 6, "", automatic);
            addRow(form, 7, "Alerta", warning);
            int option = JOptionPane.showConfirmDialog(parent, form, existing == null ? "Nuevo turno" : "Reprogramar turno", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) {
                return null;
            }
            Date selectedDate = (Date) date.getValue();
            LocalDate localDate = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            Appointment appointment = existing == null ? new Appointment() : existing;
            appointment.setPatient((Patient) patient.getSelectedItem());
            appointment.setDoctor((User) doctor.getSelectedItem());
            if (appointment.getCreatedBy() == null) {
                appointment.setCreatedBy(currentUser);
            }
            appointment.setStart(localDate.atTime((Integer) hour.getValue(), (Integer) minute.getValue()));
            appointment.setDurationMinutes((Integer) duration.getValue());
            appointment.setBedChair((Integer) bed.getValue());
            return appointment;
        }

        private static int defaultDuration(Patient patient) {
            return patient == null || patient.getProtocol() == null ? 60 : patient.getProtocol().getDefaultDurationMinutes();
        }

        private static User defaultDoctor(List<User> doctors, User currentUser) {
            if (currentUser.getRole() == Role.MEDICO) {
                return currentUser;
            }
            return doctors.isEmpty() ? null : doctors.get(0);
        }

        private static Patient findPatient(List<Patient> patients, Patient target) {
            if (target == null) {
                return null;
            }
            for (Patient patient : patients) {
                if (patient.getId() == target.getId()) {
                    return patient;
                }
            }
            return target;
        }

        private static User findUser(List<User> users, User target) {
            if (target == null) {
                return null;
            }
            for (User user : users) {
                if (user.getId() == target.getId()) {
                    return user;
                }
            }
            return target;
        }
    }

    private static class ConfigDialog {
        static ScheduleConfig showDialog(JFrame parent, ScheduleConfig existing) {
            JSpinner start = new JSpinner(new SpinnerNumberModel(existing.getWorkStart().getHour(), 0, 23, 1));
            JSpinner end = new JSpinner(new SpinnerNumberModel(existing.getWorkEnd().getHour(), 1, 23, 1));
            JComboBox<Integer> slot = new JComboBox<Integer>(new Integer[]{30, 60});
            slot.setSelectedItem(existing.getSlotMinutes());
            JSpinner beds = new JSpinner(new SpinnerNumberModel(existing.getBedCount(), 1, 30, 1));
            JPanel form = formPanel();
            addRow(form, 0, "Inicio jornada", start);
            addRow(form, 1, "Fin jornada", end);
            addRow(form, 2, "Bloque minutos", slot);
            addRow(form, 3, "Camas/butacas", beds);
            int option = JOptionPane.showConfirmDialog(parent, form, "Configuracion de agenda", JOptionPane.OK_CANCEL_OPTION);
            if (option != JOptionPane.OK_OPTION) {
                return null;
            }
            ScheduleConfig config = new ScheduleConfig();
            config.setWorkStart(LocalTime.of((Integer) start.getValue(), 0));
            config.setWorkEnd(LocalTime.of((Integer) end.getValue(), 0));
            config.setSlotMinutes((Integer) slot.getSelectedItem());
            config.setBedCount((Integer) beds.getValue());
            return config;
        }
    }

    private static JPanel formPanel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        return form;
    }

    private static void addRow(JPanel form, int row, String label, Component component) {
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridx = 0;
        gbc.gridy = row;
        form.add(new JLabel(label), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        form.add(component, gbc);
    }

    private static String empty(String value) {
        return value == null ? "" : value;
    }
}
