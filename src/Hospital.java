import java.util.ArrayList;
import java.util.List;

public class Hospital {

    private String hospitalId;
    private String hospitalName;

    private List<Bed> beds;
    private List<Doctor> doctors;

    public Hospital(String hospitalId, String hospitalName) {

        this.hospitalId = hospitalId;
        this.hospitalName = hospitalName;

        beds = new ArrayList<>();
        doctors = new ArrayList<>();
    }

    public void addBed(Bed bed) {
        beds.add(bed);
    }

    public void addDoctor(Doctor doctor) {
        doctors.add(doctor);
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public String getHospitalName() {
        return hospitalName;
    }

    public List<Bed> getBeds() {
        return beds;
    }

    public List<Doctor> getDoctors() {
        return doctors;
    }
}