package code.search;

import java.util.List;
import java.util.Queue;

import java.util.Map;
import code.delivery.State;

public interface QueuingFunction {
    Queue<Node> insert(List<Node> expanded, Queue<Node> frontier);
    boolean shouldExpand(Node node, Map<State, Node> visited);

}