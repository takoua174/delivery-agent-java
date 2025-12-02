package code.delivery.models;

import java.util.*;

public class Grid {
    private final int width, height;
    private final Map<String, Integer> trafficLevels = new HashMap<>();
    private final Set<String> blockedRoads = new HashSet<>();
    private final List<Tunnel> tunnels = new ArrayList<>();
    private final List<Point> stores = new ArrayList<>();
    private final List<Point> customers = new ArrayList<>();

    public Grid(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public boolean isValidPosition(Point p) {
        return p.getX() >= 0 && p.getX() < width && p.getY() >= 0 && p.getY() < height;
    }

    public int getTrafficCost(Point from, Point to) {
        String key = edgeKey(from, to);
        Integer cost = trafficLevels.get(key);
        return cost != null ? cost : 1;
    }

    public boolean isBlocked(Point from, Point to) {
        return getTrafficCost(from, to) == 0;
    }

    public void addTraffic(Point from, Point to, int level) {
        String key = edgeKey(from, to);
        trafficLevels.put(key, level);
        if (level == 0) blockedRoads.add(key);
    }

    public void addTunnel(Tunnel tunnel) {
        tunnels.add(tunnel);
    }

    public void addStore(Point store) {
        stores.add(store);
    }

    public void addCustomer(Point customer) {
        customers.add(customer);
    }

    public Tunnel getTunnelAt(Point position) {
        return tunnels.stream().filter(t -> t.hasEntrance(position)).findFirst().orElse(null);
    }

    public List<Point> getStores() { return stores; }
    public List<Point> getCustomers() { return customers; }

    public int manhattanDistance(Point p1, Point p2) {
        return Math.abs(p1.getX() - p2.getX()) + Math.abs(p1.getY() - p2.getY());
    }

    private String edgeKey(Point from, Point to) {
        return from.getX() + "," + from.getY() + "-" + to.getX() + "," + to.getY();
    }
}