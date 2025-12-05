package code.delivery.models;
import java.util.stream.Collectors;
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
    public int getWidth() { return width; }  
    public int getHeight() { return height; }  
    public boolean isValidPosition(Point p) {
        return p.getX() >= 0 && p.getX() < width && p.getY() >= 0 && p.getY() < height;
    }

    public int getTrafficCost(Point from, Point to) {
        String key = edgeKey(from, to);
        Integer cost = trafficLevels.get(key);
        return cost ;
    }

    public List<Tunnel> getTunnels() {
        return tunnels;
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
    //a point can't be an enterance for more than one tunnel
    public Tunnel getTunnelAt(Point position) {
        return tunnels.stream().filter(t -> t.hasEntrance(position)).findFirst().orElse(null);
    }
    public Map<String, Integer> getTrafficLevels() {
        return trafficLevels;
    }

    public List<Point> getStores() { return stores; }
    public List<Point> getCustomers() { return customers; }

    public int manhattanDistance(Point p1, Point p2) {
        return Math.abs(p1.getX() - p2.getX()) + Math.abs(p1.getY() - p2.getY());
    }

    public List<Point> getNeighbors(Point p) {
        List<Point> neighbors = new ArrayList<>();
        int x = p.getX();
        int y = p.getY();

        Point[] candidates = new Point[] {
            new Point(x + 1, y),
            new Point(x - 1, y),
            new Point(x, y + 1),
            new Point(x, y - 1)
        };

        for (Point np : candidates) {
            if (isValidPosition(np) && !isBlocked(p, np)) {
                neighbors.add(np);
            }
        }

        for (Tunnel t : tunnels) {
            if (t.hasEntrance(p)) {
                Point other = t.getOtherEnd(p);
                if (!neighbors.contains(other)) neighbors.add(other);
            }
        }

        return neighbors;
    }

    private String edgeKey(Point from, Point to) {
        return from.getX() + "," + from.getY() + "," + to.getX() + "," + to.getY();
    }


}