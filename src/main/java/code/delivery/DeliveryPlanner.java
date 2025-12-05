package code.delivery;

import code.delivery.models.*;
import code.delivery.utils.GridGenerator; 
import code.delivery.utils.VisualizationUtil; 

import java.util.*;

public class DeliveryPlanner {
    public DeliveryPlanner() {}  

    public String plan(String initialState, String strategy, boolean visualize) {
        Grid grid = GridGenerator.parseGridString(initialState);
        List<Point> stores = grid.getStores();
        List<Point> customers = grid.getCustomers();
        int P = customers.size();
        int S = stores.size();

        Map<Integer, List<Integer>> assignment = new HashMap<>();
        int[][] costMatrix = new int[S][P];
        Map<Point, String> mapOfPairResults = new HashMap<>();  
        for (int j = 0; j < P; j++) {
            int minCost = Integer.MAX_VALUE;
            int bestStore = -1;
            for (int i = 0; i < S; i++) {
                DeliverySearch search = new DeliverySearch(grid, stores.get(i), customers.get(j));
                String result = search.solve(strategy);
                String[] res = result.split(";");
                costMatrix[i][j] = Integer.parseInt(res[1]);
                mapOfPairResults.put(customers.get(j), result);
                if (costMatrix[i][j] < minCost) {
                    minCost = costMatrix[i][j];
                    bestStore = i;
                }
            }
            assignment.computeIfAbsent(bestStore, k -> new ArrayList<>()).add(j);
        }

        StringBuilder sb = new StringBuilder();
        sb.append("Planning Results:\n");
        sb.append("number of stores: ").append(stores.size()).append("\n");
        sb.append("number of customers: ").append(customers.size()).append("\n");
        sb.append("Width of the grid: ").append(grid.getWidth()).append("\n");
        sb.append("Height of the grid: ").append(grid.getHeight()).append("\n");
       for(Tunnel tunnel : grid.getTunnels()) {
            sb.append("Tunnel between ").append(tunnel.getEntrance1()).append(" and ").append(tunnel.getEntrance2()).append("\n");
        }
        sb.append("Traffic:\n");
        for (Map.Entry<String, Integer> entry : grid.getTrafficLevels().entrySet()) {
            sb.append("From ").append(entry.getKey().replace("-", " to ")).append(" : Level ").append(entry.getValue()).append("\n");
        }
        int totalCost = 0;
        for (Map.Entry<Integer, List<Integer>> entry : assignment.entrySet()) {
            int storeIdx = entry.getKey();
            List<Integer> custs = entry.getValue();
            sb.append("(Store").append(storeIdx + 1).append(",Dests").append(custs).append("): ");
            StringBuilder plans = new StringBuilder();
            int truckCost = 0;
            for (int custIdx : custs) {
                String result = mapOfPairResults.get(customers.get(custIdx));
                String[] res = result.split(";");
                plans.append(res[0]).append(";");
                truckCost += Integer.parseInt(res[1]);
                if (visualize) {
                    visualizePath(stores.get(storeIdx), customers.get(custIdx), res[0].split(","));
                }
            }
            sb.append(plans.substring(0, plans.length() - 1)).append(";Total cost of this store :").append(truckCost).append(";");
            totalCost += truckCost;
        }
        sb.append("TotalCost:").append(totalCost);

    if (visualize) {
        VisualizationUtil.showGridAndResults(grid, assignment, mapOfPairResults, true);
    }

        return sb.toString();
    }

    private void visualizePath(Point start, Point goal, String[] actions) {
        System.out.println("Path from " + start + " to " + goal + ": " + String.join(",", actions));
    }
}