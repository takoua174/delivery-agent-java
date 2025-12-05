package code;

import code.delivery.DeliveryPlanner;
import code.delivery.utils.GridGenerator;

public class Main {
    public static void main(String[] args) {
        String initialState = GridGenerator.genGrid();
        DeliveryPlanner planner = new DeliveryPlanner();
        String result = planner.plan(initialState, "DF", true);
        System.out.println(result);
    }
}