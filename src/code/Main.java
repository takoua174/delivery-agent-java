package code;

import code.delivery.DeliveryPlanner;
import code.delivery.utils.GridGenerator;

public class Main {
    public static void main(String[] args) {
        // 1. Generate complete grid string (includes traffic!)
        String initialState = GridGenerator.genGrid();
        // 2. Parse string to create Grid object
        var grid = GridGenerator.parseGridString(initialState);
        // 3. Create planner with grid
        var planner = new DeliveryPlanner(grid);
        // 4. Find plan - pass same initialState string
        String result = planner.plan(initialState, "AS1", true);
        System.out.println(result);
    }
}