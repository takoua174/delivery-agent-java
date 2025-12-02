package code;

import code.delivery.DeliveryPlanner;
import code.delivery.utils.GridGenerator;

public class Main {
    public static void main(String[] args) {
        String initialState = GridGenerator.genGrid();
        DeliveryPlanner planner = new DeliveryPlanner();
        String result = planner.plan(initialState, "AS1", true);
        System.out.println(result);
    }
}