package code.delivery.utils;

import code.delivery.models.Grid;
import code.delivery.models.Point;
import code.delivery.models.Tunnel;

import java.util.*;

public class GridGenerator {
    private static final Random rand = new Random();

    public static String genGrid() {
        int m = 5 + rand.nextInt(11);  // width 5-15
        int n = 5 + rand.nextInt(11);  // height
        int P = 1 + rand.nextInt(10);  // 1-10
        int S = 1 + rand.nextInt(3);   // 1-3
        int numTunnels = rand.nextInt(5);  // 0-4

        // Random distinct stores
        Set<Point> occupied = new HashSet<>();
        List<Point> stores = new ArrayList<>();
        for (int i = 0; i < S; i++) {
            Point p;
            do { p = new Point(rand.nextInt(m), rand.nextInt(n)); } while (occupied.contains(p));
            stores.add(p);
            occupied.add(p);
        }

        // Random distinct customers
        List<Point> customers = new ArrayList<>();
        for (int i = 0; i < P; i++) {
            Point p;
            do { p = new Point(rand.nextInt(m), rand.nextInt(n)); } while (occupied.contains(p));
            customers.add(p);
            occupied.add(p);
        }

        // Random tunnels
        List<Tunnel> tunnels = new ArrayList<>();
        for (int i = 0; i < numTunnels; i++) {
            Point e1, e2;
            do {
                e1 = new Point(rand.nextInt(m), rand.nextInt(n));
                e2 = new Point(rand.nextInt(m), rand.nextInt(n));
            } while (e1.equals(e2) || occupied.contains(e1) || occupied.contains(e2)); 
            occupied.add(e1);
            occupied.add(e2);
            tunnels.add(new Tunnel(e1, e2));
        }

        // All edges: horizontal then vertical
        StringBuilder trafficSb = new StringBuilder();
        // Horizontal
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < m - 1; x++) {
                Point from = new Point(x, y);
                Point to = new Point(x + 1, y);
                int tr = rand.nextInt(5);  // traffic between 0-4
                trafficSb.append(from.getX()).append(",").append(from.getY()).append(",")
                         .append(to.getX()).append(",").append(to.getY()).append(",").append(tr).append(";");
                trafficSb.append(to.getX()).append(",").append(to.getY()).append(",")
                 .append(from.getX()).append(",").append(from.getY()).append(",").append(tr).append(";");
            }
        }
        // Vertical
        for (int x = 0; x < m; x++) {
            for (int y = 0; y < n - 1; y++) {
                Point from = new Point(x, y);
                Point to = new Point(x, y + 1);
                int tr = rand.nextInt(5);
                trafficSb.append(from.getX()).append(",").append(from.getY()).append(",")
                         .append(to.getX()).append(",").append(to.getY()).append(",").append(tr).append(";");
                trafficSb.append(to.getX()).append(",").append(to.getY()).append(",")
                         .append(from.getX()).append(",").append(from.getY()).append(",").append(tr).append(";");
            }
        }

        // Build string: m;n;P;S;StoreX1,Y1,...;CustX1,Y1,...;TunX1,Y1,TunX2,Y2,...;traffic...
        //add stores and customers numbers
        StringBuilder sb = new StringBuilder()
            .append(m).append(";").append(n).append(";").append(P).append(";").append(S).append(";");
        //stores locations
        for (Point p : stores) sb.append(p.getX()).append(",").append(p.getY()).append(",");
        sb.setLength(sb.length() - 1); sb.append(";");
        //customers locations
        for (Point p : customers) sb.append(p.getX()).append(",").append(p.getY()).append(",");
        sb.setLength(sb.length() - 1); sb.append(";");
        //tunnels locations
        for (Tunnel t : tunnels) {
            sb.append(t.getEntrance1().getX()).append(",").append(t.getEntrance1().getY()).append(",")
            .append(t.getEntrance2().getX()).append(",").append(t.getEntrance2().getY()).append(",");
        }
        if (!tunnels.isEmpty()) sb.setLength(sb.length() - 1);
        // traffic levels
        sb.append(";");  
        sb.append(trafficSb);  // ← trafficSb already has its own semicolons
        return sb.toString();
    }

    public static Grid parseGridString(String initialState) {
        String[] parts = initialState.split(";");
        int m = Integer.parseInt(parts[0]);
        int n = Integer.parseInt(parts[1]);
        int P = Integer.parseInt(parts[2]);
        int S = Integer.parseInt(parts[3]);

        Grid grid = new Grid(m, n);

        // Stores: after S;
        String[] storeStrs = parts[4].split(",");
        List<Point> stores = new ArrayList<>();
        for (int i = 0; i < S * 2; i += 2) {
            Point p = new Point(Integer.parseInt(storeStrs[i]), Integer.parseInt(storeStrs[i+1]));
            stores.add(p);
            grid.addStore(p);
        }

        // Customers: next
        String[] custStrs = parts[5].split(",");
        List<Point> customers = new ArrayList<>();
        for (int i = 0; i < P * 2; i += 2) {
            Point p = new Point(Integer.parseInt(custStrs[i]), Integer.parseInt(custStrs[i+1]));
            customers.add(p);
            grid.addCustomer(p);
        }

        // Tunnels: pairs
        if (parts.length > 6 && !parts[6].isEmpty()) {
            String[] tunStrs = parts[6].split(",");
            for (int i = 0; i < tunStrs.length; i += 4) {
                Point e1 = new Point(Integer.parseInt(tunStrs[i]), Integer.parseInt(tunStrs[i+1]));
                Point e2 = new Point(Integer.parseInt(tunStrs[i+2]), Integer.parseInt(tunStrs[i+3]));
                grid.addTunnel(new Tunnel(e1, e2));
            }
        }

        // Traffic: last part
        if (parts.length > 6) {
            String[] trafficStrs = parts[parts.length - 1].split(";");
            for (String tr : trafficStrs) {
                if (tr.isEmpty()) continue;
                String[] t = tr.split(",");
                Point from = new Point(Integer.parseInt(t[0]), Integer.parseInt(t[1]));
                Point to = new Point(Integer.parseInt(t[2]), Integer.parseInt(t[3]));
                int level = Integer.parseInt(t[4]);
                grid.addTraffic(from, to, level);
            }
        }

        return grid;
    }
}