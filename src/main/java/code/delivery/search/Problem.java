package code.search;

import code.delivery.State;
import java.util.List;

public interface Problem {
    State getInitialState();
    List<String> getOperators(State state);
    State applyOperator(State state, String operator);
    boolean goalTest(State state);
    int pathCost(Node parent, String operator, State successor);
}