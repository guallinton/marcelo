package com.oncologia.agenda.service;

import com.oncologia.agenda.model.Appointment;
import com.oncologia.agenda.model.Patient;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;

import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

public class ReportService {
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final PDType1Font FONT = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
    private static final PDType1Font BOLD = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);

    public void exportWeeklyAgenda(Path destination, LocalDate weekStart, List<Appointment> appointments) {
        try (PDDocument document = new PDDocument()) {
            PageWriter writer = new PageWriter(document, "Agenda semanal de quimioterapia");
            writer.line("Semana: " + DATE.format(weekStart) + " al " + DATE.format(weekStart.plusDays(6)));
            writer.blank();
            for (Appointment appointment : appointments) {
                writer.line(String.format("%s %s - %s | %s | Medico: %s | %d min | Cama/Butaca %d",
                        DATE.format(appointment.getStart().toLocalDate()),
                        TIME.format(appointment.getStart()),
                        appointment.getPatient().getFullName(),
                        appointment.getPatient().getProtocol().getLabel(),
                        appointment.getDoctor().getFullName(),
                        appointment.getDurationMinutes(),
                        appointment.getBedChair()
                ));
            }
            writer.close();
            document.save(destination.toFile());
        } catch (IOException e) {
            throw new ValidationException("No se pudo generar el PDF: " + e.getMessage());
        }
    }

    public void exportOperationalReport(Path destination, LocalDate from, LocalDate to, List<Patient> pending, Map<LocalDate, Long> occupancy) {
        try (PDDocument document = new PDDocument()) {
            PageWriter writer = new PageWriter(document, "Reporte operativo");
            writer.line("Periodo: " + DATE.format(from) + " al " + DATE.format(to));
            writer.blank();
            writer.subtitle("Pacientes pendientes de turno en los proximos 7 dias");
            if (pending.isEmpty()) {
                writer.line("Sin pacientes pendientes.");
            } else {
                for (Patient patient : pending) {
                    writer.line(String.format("- %s | CI %s | %s", patient.getFullName(), patient.getCi(), patient.getProtocol().getLabel()));
                }
            }
            writer.blank();
            writer.subtitle("Ocupacion por dia");
            LocalDate cursor = from;
            while (!cursor.isAfter(to)) {
                Long count = occupancy.containsKey(cursor) ? occupancy.get(cursor) : 0L;
                writer.line(String.format("%s: %d turno(s)", DATE.format(cursor), count));
                cursor = cursor.plusDays(1);
            }
            writer.close();
            document.save(destination.toFile());
        } catch (IOException e) {
            throw new ValidationException("No se pudo generar el PDF: " + e.getMessage());
        }
    }

    private static class PageWriter {
        private final PDDocument document;
        private PDPage page;
        private PDPageContentStream content;
        private float y;

        PageWriter(PDDocument document, String title) throws IOException {
            this.document = document;
            newPage();
            content.setFont(BOLD, 16);
            write(title);
            y -= 14;
            content.setFont(FONT, 10);
        }

        void subtitle(String value) throws IOException {
            ensureSpace();
            content.setFont(BOLD, 12);
            write(value);
            content.setFont(FONT, 10);
        }

        void line(String value) throws IOException {
            ensureSpace();
            write(value);
        }

        void blank() {
            y -= 10;
        }

        void close() throws IOException {
            if (content != null) {
                content.close();
            }
        }

        private void newPage() throws IOException {
            if (content != null) {
                content.close();
            }
            page = new PDPage(PDRectangle.A4);
            document.addPage(page);
            content = new PDPageContentStream(document, page);
            y = page.getMediaBox().getHeight() - 45;
        }

        private void ensureSpace() throws IOException {
            if (y < 55) {
                newPage();
                content.setFont(FONT, 10);
            }
        }

        private void write(String value) throws IOException {
            content.beginText();
            content.newLineAtOffset(45, y);
            content.showText(sanitize(value));
            content.endText();
            y -= 15;
        }

        private String sanitize(String value) {
            return value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
        }
    }
}
