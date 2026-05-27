package com.oncologia.agenda.model;

public enum Role {
    ENFERMERIA("Licenciada en Enfermeria"),
    MEDICO("Medico");

    private final String label;

    Role(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
