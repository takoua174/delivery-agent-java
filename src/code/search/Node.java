package code.search;

import code.delivery.State;
import java.util.ArrayList;
import java.util.List;

public class Node {
    private State state;
    private Node parent;
    private String action;
    private int pathCost;
    private int depth;

    public Node(State state, Node parent, String action, int pathCost, int depth) {
        this.state = state;
        this.parent = parent;
        this.action = action;
        this.pathCost = pathCost;
        this.depth = depth;
    }

    public State getState() { return state; }
    public Node getParent() { return parent; }
    public String getAction() { return action; }
    public int getPathCost() { return pathCost; }
    public int getDepth() { return depth; }

    public List<String> getPath() {
        List<String> path = new ArrayList<>();
        Node current = this;
        while (current.parent != null) {
            path.add(0, current.action);
            current = current.parent;
        }
        return path;
    }
}