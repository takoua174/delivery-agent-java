package code.delivery;

import code.delivery.models.*;
import code.search.*;
import java.util.*;

public class DeliverySearch extends GenericSearch implements Problem {
    private final Grid grid;
    private final Point store;
    private final Point dest;

    public DeliverySearch(Grid grid, Point store, Point dest) {
        this.grid = grid;
        this.store = store;
        this.dest = dest;
    }

    @Override
    public State getInitialState() {
        return new State(store);
    }

    @Override
    public List<String> getOperators(State state) {
        List<String> ops = new ArrayList<>();
        Point pos = state.getCurrentLocation();
        String[] dirs = {"up", "down", "left", "right"};
        int[] dx = {0, 0, -1, 1};
        int[] dy = {-1, 1, 0, 0};
        for (int i = 0; i < 4; i++) {
            Point next = new Point(pos.getX() + dx[i], pos.getY() + dy[i]);
            if (grid.isValidPosition(next) && !grid.isBlocked(pos, next)) {
                ops.add(dirs[i]);
            }
        }
        Tunnel tunnel = grid.getTunnelAt(pos);
        if (tunnel != null) {
            ops.add("tunnel");
        }
        return ops;
    }

    @Override
    public State applyOperator(State state, String operator) {
        Point pos = state.getCurrentLocation();
        if (operator.equals("tunnel")) {
            Tunnel tunnel = grid.getTunnelAt(pos);
            if (tunnel != null) {
                return new State(tunnel.getOtherEnd(pos));
            }
            return null;
        }
        int dx = 0, dy = 0;
        switch (operator) {
            case "up": dy = -1; break;
            case "down": dy = 1; break;
            case "left": dx = -1; break;
            case "right": dx = 1; break;
        }
        Point next = new Point(pos.getX() + dx, pos.getY() + dy);
        if (grid.isValidPosition(next) && !grid.isBlocked(pos, next)) {
            return new State(next);
        }
        return null;
    }

    @Override
    public boolean goalTest(State state) {
        return state.getCurrentLocation().equals(dest);
    }

    @Override
    public int pathCost(Node parent, String operator, State successor) {
        Point from = parent.getState().getCurrentLocation();
        Point to = successor.getCurrentLocation();
        if (operator.equals("tunnel")) {
            return grid.manhattanDistance(from, to);
        }
        return grid.getTrafficCost(from, to);
    }

    private int heuristic1(State state) {
        return grid.manhattanDistance(state.getCurrentLocation(), dest);
    }

    private int heuristic2(State state) {
        return (int) (1.5 * heuristic1(state));
    }

    public String solve(String strategy) {
        QueuingFunction qf = getQueuingFunction(strategy);
        Node goal = search(this, qf);
        if (goal == null) return "NoPath;999999;0";  // FIXED: High int, not "INF"
        List<String> path = goal.getPath();
        int oneWayCost = goal.getPathCost();
        return String.join(",", path) + ";" + (2 * oneWayCost) + ";" + getNodesExpanded();
    }

    private QueuingFunction getQueuingFunction(String strategy) {
        return switch (strategy) {
            case "BF" -> new BFSQueuingFunction();
            case "DF" -> new DFSQueuingFunction();
            case "UC" -> new UCSQueuingFunction();
            case "GR1" -> new GreedyQueuingFunction(this, 1);
            case "GR2" -> new GreedyQueuingFunction(this, 2);
            case "AS1" -> new AStarQueuingFunction(this, 1);
            case "AS2" -> new AStarQueuingFunction(this, 2);
            case "ID" -> new IDSQueuingFunction();
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategy);
        };
    }

    private static class BFSQueuingFunction implements QueuingFunction {
        @Override
        public Queue<Node> insert(List<Node> expanded, Queue<Node> frontier) {
            Queue<Node> newFrontier = new LinkedList<>(frontier);
            newFrontier.addAll(expanded);
            return newFrontier;
        }
    }

    private static class DFSQueuingFunction implements QueuingFunction {
        @Override
        public Queue<Node> insert(List<Node> expanded, Queue<Node> frontier) {
            Deque<Node> newFrontier = new LinkedList<>(frontier);
            for (int i = expanded.size() - 1; i >= 0; i--) {
                newFrontier.addFirst(expanded.get(i));
            }
            return newFrontier;
        }
    }

    private static class UCSQueuingFunction implements QueuingFunction {
        @Override
        public Queue<Node> insert(List<Node> expanded, Queue<Node> frontier) {
            PriorityQueue<Node> pq = new PriorityQueue<>(Comparator.comparingInt(Node::getPathCost));
            pq.addAll(frontier);
            pq.addAll(expanded);
            return pq;
        }
    }

    private static class GreedyQueuingFunction implements QueuingFunction {
        private final DeliverySearch search;
        private final int hNum;

        GreedyQueuingFunction(DeliverySearch search, int hNum) {
            this.search = search;
            this.hNum = hNum;
        }

        @Override
        public Queue<Node> insert(List<Node> expanded, Queue<Node> frontier) {
            PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> {
                int ha = hNum == 1 ? search.heuristic1(a.getState()) : search.heuristic2(a.getState());
                int hb = hNum == 1 ? search.heuristic1(b.getState()) : search.heuristic2(b.getState());
                return Integer.compare(ha, hb);
            });
            pq.addAll(frontier);
            pq.addAll(expanded);
            return pq;
        }
    }

    private static class AStarQueuingFunction implements QueuingFunction {
        private final DeliverySearch search;
        private final int hNum;

        AStarQueuingFunction(DeliverySearch search, int hNum) {
            this.search = search;
            this.hNum = hNum;
        }

        @Override
        public Queue<Node> insert(List<Node> expanded, Queue<Node> frontier) {
            PriorityQueue<Node> pq = new PriorityQueue<>((a, b) -> {
                int fa = a.getPathCost() + (hNum == 1 ? search.heuristic1(a.getState()) : search.heuristic2(a.getState()));
                int fb = b.getPathCost() + (hNum == 1 ? search.heuristic1(b.getState()) : search.heuristic2(b.getState()));
                return Integer.compare(fa, fb);
            });
            pq.addAll(frontier);
            pq.addAll(expanded);
            return pq;
        }
    }

    private static class IDSQueuingFunction implements QueuingFunction {
        @Override
        public Queue<Node> insert(List<Node> expanded, Queue<Node> frontier) {
            return new DFSQueuingFunction().insert(expanded, frontier);
        }
    }
}