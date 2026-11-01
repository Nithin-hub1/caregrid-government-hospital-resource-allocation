package com.hospital.resourceallocation.service;

import com.hospital.resourceallocation.entity.Bed;
import com.hospital.resourceallocation.entity.Doctor;
import com.hospital.resourceallocation.entity.Hospital;
import com.hospital.resourceallocation.entity.HospitalRoute;
import com.hospital.resourceallocation.entity.LocationRoute;
import com.hospital.resourceallocation.entity.NetworkLocation;
import com.hospital.resourceallocation.entity.Patient;
import com.hospital.resourceallocation.repository.BedRepository;
import com.hospital.resourceallocation.repository.DoctorRepository;
import com.hospital.resourceallocation.repository.HospitalRepository;
import com.hospital.resourceallocation.repository.HospitalRouteRepository;
import com.hospital.resourceallocation.repository.LocationRouteRepository;
import com.hospital.resourceallocation.repository.NetworkLocationRepository;
import com.hospital.resourceallocation.repository.PatientRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.hospital.resourceallocation.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;

import java.util.*;

@Service
public class ReferralService {

    private final PatientRepository patientRepository;
    private final HospitalRepository hospitalRepository;
    private final HospitalRouteRepository routeRepository;
    private final LocationRouteRepository locationRouteRepository;
    private final NetworkLocationRepository networkLocationRepository;
    private final BedRepository bedRepository;
    private final DoctorRepository doctorRepository;

    public ReferralService(
            PatientRepository patientRepository,
            HospitalRepository hospitalRepository,
            HospitalRouteRepository routeRepository,
            LocationRouteRepository locationRouteRepository,
            NetworkLocationRepository networkLocationRepository,
            BedRepository bedRepository,
            DoctorRepository doctorRepository) {

        this.patientRepository = patientRepository;
        this.hospitalRepository = hospitalRepository;
        this.routeRepository = routeRepository;
        this.locationRouteRepository = locationRouteRepository;
        this.networkLocationRepository = networkLocationRepository;
        this.bedRepository = bedRepository;
        this.doctorRepository = doctorRepository;
    }

    public record RouteStep(
            Long fromHospitalId,
            String fromHospitalName,
            Long toHospitalId,
            String toHospitalName,
            int travelMinutes) {
    }

    public record ReferralResponse(
            Long patientId,
            String patientName,
            String severity,
            Long sourceHospitalId,
            String sourceHospitalName,
            Long recommendedHospitalId,
            String recommendedHospitalName,
            String recommendedHospitalCity,
            String requiredBedType,
            Long availableBedId,
            String availableBedNumber,
            Long availableDoctorId,
            String availableDoctorName,
            int estimatedTravelMinutes,
            List<RouteStep> route,
            String status,
            String message) {
    }

    public record Edge(Long destinationId, int minutes) {
    }

    public record QueueNode(Long nodeId, int minutes) {
    }

    public record PathResult(
            int minutes,
            List<Long> nodeIds) {
    }

    public record Candidate(
            Hospital hospital,
            int travelMinutes,
            List<RouteStep> route,
            Bed bed,
            Doctor doctor) {
    }

    @Transactional(readOnly = true)
    public ReferralResponse recommendReferral(
            Long patientId,
            Long sourceHospitalId) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient not found: " + patientId));

        Hospital source = hospitalRepository.findById(sourceHospitalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Source hospital not found: " + sourceHospitalId));

        if (!source.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "The source hospital is inactive.");
        }

        String requiredBedType = requiredBedType(patient.getSeverity());
        if (requiredBedType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Unsupported patient severity: " + patient.getSeverity());
        }

        Candidate best = findBestReferralCandidate(patient, sourceHospitalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No reachable referral hospital has both a compatible available bed and an available doctor."));

        return new ReferralResponse(
                patient.getId(),
                patient.getPatientName(),
                patient.getSeverity(),
                source.getId(),
                source.getName(),
                best.hospital().getId(),
                best.hospital().getName(),
                best.hospital().getCity(),
                requiredBedType,
                best.bed().getId(),
                best.bed().getBedNumber(),
                best.doctor().getId(),
                best.doctor().getName(),
                best.travelMinutes(),
                best.route(),
                patient.getStatus(),
                "Referral destination recommended. Ready to confirm referral and reserve resources."
        );
    }

    @Transactional
    public ReferralResponse confirmReferral(
            Long patientId,
            Long sourceHospitalId,
            Long destinationHospitalId,
            Long bedId,
            Long doctorId) {

        Patient patient = patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient not found: " + patientId));

        if ("DISCHARGED".equalsIgnoreCase(patient.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Cannot refer a discharged patient.");
        }

        if ("ADMITTED".equalsIgnoreCase(patient.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Patient is already admitted. Discharge patient first to reassign.");
        }

        Hospital source = hospitalRepository.findById(sourceHospitalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Source hospital not found: " + sourceHospitalId));

        Hospital destination = hospitalRepository.findById(destinationHospitalId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Destination hospital not found: " + destinationHospitalId));

        if (!destination.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Destination hospital is inactive.");
        }

        String requiredBedType = requiredBedType(patient.getSeverity());
        if (requiredBedType == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Invalid patient severity: " + patient.getSeverity());
        }

        Bed bed = bedRepository.findById(bedId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Bed not found: " + bedId));

        if (!bed.getHospital().getId().equals(destinationHospitalId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Selected bed does not belong to the destination hospital.");
        }

        if (!bed.getType().equalsIgnoreCase(requiredBedType)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Bed type " + bed.getType() + " does not match required type " + requiredBedType);
        }

        if (!bed.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Selected bed is no longer available.");
        }

        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Doctor not found: " + doctorId));

        if (!doctor.getHospital().getId().equals(destinationHospitalId)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Selected doctor does not belong to the destination hospital.");
        }

        if (!doctor.isAvailable()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Selected doctor is no longer available.");
        }

        // Atomically reserve both resources
        bed.setAvailable(false);
        doctor.setAvailable(false);
        doctor.setAssignedPatient(patient);

        patient.setAssignedBedId(bed.getId());
        patient.setStatus("REFERRED");

        bedRepository.save(bed);
        doctorRepository.save(doctor);
        patientRepository.save(patient);

        // Calculate travel time and route
        int travelMinutes = calculateTravelTimeBetween(source, destination);
        List<RouteStep> routeSteps = calculateRouteStepsBetween(source, destination);

        return new ReferralResponse(
                patient.getId(),
                patient.getPatientName(),
                patient.getSeverity(),
                source.getId(),
                source.getName(),
                destination.getId(),
                destination.getName(),
                destination.getCity(),
                requiredBedType,
                bed.getId(),
                bed.getBedNumber(),
                doctor.getId(),
                doctor.getName(),
                travelMinutes,
                routeSteps,
                "REFERRED",
                "Referral confirmed! Bed " + bed.getBedNumber() + " and doctor " + doctor.getName()
                        + " safely reserved at " + destination.getName() + ". Transfer is in progress."
        );
    }

    @Transactional
    public ReferralResponse completeTransfer(Long patientId) {

        Patient patient = patientRepository.findByIdForUpdate(patientId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Patient not found: " + patientId));

        if (!"REFERRED".equalsIgnoreCase(patient.getStatus())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only patients with REFERRED status can complete transfer. Current status: " + patient.getStatus());
        }

        if (patient.getAssignedBedId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT, "Patient has no assigned bed.");
        }

        Bed bed = bedRepository.findById(patient.getAssignedBedId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Assigned bed not found: " + patient.getAssignedBedId()));

        Hospital hospital = bed.getHospital();
        List<Doctor> doctors = doctorRepository.findByAssignedPatient_Id(patientId);
        Doctor doctor = doctors.isEmpty() ? null : doctors.get(0);

                if (SecurityUtils.isHospitalOperator()) {
            Long userHospitalId = SecurityUtils.getCurrentHospitalId().orElse(null);
            Long destHospitalId = hospital.getId();
            Long sourceHospitalId = patient.getHospital() != null ? patient.getHospital().getId() : null;
            if (userHospitalId != null && !userHospitalId.equals(destHospitalId) && !userHospitalId.equals(sourceHospitalId)) {
                throw new AccessDeniedException("Access denied: You can only complete transfer for patients involving your hospital.");
            }
        }

        patient.setStatus("ADMITTED");
        patient.setHospital(hospital);
        patientRepository.save(patient);

        return new ReferralResponse(
                patient.getId(),
                patient.getPatientName(),
                patient.getSeverity(),
                null,
                null,
                hospital.getId(),
                hospital.getName(),
                hospital.getCity(),
                bed.getType(),
                bed.getId(),
                bed.getBedNumber(),
                doctor == null ? null : doctor.getId(),
                doctor == null ? null : doctor.getName(),
                0,
                Collections.emptyList(),
                "ADMITTED",
                "Transfer completed successfully. Patient " + patient.getPatientName()
                        + " is now admitted at " + hospital.getName() + " (Bed: " + bed.getBedNumber() + ")."
        );
    }

    @Transactional(readOnly = true)
    public Optional<Candidate> findBestReferralCandidate(
            Patient patient,
            Long sourceHospitalId) {

        String requiredBedType = requiredBedType(patient.getSeverity());
        if (requiredBedType == null) {
            return Optional.empty();
        }

        Hospital source = hospitalRepository.findById(sourceHospitalId).orElse(null);
        if (source == null || !source.isActive()) {
            return Optional.empty();
        }

        List<Hospital> allHospitals = hospitalRepository.findByActiveTrue();
        List<Candidate> candidates = new ArrayList<>();

        for (Hospital targetHospital : allHospitals) {
            if (targetHospital.getId().equals(sourceHospitalId)) {
                continue;
            }

            var beds = bedRepository.findByHospital_IdAndTypeAndAvailableTrue(
                    targetHospital.getId(), requiredBedType);
            if (beds.isEmpty()) {
                continue;
            }

            var doctors = findAppropriateDoctors(targetHospital.getId(), requiredBedType);
            if (doctors.isEmpty()) {
                continue;
            }

            int travelTime = calculateTravelTimeBetween(source, targetHospital);
            if (travelTime == Integer.MAX_VALUE) {
                continue;
            }

            List<RouteStep> route = calculateRouteStepsBetween(source, targetHospital);

            candidates.add(new Candidate(
                    targetHospital,
                    travelTime,
                    route,
                    beds.get(0),
                    doctors.get(0)
            ));
        }

        candidates.sort(Comparator.comparingInt(Candidate::travelMinutes));

        return candidates.isEmpty() ? Optional.empty() : Optional.of(candidates.get(0));
    }

    private List<Doctor> findAppropriateDoctors(Long hospitalId, String requiredBedType) {
        List<Doctor> doctors = doctorRepository.findByHospital_IdAndAvailableTrue(hospitalId);
        if (doctors.isEmpty()) {
            return Collections.emptyList();
        }

        // Prioritize doctor specialization matching facility
        String keyword = "ICU".equalsIgnoreCase(requiredBedType) ? "ICU" : "GENERAL";
        List<Doctor> matched = doctors.stream()
                .filter(d -> d.getSpecialization() != null
                        && d.getSpecialization().toUpperCase(Locale.ROOT).contains(keyword))
                .toList();

        return !matched.isEmpty() ? matched : doctors;
    }

    public int calculateTravelTimeBetween(Hospital source, Hospital destination) {
        if (source.getId().equals(destination.getId())) {
            return 0;
        }

        // Try location-based network routing first (8-10 locations network)
        if (source.getNetworkLocation() != null && destination.getNetworkLocation() != null) {
            Long sourceLocId = source.getNetworkLocation().getId();
            Long destLocId = destination.getNetworkLocation().getId();

            Map<Long, List<Edge>> locGraph = buildLocationGraph();
            if (!locGraph.isEmpty()) {
                Map<Long, PathResult> paths = calculateShortestPaths(sourceLocId, locGraph);
                if (paths.containsKey(destLocId)) {
                    return paths.get(destLocId).minutes();
                }
            }
        }

        // Fallback to hospital routes
        Map<Long, List<Edge>> hospGraph = buildHospitalGraph();
        Map<Long, PathResult> paths = calculateShortestPaths(source.getId(), hospGraph);
        if (paths.containsKey(destination.getId())) {
            return paths.get(destination.getId()).minutes();
        }

        return Integer.MAX_VALUE;
    }

    public List<RouteStep> calculateRouteStepsBetween(Hospital source, Hospital destination) {
        if (source.getId().equals(destination.getId())) {
            return Collections.emptyList();
        }

        // Location routes graph first
        if (source.getNetworkLocation() != null && destination.getNetworkLocation() != null) {
            Long sourceLocId = source.getNetworkLocation().getId();
            Long destLocId = destination.getNetworkLocation().getId();

            Map<Long, List<Edge>> locGraph = buildLocationGraph();
            if (!locGraph.isEmpty()) {
                Map<Long, PathResult> paths = calculateShortestPaths(sourceLocId, locGraph);
                if (paths.containsKey(destLocId)) {
                    return buildLocationRouteSteps(paths.get(destLocId), locGraph);
                }
            }
        }

        // Hospital routes fallback
        Map<Long, List<Edge>> hospGraph = buildHospitalGraph();
        Map<Long, PathResult> paths = calculateShortestPaths(source.getId(), hospGraph);
        if (paths.containsKey(destination.getId())) {
            return buildHospitalRouteSteps(paths.get(destination.getId()), hospGraph);
        }

        return Collections.emptyList();
    }

    private Map<Long, List<Edge>> buildLocationGraph() {
        Map<Long, List<Edge>> graph = new HashMap<>();
        List<LocationRoute> routes = locationRouteRepository.findAllWithLocations();
        for (LocationRoute route : routes) {
            Long srcId = route.getSourceLocation().getId();
            Long dstId = route.getDestinationLocation().getId();
            graph.computeIfAbsent(srcId, k -> new ArrayList<>())
                    .add(new Edge(dstId, route.getTravelMinutes()));
            graph.computeIfAbsent(dstId, k -> new ArrayList<>());
        }
        return graph;
    }

    private Map<Long, List<Edge>> buildHospitalGraph() {
        Map<Long, List<Edge>> graph = new HashMap<>();
        for (HospitalRoute route : routeRepository.findAll()) {
            Long srcId = route.getSourceHospital().getId();
            Long dstId = route.getDestinationHospital().getId();
            graph.computeIfAbsent(srcId, k -> new ArrayList<>())
                    .add(new Edge(dstId, route.getTravelMinutes()));
            graph.computeIfAbsent(dstId, k -> new ArrayList<>());
        }
        return graph;
    }

    private Map<Long, PathResult> calculateShortestPaths(Long sourceId, Map<Long, List<Edge>> graph) {
        Map<Long, Integer> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();
        PriorityQueue<QueueNode> queue = new PriorityQueue<>(Comparator.comparingInt(QueueNode::minutes));

        distances.put(sourceId, 0);
        queue.add(new QueueNode(sourceId, 0));

        while (!queue.isEmpty()) {
            QueueNode current = queue.poll();
            if (current.minutes() != distances.getOrDefault(current.nodeId(), Integer.MAX_VALUE)) {
                continue;
            }

            for (Edge edge : graph.getOrDefault(current.nodeId(), Collections.emptyList())) {
                long proposedLong = (long) current.minutes() + edge.minutes();
                if (proposedLong > Integer.MAX_VALUE) continue;
                int proposed = (int) proposedLong;

                if (proposed < distances.getOrDefault(edge.destinationId(), Integer.MAX_VALUE)) {
                    distances.put(edge.destinationId(), proposed);
                    previous.put(edge.destinationId(), current.nodeId());
                    queue.add(new QueueNode(edge.destinationId(), proposed));
                }
            }
        }

        Map<Long, PathResult> results = new HashMap<>();
        for (Map.Entry<Long, Integer> entry : distances.entrySet()) {
            Long destId = entry.getKey();
            LinkedList<Long> path = new LinkedList<>();
            Long current = destId;
            while (current != null) {
                path.addFirst(current);
                if (current.equals(sourceId)) break;
                current = previous.get(current);
            }
            if (!path.isEmpty() && path.getFirst().equals(sourceId)) {
                results.put(destId, new PathResult(entry.getValue(), List.copyOf(path)));
            }
        }

        return results;
    }

    private List<RouteStep> buildLocationRouteSteps(PathResult path, Map<Long, List<Edge>> graph) {
        List<RouteStep> steps = new ArrayList<>();
        Map<Long, NetworkLocation> locMap = new HashMap<>();
        networkLocationRepository.findAll().forEach(l -> locMap.put(l.getId(), l));

        for (int i = 0; i < path.nodeIds().size() - 1; i++) {
            Long fromId = path.nodeIds().get(i);
            Long toId = path.nodeIds().get(i + 1);

            Edge chosenEdge = graph.getOrDefault(fromId, Collections.emptyList())
                    .stream()
                    .filter(e -> e.destinationId().equals(toId))
                    .min(Comparator.comparingInt(Edge::minutes))
                    .orElseThrow(() -> new IllegalStateException("Route edge not found"));

            NetworkLocation fromLoc = locMap.get(fromId);
            NetworkLocation toLoc = locMap.get(toId);

            steps.add(new RouteStep(
                    fromId,
                    fromLoc != null ? fromLoc.getName() : "Location #" + fromId,
                    toId,
                    toLoc != null ? toLoc.getName() : "Location #" + toId,
                    chosenEdge.minutes()
            ));
        }

        return steps;
    }

    private List<RouteStep> buildHospitalRouteSteps(PathResult path, Map<Long, List<Edge>> graph) {
        List<RouteStep> steps = new ArrayList<>();
        Map<Long, Hospital> hospMap = new HashMap<>();
        hospitalRepository.findAll().forEach(h -> hospMap.put(h.getId(), h));

        for (int i = 0; i < path.nodeIds().size() - 1; i++) {
            Long fromId = path.nodeIds().get(i);
            Long toId = path.nodeIds().get(i + 1);

            Edge chosenEdge = graph.getOrDefault(fromId, Collections.emptyList())
                    .stream()
                    .filter(e -> e.destinationId().equals(toId))
                    .min(Comparator.comparingInt(Edge::minutes))
                    .orElseThrow(() -> new IllegalStateException("Hospital route edge not found"));

            Hospital fromHosp = hospMap.get(fromId);
            Hospital toHosp = hospMap.get(toId);

            steps.add(new RouteStep(
                    fromId,
                    fromHosp != null ? fromHosp.getName() : "Hospital #" + fromId,
                    toId,
                    toHosp != null ? toHosp.getName() : "Hospital #" + toId,
                    chosenEdge.minutes()
            ));
        }

        return steps;
    }

    public String requiredBedType(String severity) {
        if (severity == null) {
            return null;
        }

        return switch (severity.trim().toUpperCase(Locale.ROOT)) {
            case "CRITICAL" -> "ICU";
            case "SERIOUS", "MINOR" -> "GENERAL";
            default -> null;
        };
    }
}
