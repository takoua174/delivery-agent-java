package code.delivery;
import java.util.*;
import code.delivery.models.*;
import code.delivery.utils.GridGenerator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive Test Suite for Delivery Search Algorithms
 * 
 * Test Grid Layout (5x4):
 * ========================
 * Y-axis (top to bottom, 0-3):
 * 
        0       1       2       3       4
    ┌───────┬───────┬───────┬───────┬───────┐
    │       2       1       3       4       │
  0 │  [S]  │       │       │       │  [C1] │
    │   1   │   2   │   3   │   0   │   1   │
    ├───────┼───────┼───────┼───────┼───────┤
    │       1       3       0       2       │
  1 │       │       │       │       │       │
    │   3   │   1   │   2   │   1   │   3   │
    ├───────┼───────┼───────┼───────┼───────┤
    │       2       1       2       1       │
  2 │       │       │  [T1] │       │  [C2] │
    │   2   │   1   │   4   │   3   │   1   │
    ├───────┼───────┼───────┼───────┼───────┤
    │       3       2       2       4       │
  3 │       │       │  [T1] │       │       │
    └───────┴───────┴───────┴───────┴───────┘
 * 
 * Legend:
 * - [S]  = Store at (0,0)
 * - [C1] = Customer 1 at (4,0)
 * - [C2] = Customer 2 at (4,2)
 * - [T1] = Tunnel entrance at (2,2) <-> (2,3)
 * - -X-  = Blocked road (traffic level 0) between (2,1) and (3,1)
 * - Numbers on edges = Traffic cost (time to traverse)
 * 
 * Grid String Format:
 * m;n;P;S;stores;customers;tunnels;traffic_edges
 * 
 * Optimal Paths:
 * ==============
 * Store(0,0) to Customer1(4,0):
 * - Best Path: right,right,right,right (cost = 2+1+3+4 = 10)
 * - BFS finds this (shortest # of moves)
 * - UCS finds this (lowest cost)
 * - A* finds this quickly
 * 
 * Store(0,0) to Customer2(4,2):
 * - Best Path: "up,right,up,right,right,right,|RETURN|,left,left,left,down,left,down" (cost = 7)
 *   Alternative: right,right,up,up,right,right (cost = 2+1+3+2+2+1 = 11)
 * - UCS/A* should find the optimal 10-cost path
 * - Tunnel at (2,2)↔(2,3) doesn't help for this customer (moves away from goal) *
 * Algorithm Expected Behaviors:
 * ==============================
 * 1. BF (Breadth-First Search): 
 *    - Explores level by level
 *    - Finds shortest path in # of moves (not cost)
 *    - High node expansion
 * 
 * 2. DF (Depth-First Search):
 *    - Explores deeply before backtracking
 *    - May find suboptimal paths
 *    - May expand more nodes if goal is shallow or explores wrong branches first
 *   
 * 3. ID (Iterative Deepening):
 *    - Combines BFS completeness with DFS memory efficiency
 *    - Finds shortest path in # of moves
 *    - Re-expands nodes at each depth
 * 
 * 4. UC (Uniform Cost Search):
 *    - Expands lowest-cost nodes first
 *    - Guarantees optimal path by cost
 *    - More expansions than A*
 * 
 * 5. GR1 (Greedy with h1=Manhattan):
 *    - Uses only heuristic (no path cost)
 *    - Fast but may be suboptimal
 *    - Fewer expansions
 * 
 * 6. GR2 (Greedy with h2=):
 *    - More aggressive heuristic
 *    - Even faster, potentially less optimal
 * 
 * 7. AS1 (A* with h1=Manhattan):
 *    - f(n) = g(n) + h(n)
 *    - Optimal and efficient
 *    - Fewer expansions than UC
 * 
 * 8. AS2 (A* with h2=):
 *    - Very fast but may miss optimal path
 */
public class DeliverySearchTest {
    
    // Test grid string - carefully constructed to have known paths
    private static String testGridString;
    private static Grid testGrid;
    
    /**
     * Initialize the test grid before all tests
     * Grid: 5x4 with 1 store, 2 customers, 1 tunnel, mixed traffic costs
     */
    @BeforeAll
    public static void setUp() {
        // Grid format: m;n;P;S;stores;customers;tunnels;traffic
        // 5 width, 4 height, 2 customers, 1 store
        StringBuilder sb = new StringBuilder();
        sb.append("5;4;2;1;");  // Dimensions and counts
        sb.append("0,0;");       // Store at (0,0)
        sb.append("4,0,4,2;");   // Customer1 at (4,0), Customer2 at (4,2)
        sb.append("2,2,2,3;");   // Tunnel between (2,2) and (2,3)
        
        // Traffic edges (bidirectional):
        // Horizontal edges - Row 0
        sb.append("0,0,1,0,2;1,0,0,0,2;");  // (0,0)<->(1,0) traffic=2
        sb.append("1,0,2,0,1;2,0,1,0,1;");  // (1,0)<->(2,0) traffic=1
        sb.append("2,0,3,0,3;3,0,2,0,3;");  // (2,0)<->(3,0) traffic=3
        sb.append("3,0,4,0,4;4,0,3,0,4;");  // (3,0)<->(4,0) traffic=4
        
        // Horizontal edges - Row 1
        sb.append("0,1,1,1,1;1,1,0,1,1;");  // (0,1)<->(1,1) traffic=1
        sb.append("1,1,2,1,3;2,1,1,1,3;");  // (1,1)<->(2,1) traffic=3
        sb.append("2,1,3,1,0;3,1,2,1,0;");  // (2,1)<->(3,1) BLOCKED traffic=0
        sb.append("3,1,4,1,2;4,1,3,1,2;");  // (3,1)<->(4,1) traffic=2
        
        // Horizontal edges - Row 2
        sb.append("0,2,1,2,2;1,2,0,2,2;");  // (0,2)<->(1,2) traffic=2
        sb.append("1,2,2,2,1;2,2,1,2,1;");  // (1,2)<->(2,2) traffic=1
        sb.append("2,2,3,2,2;3,2,2,2,2;");  // (2,2)<->(3,2) traffic=2
        sb.append("3,2,4,2,1;4,2,3,2,1;");  // (3,2)<->(4,2) traffic=1
        
        // Horizontal edges - Row 3
        sb.append("0,3,1,3,3;1,3,0,3,3;");  // (0,3)<->(1,3) traffic=3
        sb.append("1,3,2,3,2;2,3,1,3,2;");  // (1,3)<->(2,3) traffic=2
        sb.append("2,3,3,3,2;3,3,2,3,2;");  // (2,3)<->(3,3) traffic=2
        sb.append("3,3,4,3,4;4,3,3,3,4;");  // (3,3)<->(4,3) traffic=4
        
        // Vertical edges - Column 0
        sb.append("0,0,0,1,1;0,1,0,0,1;");  // (0,0)<->(0,1) traffic=1
        sb.append("0,1,0,2,3;0,2,0,1,3;");  // (0,1)<->(0,2) traffic=3
        sb.append("0,2,0,3,2;0,3,0,2,2;");  // (0,2)<->(0,3) traffic=2
        
        // Vertical edges - Column 1
        sb.append("1,0,1,1,2;1,1,1,0,2;");  // (1,0)<->(1,1) traffic=2
        sb.append("1,1,1,2,1;1,2,1,1,1;");  // (1,1)<->(1,2) traffic=1
        sb.append("1,2,1,3,1;1,3,1,2,1;");  // (1,2)<->(1,3) traffic=1
        
        // Vertical edges - Column 2
        sb.append("2,0,2,1,3;2,1,2,0,3;");  // (2,0)<->(2,1) traffic=3
        sb.append("2,1,2,2,2;2,2,2,1,2;");  // (2,1)<->(2,2) traffic=2
        sb.append("2,2,2,3,4;2,3,2,2,4;");  // (2,2)<->(2,3) traffic=4 (tunnel also available)
        
        // Vertical edges - Column 3
        sb.append("3,0,3,1,0;3,1,3,0,0;");  // (3,0)<->(3,1) BLOCKED traffic=0
        sb.append("3,1,3,2,1;3,2,3,1,1;");  // (3,1)<->(3,2) traffic=1
        sb.append("3,2,3,3,3;3,3,3,2,3;");  // (3,2)<->(3,3) traffic=3
        
        // Vertical edges - Column 4
        sb.append("4,0,4,1,1;4,1,4,0,1;");  // (4,0)<->(4,1) traffic=1
        sb.append("4,1,4,2,3;4,2,4,1,3;");  // (4,1)<->(4,2) traffic=3
        sb.append("4,2,4,3,1;4,3,4,2,1;");  // (4,2)<->(4,3) traffic=1
        
        testGridString = sb.toString();
        testGrid = GridGenerator.parseGridString(testGridString);
        
    }
    // ==================== CUSTOMER 1 (4,0) TESTS (already correct) ====================
    /**
     * Test BFS (Breadth-First Search)
     * Expected: Finds path right,right,right,right
     * Should find shortest path in number of moves
     */
    @Test
    public void testBFS_StoreToCustomer1() {
        Point store = testGrid.getStores().get(0);      // (0,0)
        Point customer = testGrid.getCustomers().get(0); // (4,0)
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("BF");
        
        assertNotNull(result, "BFS should find a path");
        assertFalse(result.startsWith("NoPath"), "BFS should not return NoPath");
        
        String[] parts = result.split(";");
        String path = parts[0];
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        
        System.out.println("\n=== BFS Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + path);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        // BFS should find a path with 4 moves (right,right,right,right)
        String[] actions = path.split(",");
        int moveCount = 0;
        for (String action : actions) {
            if (!action.equals("|RETURN|") && action.equals("right") ) moveCount++;
            else break;
        }
        String expectedPath="right,right,right,right,|RETURN|,left,left,left,left";
        assertTrue(path.equals(expectedPath), "BFS path should match expected path");
        assertEquals(4, moveCount, "BFS should find 4-move path to customer1");
        assertTrue(cost > 0, "Path cost should be positive");
        assertTrue(nodesExpanded > 0, "Should expand some nodes");
    }
    
    /**
     * Test DFS (Depth-First Search)
     */
    @Test
    public void testDFS_StoreToCustomer1() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(0);
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("DF");
        
        assertNotNull(result, "DFS should find a path");
        assertFalse(result.startsWith("NoPath"), "DFS should not return NoPath");
        
        String[] parts = result.split(";");
        String path = parts[0];
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        String expectedPath="up,up,up,right,down,down,down,right,up,up,up,right,down,down,right,down,|RETURN|,up,left,up,up,left,down,down,down,left,up,up,up,left,down,down,down";//don't forget there is a blocked edge from (2,1) to (3,1)
        System.out.println("\n=== DFS Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        assertTrue(path.equals(expectedPath), "DFS path should match expected path");
        assertTrue(cost > 0, "Path cost should be positive");
    }

    /**
     * Test Iterative Deepening
     * Expected: Finds optimal path in number of moves like BFS
     * But uses less memory by doing depth-limited searches
     */
    @Test
    public void testID_StoreToCustomer1() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(0);
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("ID");
        
        assertNotNull(result, "ID should find a path");
        assertFalse(result.startsWith("NoPath"), "ID should not return NoPath");
        
        String[] parts = result.split(";");
        String path = parts[0];
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        String expectedPath="right,right,right,right,|RETURN|,left,left,left,left";
        
        System.out.println("\n=== ID Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        assertTrue(path.equals(expectedPath), "ID path should match expected path");
        assertTrue(cost > 0, "Path cost should be positive");
        assertTrue(nodesExpanded > 4, "ID should expand multiple nodes");
    }
    /**
     * Test Uniform Cost Search
     * Expected: Finds optimal path by cost
     */
    @Test
    public void testUCS_StoreToCustomer1() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(0);
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("UC");
        
        assertNotNull(result, "UCS should find a path");
        assertFalse(result.startsWith("NoPath"), "UCS should not return NoPath");
        
        String[] parts = result.split(";");
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        String path = parts[0];
        
        System.out.println("\n=== UCS Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        String expectedPath="right,right,right,right,|RETURN|,left,left,left,left";
        assertTrue(path.equals(expectedPath), "UCS path should match expected path");
        assertEquals(10, cost, "UCS should find optimal cost path (10)");
        assertTrue(nodesExpanded > 0, "Should expand some nodes");
    }
        /**
     * Test Greedy Search with h1 (Manhattan Distance)
     * Greedily moves toward goal
     */
    @Test
    public void testGreedy1_StoreToCustomer1() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(0);
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("GR1");
        
        assertNotNull(result, "GR1 should find a path");
        assertFalse(result.startsWith("NoPath"), "GR1 should not return NoPath");
        
        String[] parts = result.split(";");
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        String path = parts[0];
        String expectedPath="right,right,right,right,|RETURN|,left,left,left,left";
        
        System.out.println("\n=== Greedy-h1 Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        assertTrue(path.equals(expectedPath), "GR1 path should match expected path");
        assertTrue(cost > 0, "Path cost should be positive");
        assertTrue(nodesExpanded > 0, "Should expand some nodes");
    }
    /**
     * Test A* with h1 (Manhattan Distance)
     * Expected: Finds optimal path efficiently
     * f(n) = g(n) + h(n), h1 is admissible so guarantees optimality
     */
    @Test
    public void testAStar1_StoreToCustomer1() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(0);
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("AS1");
        
        assertNotNull(result, "A*-h1 should find a path");
        assertFalse(result.startsWith("NoPath"), "A*-h1 should not return NoPath");
        
        String[] parts = result.split(";");
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        
        System.out.println("\n=== A*-h1 Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);

        String path = parts[0];
        String expectedPath="right,right,right,right,|RETURN|,left,left,left,left";
        assertTrue(path.equals(expectedPath), "A*-h1 path should match expected path");
        
        // A* with admissible heuristic guarantees optimal
        assertEquals(10, cost, "A*1 should find optimal cost path (10)");
        // A* should expand fewer nodes than UCS
        assertTrue(nodesExpanded > 0, "Should expand some nodes");
    }

    // ==================== CUSTOMER 2 (4,2) TESTS ====================

    /**
     * Optimal path to Customer 2: down,down,right,right,right,right
     * Cost: 1 (down) + 3 (down) + 2 + 1 + 2 + 1 = 10
     */

    @Test void testBFS_StoreToCustomer2() {
        Point store = testGrid.getStores().get(0);
        Point cust = testGrid.getCustomers().get(1); // (4,2)
        DeliverySearch search = new DeliverySearch(testGrid, store, cust);
        String result = search.solve("BF");

        String[] p = result.split(";");
        String path = p[0];
        int cost = Integer.parseInt(p[1]);
        int nodesExpanded = Integer.parseInt(p[2]);
        String expectedPath="up,up,right,right,right,right,|RETURN|,left,left,left,left,down,down";

        System.out.println("\n=== BFS Test: Store(0,0) -> Customer2(4,2) ===");
        System.out.println("Path: " + path);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        assertTrue(path.equals(expectedPath), "BFS should find expected path to Customer2");
    }
    @Test void testUCS_StoreToCustomer2() {
        Point store = testGrid.getStores().get(0);
        Point cust = testGrid.getCustomers().get(1);
        DeliverySearch search = new DeliverySearch(testGrid, store, cust);
        String result = search.solve("UC");

        String[] p = result.split(";");
        int cost = Integer.parseInt(p[1]);
        int nodesExpanded = Integer.parseInt(p[2]);
        String path = p[0];
        System.out.println("\n=== UCS Test: Store(0,0) -> Customer2(4,2) ===");
        System.out.println("Path: " + path);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        String expectedPath="up,right,up,right,right,right,|RETURN|,left,left,left,down,left,down";
        assertTrue(path.equals(expectedPath), "UCS should find optimal path to Customer2");
        assertEquals(7, cost, "UCS must find optimal cost 7 to Customer 2");
    }
    @Test void testAStar1_StoreToCustomer2() {
        Point store = testGrid.getStores().get(0);
        Point cust = testGrid.getCustomers().get(1);
        DeliverySearch search = new DeliverySearch(testGrid, store, cust);
        String result = search.solve("AS1");

        String[] p = result.split(";");
        int cost = Integer.parseInt(p[1]);
        int nodes = Integer.parseInt(p[2]);
        int nodesExpanded = Integer.parseInt(p[2]);
        String path = p[0];
        System.out.println("\n=== A*1 Test: Store(0,0) -> Customer2(4,2) ===");
        System.out.println("Path: " + path);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        String expectedPath="up,right,up,right,right,right,|RETURN|,left,left,left,down,left,down";
        assertTrue(path.equals(expectedPath), "A*-h1 should find optimal path to Customer2");
        assertEquals(7, cost, "A*-h1 must find optimal cost 7  to Customer 2");
    }
    @Test void testAStar2_StoreToCustomer2() {
        Point store = testGrid.getStores().get(0);
        Point cust = testGrid.getCustomers().get(1); // (4,2)
        DeliverySearch search = new DeliverySearch(testGrid, store, cust);
        String result = search.solve("AS2");

        String[] p = result.split(";");
        int cost = Integer.parseInt(p[1]);
        int nodes = Integer.parseInt(p[2]);
        int nodesExpanded = Integer.parseInt(p[2]);
        String path=p[0];

        System.out.println("\n=== A*2 Test: Store(0,0) -> Customer2(4,2) ===");
        System.out.println("Path: " + path);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        String expectedPath="up,right,up,right,right,right,|RETURN|,left,left,left,down,left,down";
        assertTrue(path.equals(expectedPath), "A*-h2 should find optimal path to Customer2");
        assertEquals(7, cost, "A*-h2 must find optimal cost 7  to Customer 2");
    }    
    
    @Test
    public void testGreedy1_StoreToCustomer2() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(1);
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("GR1");
        
        assertNotNull(result, "GR1 should find a path");
        assertFalse(result.startsWith("NoPath"), "GR1 should not return NoPath");
        
        String[] parts = result.split(";");
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        String path = parts[0];
        
        System.out.println("\n=== Greedy-h1 Test: Store(0,0) -> Customer2(4,2) ===");
        System.out.println("Path: " + path);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        assertTrue(cost > 0, "Path cost should be positive");
        assertTrue(nodesExpanded > 0, "Should expand some nodes");
        System.out.println("Note: Greedy may not find optimal cost (7), but should be fast");
    }

    @Test
    public void testGreedy2_StoreToCustomer2() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(1);
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("GR2");
        
        assertNotNull(result, "GR2 should find a path");
        assertFalse(result.startsWith("NoPath"), "GR2 should not return NoPath");
        
        String[] parts = result.split(";");
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        String path = parts[0];
        
        System.out.println("\n=== Greedy-h2 Test: Store(0,0) -> Customer2(4,2) ===");
        System.out.println("Path: " + path);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        assertTrue(cost > 0, "Path cost should be possitive");
        assertTrue(nodesExpanded > 0, "Should expand some nodes");
        System.out.println("Note: Greedy-h2 with Euclidean may differ from GR1 in node expansion");
    }
    /**
     * Summary test that runs all algorithms and compares results
     * Measures: Cost, Nodes Expanded, Time, Memory
     */
    @Test
    public void testAllAlgorithms_Comparison() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(1);
        
        String[] algorithms = {"BF", "DF", "ID", "UC", "GR1", "GR2", "AS1", "AS2"};
        
        System.out.println("\n" + "=".repeat(100));
        System.out.println("COMPREHENSIVE ALGORITHM COMPARISON: Store(0,0) -> Customer2(4,2)");
        System.out.println("=".repeat(100));
        System.out.println(String.format("%-10s | %-6s | %-8s | %-12s | %-12s | %s", 
                                        "Strategy", "Cost", "Nodes", "Time (ms)", "Memory (KB)", "Path Preview"));
        System.out.println("-".repeat(100));
        
        Map<String, AlgorithmMetrics> results = new HashMap<>();
        
        for (String algo : algorithms) {
            Runtime runtime = Runtime.getRuntime();
            // Force garbage collection before each test for fair memory comparison 
            System.gc();
            System.gc(); // Call twice for better clearing
            try {
                Thread.sleep(200); // Longer wait for GC to complete
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            
            long memBefore = runtime.totalMemory() - runtime.freeMemory();
            long startTime = System.nanoTime();
            
            DeliverySearch search = new DeliverySearch(testGrid, store, customer);
            String result = search.solve(algo);
            
            long endTime = System.nanoTime();
    
            long memAfter = runtime.totalMemory() - runtime.freeMemory();
            
            assertNotNull(result, algo + " should find a path");
            assertFalse(result.startsWith("NoPath"), algo + " should not return NoPath");
            
            String[] parts = result.split(";");
            int cost = Integer.parseInt(parts[1]);
            int nodes = Integer.parseInt(parts[2]);
            long timeMs = (endTime - startTime) / 1_000_000;
            long memoryKB = Math.max(0, memAfter - memBefore) / 1024;
            String pathPreview = parts[0].substring(0, Math.min(35, parts[0].length()));
            
            AlgorithmMetrics metrics = new AlgorithmMetrics(algo, cost, nodes, timeMs, memoryKB, parts[0]);
            results.put(algo, metrics);
            
            System.out.println(String.format("%-10s | %-6d | %-8d | %-12d | %-12d | %s...", 
                                            algo, cost, nodes, timeMs, memoryKB, pathPreview));
        }
        
        // Analysis Section
        System.out.println("\n" + "=".repeat(100));
        System.out.println("ANALYSIS");
        System.out.println("=".repeat(100));
        
        // 1. Completeness Analysis
        System.out.println("\n1. COMPLETENESS (All algorithms should find a solution):");
        boolean allComplete = results.values().stream().allMatch(m -> m.cost > 0);
        System.out.println("All algorithms found a path: " + allComplete);
        
        // 2. Optimality Analysis
        System.out.println("\n2. OPTIMALITY (Which algorithms guarantee optimal solutions):");
        int optimalCost = results.get("UC").cost;
        System.out.println("   Optimal Cost (from UC): " + optimalCost);
        System.out.println("   - BF (shortest moves): Cost = " + results.get("BF").cost + 
                         (results.get("BF").cost == optimalCost ? " ✓ Optimal by cost" : " (optimal by # moves)"));
        System.out.println("   - UC (optimal cost): Cost = " + results.get("UC").cost + " ✓ Guaranteed optimal");
        System.out.println("   - AS1 (A* Manhattan): Cost = " + results.get("AS1").cost + 
                         (results.get("AS1").cost == optimalCost ? " ✓ Optimal (admissible heuristic)" : " ✗ Suboptimal"));
        System.out.println("   - AS2 (A* Euclidean): Cost = " + results.get("AS2").cost + 
                         (results.get("AS2").cost == optimalCost ? " ✓ Optimal (admissible heuristic)" : " ✗ Suboptimal"));
        System.out.println("   - DF (depth-first): Cost = " + results.get("DF").cost + " (not guaranteed optimal)");
        System.out.println("   - ID (iterative deep): Cost = " + results.get("ID").cost + " (optimal by # moves)");
        System.out.println("   - GR1 (greedy): Cost = " + results.get("GR1").cost + " (not guaranteed optimal)");
        System.out.println("   - GR2 (greedy): Cost = " + results.get("GR2").cost + " (not guaranteed optimal)");
        
        // 3. Node Expansion Efficiency
        System.out.println("\n3. NODE EXPANSION EFFICIENCY (Lower is better):");
        results.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.comparingInt(m -> m.nodesExpanded)))
            .forEach(e -> System.out.println(String.format("   %-10s: %6d nodes", e.getKey(), e.getValue().nodesExpanded)));
        
        System.out.println("\n   Key Insights:");
        System.out.println("   - Greedy algorithms (GR1, GR2) expand fewest nodes (fast but may be suboptimal)");
        System.out.println("   - A* algorithms balance optimality with efficiency (fewer nodes than UC)");
        System.out.println("   - BF explores all paths at each level (many nodes for optimal # moves)");
        System.out.println("   - DF may explore deeply before finding goal (variable performance)");
        
        // 4. Time Performance
        System.out.println("\n4. TIME PERFORMANCE (Execution time in milliseconds):");
        results.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.comparingLong(m -> m.timeMs)))
            .forEach(e -> System.out.println(String.format("   %-10s: %6d ms", e.getKey(), e.getValue().timeMs)));
        
        // 5. Memory Usage
        System.out.println("\n5. MEMORY USAGE (RAM in KB - approximate):");
        results.entrySet().stream()
            .sorted(Map.Entry.comparingByValue(Comparator.comparingLong(m -> m.memoryKB)))
            .forEach(e -> System.out.println(String.format("   %-10s: %6d KB", e.getKey(), e.getValue().memoryKB)));
        
        System.out.println("\n   Note: Memory measurements are approximate due to JVM garbage collection");
        System.out.println("   and memory management. DFS/ID typically use less memory (depth-limited).");
        
        // 6. Overall Recommendations
        System.out.println("\n6. RECOMMENDATIONS:");
        System.out.println("   + For OPTIMAL cost: Use UC or AS1/AS2 (A* is faster)");
        System.out.println("   + For SPEED: Use GR1 or GR2 (if suboptimal solutions acceptable)");
        System.out.println("   + For LOW MEMORY: Use DF or ID (depth-limited exploration)");
        System.out.println("   + For shortest # of MOVES: Use BF or ID");
        
        System.out.println("\n" + "=".repeat(100) + "\n");
        
        // Assertions for key properties
        assertEquals(optimalCost, results.get("AS1").cost, "A*1 should find optimal solution");
        assertTrue(results.get("AS1").nodesExpanded <= results.get("UC").nodesExpanded,
                  "A* should expand fewer or equal nodes than UC");
    }
    
    /**
     * Helper class to store algorithm performance metrics
     */
    private static class AlgorithmMetrics {
        String algorithm;
        int cost;
        int nodesExpanded;
        long timeMs;
        long memoryKB;
        String path;
        
        AlgorithmMetrics(String algorithm, int cost, int nodesExpanded, long timeMs, long memoryKB, String path) {
            this.algorithm = algorithm;
            this.cost = cost;
            this.nodesExpanded = nodesExpanded;
            this.timeMs = timeMs;
            this.memoryKB = memoryKB;
            this.path = path;
        }
    }
}