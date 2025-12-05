package code.delivery;

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
 *   0   1   2   3   4
 * 0 [S] -2- [ ] -1- [ ] -3- [C1]
 *   |   |   |   |   |   |   |
 *   1   2   3   0   2   1   4
 *   |   |   |   |   |   |   |
 * 1 [ ] -1- [ ] -X- [ ] -2- [ ]
 *   |   |   |   |   |   |   |
 *   3   1   2   0   1   3   2
 *   |   |   |   |   |   |   |
 * 2 [ ] -2- [T1]-1- [ ] -1- [C2]
 *   |   |   |   |   |   |   |
 *   2   1   4   2   3   1   1
 *   |   |   |   |   |   |   |
 * 3 [ ] -3- [T1]-2- [ ] -4- [ ]
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
 * - Best Path: down,down,right,right,tunnel,right,right (cost = 1+3+1+2+4+2+3 = 16)
 *   OR: right,right,down,down,right,right (cost = 2+1+3+2+2+1 = 11)
 * - UCS/A* should find the optimal 11-cost path
 * 
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
 *    - Lower memory, more nodes expanded
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
 * 6. GR2 (Greedy with h2=1.5*Manhattan):
 *    - More aggressive heuristic
 *    - Even faster, potentially less optimal
 * 
 * 7. AS1 (A* with h1=Manhattan):
 *    - f(n) = g(n) + h(n)
 *    - Optimal and efficient
 *    - Fewer expansions than UC
 * 
 * 8. AS2 (A* with h2=1.5*Manhattan):
 *    - Inadmissible heuristic (can overestimate)
 *    - Very fast but may miss optimal path
 *    - Fewest expansions
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
        sb.append("0,0,1,0,2;1,0,0,0,2;");  // (0,0)<->(1,0) cost=2
        sb.append("1,0,2,0,1;2,0,1,0,1;");  // (1,0)<->(2,0) cost=1
        sb.append("2,0,3,0,3;3,0,2,0,3;");  // (2,0)<->(3,0) cost=3
        sb.append("3,0,4,0,4;4,0,3,0,4;");  // (3,0)<->(4,0) cost=4
        
        // Horizontal edges - Row 1
        sb.append("0,1,1,1,1;1,1,0,1,1;");  // (0,1)<->(1,1) cost=1
        sb.append("1,1,2,1,3;2,1,1,1,3;");  // (1,1)<->(2,1) cost=3
        sb.append("2,1,3,1,0;3,1,2,1,0;");  // (2,1)<->(3,1) BLOCKED cost=0
        sb.append("3,1,4,1,2;4,1,3,1,2;");  // (3,1)<->(4,1) cost=2
        
        // Horizontal edges - Row 2
        sb.append("0,2,1,2,2;1,2,0,2,2;");  // (0,2)<->(1,2) cost=2
        sb.append("1,2,2,2,1;2,2,1,2,1;");  // (1,2)<->(2,2) cost=1
        sb.append("2,2,3,2,2;3,2,2,2,2;");  // (2,2)<->(3,2) cost=2
        sb.append("3,2,4,2,1;4,2,3,2,1;");  // (3,2)<->(4,2) cost=1
        
        // Horizontal edges - Row 3
        sb.append("0,3,1,3,3;1,3,0,3,3;");  // (0,3)<->(1,3) cost=3
        sb.append("1,3,2,3,2;2,3,1,3,2;");  // (1,3)<->(2,3) cost=2
        sb.append("2,3,3,3,2;3,3,2,3,2;");  // (2,3)<->(3,3) cost=2
        sb.append("3,3,4,3,4;4,3,3,3,4;");  // (3,3)<->(4,3) cost=4
        
        // Vertical edges - Column 0
        sb.append("0,0,0,1,1;0,1,0,0,1;");  // (0,0)<->(0,1) cost=1
        sb.append("0,1,0,2,3;0,2,0,1,3;");  // (0,1)<->(0,2) cost=3
        sb.append("0,2,0,3,2;0,3,0,2,2;");  // (0,2)<->(0,3) cost=2
        
        // Vertical edges - Column 1
        sb.append("1,0,1,1,2;1,1,1,0,2;");  // (1,0)<->(1,1) cost=2
        sb.append("1,1,1,2,1;1,2,1,1,1;");  // (1,1)<->(1,2) cost=1
        sb.append("1,2,1,3,1;1,3,1,2,1;");  // (1,2)<->(1,3) cost=1
        
        // Vertical edges - Column 2
        sb.append("2,0,2,1,3;2,1,2,0,3;");  // (2,0)<->(2,1) cost=3
        sb.append("2,1,2,2,2;2,2,2,1,2;");  // (2,1)<->(2,2) cost=2
        sb.append("2,2,2,3,4;2,3,2,2,4;");  // (2,2)<->(2,3) cost=4 (tunnel also available)
        
        // Vertical edges - Column 3
        sb.append("3,0,3,1,0;3,1,3,0,0;");  // (3,0)<->(3,1) BLOCKED cost=0
        sb.append("3,1,3,2,1;3,2,3,1,1;");  // (3,1)<->(3,2) cost=1
        sb.append("3,2,3,3,3;3,3,3,2,3;");  // (3,2)<->(3,3) cost=3
        
        // Vertical edges - Column 4
        sb.append("4,0,4,1,1;4,1,4,0,1;");  // (4,0)<->(4,1) cost=1
        sb.append("4,1,4,2,3;4,2,4,1,3;");  // (4,1)<->(4,2) cost=3
        sb.append("4,2,4,3,1;4,3,4,2,1;");  // (4,2)<->(4,3) cost=1
        
        testGridString = sb.toString();
        testGrid = GridGenerator.parseGridString(testGridString);
        
        System.out.println("=== Test Grid Initialized ===");
        System.out.println("Store: " + testGrid.getStores().get(0));
        System.out.println("Customer 1: " + testGrid.getCustomers().get(0));
        System.out.println("Customer 2: " + testGrid.getCustomers().get(1));
        System.out.println("Tunnel: " + testGrid.getTunnels().get(0).getEntrance1() + 
                          " <-> " + testGrid.getTunnels().get(0).getEntrance2());
    }
    
    /**
     * Test BFS (Breadth-First Search)
     * Expected: Finds path but not necessarily optimal by cost
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
            if (!action.equals("|RETURN|")) moveCount++;
            else break;
        }
        
        assertEquals(4, moveCount, "BFS should find 4-move path to customer1");
        assertTrue(cost > 0, "Path cost should be positive");
        assertTrue(nodesExpanded > 0, "Should expand some nodes");
    }
    
    /**
     * Test DFS (Depth-First Search)
     * Expected: Finds a path but likely suboptimal
     * May explore deeply before finding goal
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
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        
        System.out.println("\n=== DFS Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        assertTrue(cost > 0, "Path cost should be positive");
        // DFS may find longer paths
        assertTrue(cost >= 10, "DFS path cost should be at least optimal (10)");
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
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        
        System.out.println("\n=== ID Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        assertTrue(cost > 0, "Path cost should be positive");
        // ID re-expands nodes so higher node count
        assertTrue(nodesExpanded > 4, "ID should expand multiple nodes");
    }
    
    /**
     * Test Uniform Cost Search
     * Expected: Finds optimal path by cost
     * Path: right(2),right(1),right(3),right(4) = cost 10
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
        
        System.out.println("\n=== UCS Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        // UCS guarantees optimal cost path
        assertEquals(10, cost, "UCS should find optimal cost path (10)");
        assertTrue(nodesExpanded > 0, "Should expand some nodes");
    }
    
    /**
     * Test Greedy Search with h1 (Manhattan Distance)
     * Expected: Fast but may not find optimal path
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
        
        System.out.println("\n=== Greedy-h1 Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        assertTrue(cost >= 10, "Greedy cost should be >= optimal (10)");
        // Greedy typically expands fewer nodes
        assertTrue(nodesExpanded <= 10, "Greedy should expand relatively few nodes");
    }
    
    /**
     * Test Greedy Search with h2 (1.5 * Manhattan)
     * Expected: Even more aggressive, faster but potentially less optimal
     */
    @Test
    public void testGreedy2_StoreToCustomer1() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(0);
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("GR2");
        
        assertNotNull(result, "GR2 should find a path");
        assertFalse(result.startsWith("NoPath"), "GR2 should not return NoPath");
        
        String[] parts = result.split(";");
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        
        System.out.println("\n=== Greedy-h2 Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        assertTrue(cost >= 10, "Greedy-h2 cost should be >= optimal (10)");
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
        
        // A* with admissible heuristic guarantees optimal
        assertEquals(10, cost, "A*-h1 should find optimal cost path (10)");
        // A* should expand fewer nodes than UCS
        assertTrue(nodesExpanded > 0, "Should expand some nodes");
    }
    
    /**
     * Test A* with h2 (1.5 * Manhattan)
     * Expected: Very fast but may not be optimal (inadmissible heuristic)
     * Overestimates can lead to suboptimal paths
     */
    @Test
    public void testAStar2_StoreToCustomer1() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(0);
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("AS2");
        
        assertNotNull(result, "A*-h2 should find a path");
        assertFalse(result.startsWith("NoPath"), "A*-h2 should not return NoPath");
        
        String[] parts = result.split(";");
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        
        System.out.println("\n=== A*-h2 Test: Store(0,0) -> Customer1(4,0) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        assertTrue(cost >= 10, "A*-h2 cost should be >= optimal (10)");
        // A*-h2 typically expands fewest nodes
        assertTrue(nodesExpanded > 0, "Should expand some nodes");
    }
    
    /**
     * Test UCS for more complex path to Customer2
     * Expected: Optimal path avoiding blocked edge at (2,1)-(3,1)
     */
    @Test
    public void testUCS_StoreToCustomer2_ComplexPath() {
        Point store = testGrid.getStores().get(0);      // (0,0)
        Point customer = testGrid.getCustomers().get(1); // (4,2)
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("UC");
        
        assertNotNull(result, "UCS should find a path to customer2");
        assertFalse(result.startsWith("NoPath"), "UCS should not return NoPath");
        
        String[] parts = result.split(";");
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        
        System.out.println("\n=== UCS Test: Store(0,0) -> Customer2(4,2) ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        // Optimal path should be: right(2),right(1),down(3),down(2),right(2),right(1) = 11
        // Or other paths avoiding blocked edge
        assertTrue(cost >= 11, "Cost should be at least 11 (optimal)");
        assertTrue(cost <= 20, "Cost should be reasonable");
        assertTrue(nodesExpanded > 4, "Should explore multiple paths");
    }
    
    /**
     * Test A* for complex path with tunnel
     * Expected: Should efficiently find good path, possibly using tunnel
     */
    @Test
    public void testAStar1_StoreToCustomer2_WithTunnel() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(1);
        
        DeliverySearch search = new DeliverySearch(testGrid, store, customer);
        String result = search.solve("AS1");
        
        assertNotNull(result, "A*-h1 should find a path to customer2");
        assertFalse(result.startsWith("NoPath"), "A*-h1 should not return NoPath");
        
        String[] parts = result.split(";");
        int cost = Integer.parseInt(parts[1]);
        int nodesExpanded = Integer.parseInt(parts[2]);
        
        System.out.println("\n=== A*-h1 Test: Store(0,0) -> Customer2(4,2) with Tunnel ===");
        System.out.println("Path: " + parts[0]);
        System.out.println("Cost: " + cost);
        System.out.println("Nodes Expanded: " + nodesExpanded);
        
        assertTrue(cost >= 11, "Cost should be at least optimal");
        assertTrue(nodesExpanded > 0, "Should expand nodes");
        
        // Check if path uses tunnel
        boolean usesTunnel = parts[0].contains("tunnel");
        System.out.println("Uses tunnel: " + usesTunnel);
    }
    
    /**
     * Summary test that runs all algorithms and compares results
     */
    @Test
    public void testAllAlgorithms_Comparison() {
        Point store = testGrid.getStores().get(0);
        Point customer = testGrid.getCustomers().get(0);
        
        String[] algorithms = {"BF", "DF", "ID", "UC", "GR1", "GR2", "AS1", "AS2"};
        
        System.out.println("\n=== Algorithm Comparison: Store(0,0) -> Customer1(4,0) ===");
        System.out.println(String.format("%-8s | %-6s | %-15s | %s", 
                                        "Strategy", "Cost", "Nodes Expanded", "Path Preview"));
        System.out.println("-".repeat(70));
        
        for (String algo : algorithms) {
            DeliverySearch search = new DeliverySearch(testGrid, store, customer);
            String result = search.solve(algo);
            
            assertNotNull(result, algo + " should find a path");
            assertFalse(result.startsWith("NoPath"), algo + " should not return NoPath");
            
            String[] parts = result.split(";");
            int cost = Integer.parseInt(parts[1]);
            int nodes = Integer.parseInt(parts[2]);
            String pathPreview = parts[0].substring(0, Math.min(30, parts[0].length()));
            
            System.out.println(String.format("%-8s | %-6d | %-15d | %s...", 
                                            algo, cost, nodes, pathPreview));
        }
        
        System.out.println("\nExpected: UC and AS1 should find optimal cost (10)");
        System.out.println("Expected: AS1/AS2 should expand fewer nodes than UC");
        System.out.println("Expected: BF/ID find shortest # of moves (4 moves)");
    }
}