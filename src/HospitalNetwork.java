import java.util.*;

public class HospitalNetwork {

    private Map<String, Map<String, Integer>> graph;

    public HospitalNetwork() {
        graph = new HashMap<>();
    }

    // Add a hospital/location to the network
    public void addLocation(String location) {
        graph.putIfAbsent(location, new HashMap<>());
    }

    // Add a two-way connection
    public void addConnection(String location1, String location2, int travelTime) {

        addLocation(location1);
        addLocation(location2);

        graph.get(location1).put(location2, travelTime);
        graph.get(location2).put(location1, travelTime);
    }

    // Existing Dijkstra method
    public Map<String, Integer> dijkstra(String start) {

        Map<String, Integer> distance = new HashMap<>();

        for (String node : graph.keySet()) {
            distance.put(node, Integer.MAX_VALUE);
        }

        distance.put(start, 0);

        PriorityQueue<Node> pq =
                new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));

        pq.add(new Node(start, 0));

        while (!pq.isEmpty()) {

            Node current = pq.poll();

            // Ignore outdated queue entries
            if (current.distance > distance.get(current.location)) {
                continue;
            }

            for (Map.Entry<String, Integer> neighbor :
                    graph.get(current.location).entrySet()) {

                String nextLocation = neighbor.getKey();
                int travelTime = neighbor.getValue();

                int newDistance =
                        current.distance + travelTime;

                if (newDistance < distance.get(nextLocation)) {

                    distance.put(nextLocation, newDistance);

                    pq.add(new Node(
                            nextLocation,
                            newDistance
                    ));
                }
            }
        }

        return distance;
    }

    // Find shortest travel time between two locations
    public int getShortestDistance(String start, String destination) {

        Map<String, Integer> distances = dijkstra(start);

        return distances.getOrDefault(
                destination,
                Integer.MAX_VALUE
        );
    }

    // Find the actual shortest route
    public List<String> getShortestPath(
            String start,
            String destination) {

        Map<String, Integer> distance = new HashMap<>();
        Map<String, String> previous = new HashMap<>();

        for (String node : graph.keySet()) {
            distance.put(node, Integer.MAX_VALUE);
        }

        distance.put(start, 0);

        PriorityQueue<Node> pq =
                new PriorityQueue<>(Comparator.comparingInt(n -> n.distance));

        pq.add(new Node(start, 0));

        while (!pq.isEmpty()) {

            Node current = pq.poll();

            if (current.distance >
                    distance.get(current.location)) {

                continue;
            }

            if (current.location.equals(destination)) {
                break;
            }

            for (Map.Entry<String, Integer> neighbor :
                    graph.get(current.location).entrySet()) {

                String nextLocation = neighbor.getKey();
                int travelTime = neighbor.getValue();

                int newDistance =
                        current.distance + travelTime;

                if (newDistance < distance.get(nextLocation)) {

                    distance.put(nextLocation, newDistance);

                    previous.put(
                            nextLocation,
                            current.location
                    );

                    pq.add(new Node(
                            nextLocation,
                            newDistance
                    ));
                }
            }
        }

        // Destination unreachable
        if (!distance.containsKey(destination)
                || distance.get(destination) == Integer.MAX_VALUE) {

            return new ArrayList<>();
        }

        // Reconstruct path
        LinkedList<String> path = new LinkedList<>();

        String current = destination;

        while (current != null) {

            path.addFirst(current);

            current = previous.get(current);
        }

        return path;
    }

    // Display network connections
    public void displayNetwork() {

        System.out.println("\n========== HOSPITAL NETWORK ==========");

        for (String location : graph.keySet()) {

            for (Map.Entry<String, Integer> connection :
                    graph.get(location).entrySet()) {

                // Avoid printing duplicate connections
                if (location.compareTo(connection.getKey()) < 0) {

                    System.out.println(
                            location + " <---- "
                            + connection.getValue()
                            + " min ----> "
                            + connection.getKey()
                    );
                }
            }
        }

        System.out.println("======================================");
    }

    // Internal node class for Dijkstra
    private static class Node {

        String location;
        int distance;

        Node(String location, int distance) {

            this.location = location;
            this.distance = distance;
        }
    }
}