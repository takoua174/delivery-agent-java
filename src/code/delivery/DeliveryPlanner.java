package code.delivery;

import code.delivery.models.*;

import java.util.*;

public class DeliveryPlanner {
    private final Grid grid;
    private List<Point> stores;
    private List<Point> customers;

    public DeliveryPlanner(Grid grid) {
        this.grid = grid;
    }

    public String plan(String initialState, String strategy, boolean visualize) {
        String[] parts = initialState.split(";");
        if (parts.length < 8) {  // FIXED: Handle short strings
            return "Invalid initialState;0;0";
        }
        int P = Integer.parseInt(parts[2]);
        int S = Integer.parseInt(parts[3]);
        stores = parsePoints(parts[4], S);
        customers = parsePoints(parts[5], P);
        if (!parts[6].isEmpty()) parseTunnels(grid, parts[6]);
        if (!parts[7].isEmpty()) parseTraffic(grid, parts[7]);

        // All combinations: Recursive partition
        List<Integer> custIndices = new ArrayList<>();
        for (int i = 0; i < P; i++) custIndices.add(i);
        Assignment best = findMinCostAssignment(custIndices, 0, new ArrayList<>(Collections.nCopies(S, new ArrayList<>())), 0, strategy);

        // Output
        StringBuilder sb = new StringBuilder();
        int totalCost = best.totalCost;
        for (int i = 0; i < S; i++) {
            List<Integer> subset = best.subsets.get(i);
            if (subset.isEmpty()) continue;
            sb.append("(Store").append(i + 1).append(",Dests").append(subset).append("): ");
            StringBuilder plans = new StringBuilder();
            int subsetCost = 0;
            for (int custIdx : subset) {
                DeliverySearch search = new DeliverySearch(grid, stores.get(i), customers.get(custIdx));
                String result = search.solve(strategy);
                String[] res = result.split(";");
                plans.append(res[0]).append(";");
                subsetCost += Integer.parseInt(res[1]);
                if (visualize) visualizePath(stores.get(i), customers.get(custIdx), res[0].split(","));
            }
            sb.append(plans.substring(0, plans.length() - 1)).append(";").append(subsetCost).append(";").append("aggNodes\n");
        }
        sb.append("TotalCost:").append(totalCost);
        return sb.toString();
    }

    private Assignment findMinCostAssignment(List<Integer> custs, int truckIdx, List<List<Integer>> subsets, int currentCost, String strategy) {
        if (custs.isEmpty()) {
            return new Assignment(new ArrayList<>(subsets), currentCost);
        }

        int minCost = Integer.MAX_VALUE;
        List<List<Integer>> bestSubsets = new ArrayList<>(subsets);
        for (int i = 0; i < stores.size(); i++) {  // Try assigning next cust to each truck
            List<Integer> newCusts = new ArrayList<>(custs);
            int cust = newCusts.remove(0);
            List<Integer> subset = new ArrayList<>(subsets.get(i));
            subset.add(cust);
            subsets.set(i, subset);

            DeliverySearch search = new DeliverySearch(grid, stores.get(i), customers.get(cust));
            String result = search.solve(strategy);
            String[] res = result.split(";");
            int pairCost = Integer.parseInt(res[1]);  // FIXED: Handle "999999" as int

            Assignment rec = findMinCostAssignment(newCusts, truckIdx, subsets, currentCost + pairCost, strategy);
            if (rec.totalCost < minCost) {
                minCost = rec.totalCost;
                bestSubsets = rec.subsets;
            }

            subsets.set(i, new ArrayList<>(subset));  // Backtrack
            subset.remove(Integer.valueOf(cust));
        }

        return new Assignment(bestSubsets, minCost);
    }

    private List<Point> parsePoints(String str, int count) {
        List<Point> points = new ArrayList<>();
        if (str.isEmpty()) return points;  // FIXED: Empty string
        String[] coords = str.split(",");
        for (int i = 0; i < Math.min(coords.length, count * 2); i += 2) {
            points.add(new Point(Integer.parseInt(coords[i]), Integer.parseInt(coords[i + 1])));
        }
        return points;
    }

    private void parseTunnels(Grid grid, String str) {
        if (str.isEmpty()) return;
        String[] pairs = str.split(",");
        for (int i = 0; i < pairs.length; i += 4) {
            if (i + 3 >= pairs.length) break;  // FIXED: Incomplete
            Point e1 = new Point(Integer.parseInt(pairs[i]), Integer.parseInt(pairs[i + 1]));
            Point e2 = new Point(Integer.parseInt(pairs[i + 2]), Integer.parseInt(pairs[i + 3]));
            grid.addTunnel(new Tunnel(e1, e2));
        }
    }

    private void parseTraffic(Grid grid, String str) {
        if (str.isEmpty()) return;
        String[] edges = str.split(";");
        for (String edge : edges) {
            if (edge.isEmpty()) continue;
            String[] t = edge.split(",");
            if (t.length < 5) continue;  // FIXED: Incomplete
            Point from = new Point(Integer.parseInt(t[0]), Integer.parseInt(t[1]));
            Point to = new Point(Integer.parseInt(t[2]), Integer.parseInt(t[3]));
            int level = Integer.parseInt(t[4]);
            grid.addTraffic(from, to, level);
        }
    }

    private void visualizePath(Point start, Point goal, String[] actions) {
        System.out.println("Visualizing from " + start + " to " + goal + ": " + String.join(",", actions));
    }

    private static class Assignment {
        List<List<Integer>> subsets;
        int totalCost;

        Assignment(List<List<Integer>> subsets, int totalCost) {
            this.subsets = subsets;
            this.totalCost = totalCost;
        }
    }
}