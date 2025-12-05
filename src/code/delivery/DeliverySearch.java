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
        int[] dy = {1, -1, 0, 0};
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
            case "up": dy = 1; break;
            case "down": dy = -1; break;
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

    private String formatSolution(Node goal) {
        List<String> path = goal.getPath();
        
        // NEW: Validate path reaches goal (debug the reconstruction bug)
        Point simulated = store;
        System.out.println("DEBUG: Simulating path from " + store + " with actions: " + String.join(",", path));  // Log the raw path
        for (int i = 0; i < path.size(); i++) {
            String op = path.get(i);
            State currState = new State(simulated);
            State successor = applyOperator(currState, op);  // Re-apply exactly
            if (successor == null) {
                System.err.println("ERROR: Invalid step #" + (i+1) + ": '" + op + "' from " + simulated + " (applyOperator failed)");
                return "InvalidPath;" + 0 + ";" + getNodesExpanded();
            }
            simulated = successor.getCurrentLocation();
            System.out.println("  Step " + (i+1) + " (" + op + "): to " + simulated);
        }
        if (!simulated.equals(dest)) {
            System.err.println("ERROR: Path ends at " + simulated + ", expected goal " + dest + "!");
            return "NoPath;0;" + getNodesExpanded();
        }
        System.out.println("DEBUG: Path validated OK, ends at " + dest + ", cost=" + goal.getPathCost());
        
        // Rest unchanged: returnPath, totalCost=2*..., fullPath
        List<String> returnPath = new ArrayList<>();
        // ... (reverse loop)
        int totalCost = goal.getPathCost();
        String fullPath = String.join(",", path) + ",|RETURN|," + String.join(",", returnPath);
        return fullPath + ";" + totalCost + ";" + getNodesExpanded();
    }

    public String solve(String strategy) {
        if (strategy.equals("ID")) {
            return solveIDS();
        }
        QueuingFunction qf = getQueuingFunction(strategy);
        Node goal = search(this, qf);
        if (goal == null) return "NoPath;0;0"; 
        return formatSolution(goal); 
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
            default -> throw new IllegalArgumentException("Unknown strategy: " + strategy);
        };
    }
    private String solveIDS() {
        int maxDepth = grid.getWidth() * grid.getHeight();
        Node result = null;

        for (int depth = 0; depth <= maxDepth; depth++) {
            result = depthLimitedSearch(depth);

            if (result != null) {
                return formatSolution(result);
            }
        }
        return "NoPath;" + 0 + ";" + getNodesExpanded();
    }

    private static class BFSQueuingFunction implements QueuingFunction {
        @Override
        public Queue<Node> insert(List<Node> expanded, Queue<Node> frontier) {
            frontier.addAll(expanded);
            return frontier;
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
            //which heuristic to use
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

    private Node depthLimitedSearch(int limit) {
        QueuingFunction qf = new LimitedDepthSearchQueuingFunction(limit);
        return search(this, qf);
    }
    
    private String reverseOp(String op) {
    return switch (op) {
        case "up" -> "down";
        case "down" -> "up";
        case "left" -> "right";
        case "right" -> "left";
        case "tunnel" -> "tunnel"; 
        default -> throw new IllegalArgumentException("Unknown operator: " + op);
    };

    }

    private static class LimitedDepthSearchQueuingFunction implements QueuingFunction, DepthLimited {
        private final int depthLimit;
        private final DFSQueuingFunction dfs = new DFSQueuingFunction();

        LimitedDepthSearchQueuingFunction(int depthLimit) {
            this.depthLimit = depthLimit;
        }

        @Override
        public int getDepthLimit() { return depthLimit; }

        @Override
        public Queue<Node> insert(List<Node> expanded, Queue<Node> frontier) {
            return dfs.insert(expanded, frontier);
        }
    }
}
