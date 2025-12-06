package code.search;

import code.delivery.State;
import java.util.*;
public class GenericSearch {
    private int nodesExpanded = 0;

    public Node search(Problem problem, QueuingFunction queuingFn) {
        nodesExpanded = 0;
        Queue<Node> nodes = new LinkedList<>();
        nodes.add(makeNode(problem.getInitialState()));
        Map<State, Node> visited = new HashMap<>();

        while (!nodes.isEmpty()) {
            Node node = nodes.poll();
    
            if (!queuingFn.shouldExpand(node, visited)) {
                continue;
            }

            visited.put(node.getState(), node);
            // Goal test AFTER the shouldExpand check (A* compatibility)
            if (problem.goalTest(node.getState())) {
                return node;
            }

            // Expand children
            List<Node> expanded = expand(node, problem);
            nodes = queuingFn.insert(expanded, nodes);
        }
        return null;
    }

    private Node makeNode(State state) {
        return new Node(state, null, null, 0, 0);
    }

    private List<Node> expand(Node node, Problem problem) {
        nodesExpanded++;
        List<Node> children = new ArrayList<>();
        List<String> operators = problem.getOperators(node.getState());
        for (String op : operators) {
            State successor = problem.applyOperator(node.getState(), op);
            if (successor != null) {
                int stepCost = problem.pathCost(node, op, successor);
                Node child = new Node(successor, node, op, node.getPathCost() + stepCost, node.getDepth() + 1);
                children.add(child);
            }
        }
        return children;
    }

    public int getNodesExpanded() { return nodesExpanded; }
}