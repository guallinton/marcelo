package com.oncologia.agenda.model;

public class Patient {
    private long id;
    private String firstName;
    private String lastName;
    private String dni;
    private String insurance;
    private User doctor;
    private String diagnosis;
    private ChemoProtocol protocol;
    private String allergies;
    private String emergencyContact;
    private byte[] photo;
    private boolean neutropenic;
    private boolean fever;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getDni() {
        return dni;
    }

    public void setDni(String dni) {
        this.dni = dni;
    }

    public String getCi() {
        return dni;
    }

    public void setCi(String ci) {
        this.dni = ci;
    }

    public String getInsurance() {
        return insurance;
    }

    public void setInsurance(String insurance) {
        this.insurance = insurance;
    }

    public String getProvider() {
        return insurance;
    }

    public void setProvider(String provider) {
        this.insurance = provider;
    }

    public User getDoctor() {
        return doctor;
    }

    public void setDoctor(User doctor) {
        this.doctor = doctor;
    }

    public String getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(String diagnosis) {
        this.diagnosis = diagnosis;
    }

    public ChemoProtocol getProtocol() {
        return protocol;
    }

    public void setProtocol(ChemoProtocol protocol) {
        this.protocol = protocol;
    }

    public String getAllergies() {
        return allergies;
    }

    public void setAllergies(String allergies) {
        this.allergies = allergies;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public byte[] getPhoto() {
        return photo;
    }

    public void setPhoto(byte[] photo) {
        this.photo = photo;
    }

    public boolean isNeutropenic() {
        return neutropenic;
    }

    public void setNeutropenic(boolean neutropenic) {
        this.neutropenic = neutropenic;
    }

    public boolean isFever() {
        return fever;
    }

    public void setFever(boolean fever) {
        this.fever = fever;
    }

    public String getFullName() {
        return (nullToEmpty(firstName) + " " + nullToEmpty(lastName)).trim();
    }

    public String getRiskText() {
        if (neutropenic && fever) {
            return "Neutropenico y con fiebre";
        }
        if (neutropenic) {
            return "Neutropenico";
        }
        if (fever) {
            return "Con fiebre";
        }
        return "Sin alertas";
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override
    public String toString() {
        return getFullName() + " - CI " + dni;
    }
}
