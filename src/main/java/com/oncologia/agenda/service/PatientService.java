package com.oncologia.agenda.service;

import com.oncologia.agenda.dao.PatientDao;
import com.oncologia.agenda.model.ChemoProtocol;
import com.oncologia.agenda.model.Patient;

import java.util.List;

public class PatientService {
    private final PatientDao patientDao = new PatientDao();

    public List<Patient> search(String query) {
        return patientDao.search(query);
    }

    public Patient save(Patient patient) {
        validate(patient);
        patientDao.findByCi(patient.getCi()).ifPresent(existing -> {
            if (patient.getId() == 0 || existing.getId() != patient.getId()) {
                throw new ValidationException("Ya existe un paciente con esa CI. Use editar sobre el registro existente.");
            }
        });
        return patientDao.save(patient);
    }

    public Patient saveOrUpdateByCi(Patient patient) {
        validate(patient);
        patientDao.findByCi(patient.getCi()).ifPresent(existing -> patient.setId(existing.getId()));
        return patientDao.save(patient);
    }

    public void delete(Patient patient) {
        patientDao.delete(patient.getId());
    }

    private void validate(Patient patient) {
        if (isBlank(patient.getFirstName()) || isBlank(patient.getLastName())) {
            throw new ValidationException("Nombre y apellido son obligatorios.");
        }
        if (isBlank(patient.getDni())) {
            throw new ValidationException("La CI es obligatoria y debe ser unica.");
        }
        if (!CiUyValidator.isValid(patient.getDni())) {
            throw new ValidationException("La CI no es valida. Verifique el digito verificador.");
        }
        patient.setCi(CiUyValidator.clean(patient.getDni()));
        if (patient.getProtocol() == null) {
            patient.setProtocol(ChemoProtocol.OTRO);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
