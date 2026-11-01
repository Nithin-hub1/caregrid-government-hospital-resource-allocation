public class Doctor {

    private String doctorId;
    private String name;
    private String specialization;
    private boolean available;

    public Doctor(String doctorId, String name,
                  String specialization, boolean available) {

        this.doctorId = doctorId;
        this.name = name;
        this.specialization = specialization;
        this.available = available;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public String getName() {
        return name;
    }

    public String getSpecialization() {
        return specialization;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    @Override
    public String toString() {

        String status;

        if (available) {
            status = "AVAILABLE";
        } else {
            status = "ATTENDING";
        }

        return "Doctor ID: " + doctorId +
                " | Name: " + name +
                " | Specialization: " + specialization +
                " | Status: " + status;
    }
}