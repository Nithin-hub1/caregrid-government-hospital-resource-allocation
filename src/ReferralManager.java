import java.util.*;

public class ReferralManager {

    private HospitalNetwork network;

    // Information about the latest selected referral
    private int lastTravelTime = -1;
    private List<String> lastRoute = new ArrayList<>();

    public ReferralManager(HospitalNetwork network) {

        this.network = network;
    }

    // Find the best reachable hospital with required resources
    public Hospital findReferralHospital(
            Patient patient,
            Hospital primaryHospital,
            Hospital[] referralHospitals) {

        // Reset previous referral information
        lastTravelTime = -1;
        lastRoute = new ArrayList<>();

        Map<String, Integer> distances =
                network.dijkstra(primaryHospital.getHospitalName());

        Hospital selectedHospital = null;
        int shortestDistance = Integer.MAX_VALUE;

        for (Hospital hospital : referralHospitals) {

            String hospitalName =
                    hospital.getHospitalName();

            // Check whether hospital is reachable
            if (!distances.containsKey(hospitalName)) {
                continue;
            }

            int distance = distances.get(hospitalName);

            if (distance == Integer.MAX_VALUE) {
                continue;
            }

            // Check required bed
            ResourceManager resourceManager =
                    new ResourceManager(hospital);

            boolean bedAvailable =
                    resourceManager.isBedAvailable(
                            patient.getRequiredFacility()
                    );

            // Check required doctor
            boolean doctorAvailable =
                    resourceManager.isDoctorAvailable(
                            patient.getRequiredFacility()
                    );

            // Hospital is eligible only if BOTH are available
            if (bedAvailable && doctorAvailable) {

                if (distance < shortestDistance) {

                    shortestDistance = distance;
                    selectedHospital = hospital;
                }
            }
        }

        // If a hospital was selected
        if (selectedHospital != null) {

            lastTravelTime = shortestDistance;

            lastRoute =
                    network.getShortestPath(
                            primaryHospital.getHospitalName(),
                            selectedHospital.getHospitalName()
                    );
        }

        return selectedHospital;
    }

    // Check whether any referral hospital has a suitable doctor
    public boolean isAnyReferralDoctorAvailable(
            Patient patient,
            Hospital[] referralHospitals) {

        for (Hospital hospital : referralHospitals) {

            ResourceManager resourceManager =
                    new ResourceManager(hospital);

            if (resourceManager.isDoctorAvailable(
                    patient.getRequiredFacility())) {

                return true;
            }
        }

        return false;
    }

    // Get travel time of latest referral
    public int getLastTravelTime() {

        return lastTravelTime;
    }

    // Get route of latest referral
    public List<String> getLastRoute() {

        return lastRoute;
    }
}