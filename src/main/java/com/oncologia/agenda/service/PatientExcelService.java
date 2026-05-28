package com.oncologia.agenda.service;

import com.oncologia.agenda.model.ChemoProtocol;
import com.oncologia.agenda.model.Patient;
import com.oncologia.agenda.model.User;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class PatientExcelService {
    private static final String[] HEADERS = {
            "Nombre", "Apellido", "CI", "Prestador", "Medico", "Diagnostico",
            "Protocolo", "Alergias", "Contacto emergencia", "Neutropenico", "Fiebre"
    };

    private final PatientService patientService;

    public PatientExcelService(PatientService patientService) {
        this.patientService = patientService;
    }

    public void exportPatients(Path file, List<Patient> patients) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Pacientes");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            int rowIndex = 1;
            for (Patient patient : patients) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(value(patient.getFirstName()));
                row.createCell(1).setCellValue(value(patient.getLastName()));
                row.createCell(2).setCellValue(value(patient.getCi()));
                row.createCell(3).setCellValue(value(patient.getProvider()));
                row.createCell(4).setCellValue(patient.getDoctor() == null ? "" : patient.getDoctor().getFullName());
                row.createCell(5).setCellValue(value(patient.getDiagnosis()));
                row.createCell(6).setCellValue(patient.getProtocol() == null ? ChemoProtocol.OTRO.getLabel() : patient.getProtocol().getLabel());
                row.createCell(7).setCellValue(value(patient.getAllergies()));
                row.createCell(8).setCellValue(value(patient.getEmergencyContact()));
                row.createCell(9).setCellValue(patient.isNeutropenic());
                row.createCell(10).setCellValue(patient.isFever());
            }
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }
            try (OutputStream output = Files.newOutputStream(file)) {
                workbook.write(output);
            }
        } catch (IOException e) {
            throw new ValidationException("No se pudo exportar Excel: " + e.getMessage());
        }
    }

    public int importPatients(Path file, List<User> doctors) {
        int imported = 0;
        try (InputStream input = Files.newInputStream(file); Workbook workbook = new XSSFWorkbook(input)) {
            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isBlank(text(row, 2))) {
                    continue;
                }
                Patient patient = new Patient();
                patient.setFirstName(text(row, 0));
                patient.setLastName(text(row, 1));
                patient.setCi(text(row, 2));
                patient.setProvider(text(row, 3));
                patient.setDoctor(matchDoctor(text(row, 4), doctors));
                patient.setDiagnosis(text(row, 5));
                patient.setProtocol(matchProtocol(text(row, 6)));
                patient.setAllergies(text(row, 7));
                patient.setEmergencyContact(text(row, 8));
                patient.setNeutropenic(bool(row, 9));
                patient.setFever(bool(row, 10));
                patientService.saveOrUpdateByCi(patient);
                imported++;
            }
            return imported;
        } catch (IOException e) {
            throw new ValidationException("No se pudo importar Excel: " + e.getMessage());
        }
    }

    private User matchDoctor(String value, List<User> doctors) {
        if (isBlank(value)) {
            return null;
        }
        for (User doctor : doctors) {
            if (doctor.getFullName().equalsIgnoreCase(value.trim()) || doctor.getUsername().equalsIgnoreCase(value.trim())) {
                return doctor;
            }
        }
        return null;
    }

    private ChemoProtocol matchProtocol(String value) {
        if (isBlank(value)) {
            return ChemoProtocol.OTRO;
        }
        for (ChemoProtocol protocol : ChemoProtocol.values()) {
            if (protocol.name().equalsIgnoreCase(value.trim()) || protocol.getLabel().equalsIgnoreCase(value.trim())) {
                return protocol;
            }
        }
        return ChemoProtocol.OTRO;
    }

    private boolean bool(Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            return false;
        }
        switch (cell.getCellType()) {
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case STRING:
                String text = cell.getStringCellValue();
                return "si".equalsIgnoreCase(text) || "true".equalsIgnoreCase(text) || "x".equalsIgnoreCase(text);
            case NUMERIC:
                return cell.getNumericCellValue() != 0;
            default:
                return false;
        }
    }

    private String text(Row row, int column) {
        Cell cell = row.getCell(column);
        if (cell == null) {
            return "";
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                return "";
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String value(String value) {
        return value == null ? "" : value;
    }
}
