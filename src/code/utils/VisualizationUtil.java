package code.delivery.utils;

import code.delivery.models.Grid;
import code.delivery.models.Point;
import code.delivery.models.Tunnel;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class VisualizationUtil {

    public static void showGridAndResults(Grid grid, Map<Integer, List<Integer>> assignment,
                                         Map<Point, String> pairResults, boolean autoPlay) {
        try {
            Path vizDir = Paths.get("visualization");
            if (!Files.exists(vizDir)) Files.createDirectories(vizDir);

            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
            String filename = "delivery_viz_" + timestamp + ".html";
            Path filePath = vizDir.resolve(filename);

            String htmlContent = generateHTML(grid, assignment, pairResults, autoPlay);
            Files.write(filePath, htmlContent.getBytes());

            System.out.println("Visualization saved to: " + filePath.toAbsolutePath());
            openInBrowser(filePath.toAbsolutePath().toString());
        } catch (IOException e) {
            System.err.println("Error creating visualization: " + e.getMessage());
        }
    }

    private static String generateHTML(Grid grid, Map<Integer, List<Integer>> assignment,
                                      Map<Point, String> pairResults, boolean autoPlay) {
        return """
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Delivery Route Visualization</title>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css">
    <style>
        %s
    </style>
</head>
<body>
    <div class="container">
        <h1>Delivery Route Visualization</h1>
        <div class="controls">
            <button id="nextBtn" class="btn btn-primary">Next Step</button>
            <button id="playBtn" class="btn btn-success">Play</button>
            <button id="pauseBtn" class="btn btn-warning" style="display:none;">Pause</button>
            <button id="resetBtn" class="btn btn-secondary">Reset</button>
            <span class="speed-control">
                <label>Speed: </label>
                <input type="range" id="speedSlider" min="200" max="2000" value="800" step="100">
                <span id="speedValue">800ms</span>
            </span>
        </div>

        <div class="main-content">
            <div class="grid-container">%s</div>
            <div class="results-panel">
                <h2>Select Path to Animate</h2>
                <div id="pathSelector">%s</div>
                <h3>Search Results</h3>
                <div id="results">%s</div>
                <div class="legend">
                    <h3>Legend</h3>
                    <div class="legend-item"><span class="legend-box store">Store</span> Store</div>
                    <div class="legend-item"><span class="legend-box customer">Customer</span> Customer</div>
                    <div class="legend-item"><span class="legend-box tunnel">Tunnel 1</span> Tunnel Entrance</div>
                    <div class="legend-item"><span class="legend-box blocked">Blocked</span> Blocked Road</div>
                    <div class="legend-item"><span class="legend-box truck">Truck</span> Truck</div>
                    <div class="legend-item"><span class="legend-box edge-label">3</span> Traffic Cost</div>
                </div>
            </div>
        </div>
    </div>

    <script>
        %s
    </script>
</body>
</html>
        """.formatted(
            generateCSS(grid),
            generateGrid(grid),
            generatePathSelector(assignment, grid, pairResults),
            generateResults(grid, assignment, pairResults),
            generateJavaScript(grid, assignment, pairResults, autoPlay)
        );
    }

    private static String generateCSS(Grid grid) {
        return """
* { margin:0; padding:0; box-sizing:border-box; }
body { font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;
       background:linear-gradient(135deg,#667eea 0%%,#764ba2 100%%); min-height:100vh; padding:20px; }
.container { max-width:1600px; margin:0 auto; background:white;
             border-radius:15px; box-shadow:0 20px 60px rgba(0,0,0,0.3); padding:30px; }
h1 { text-align:center; margin-bottom:30px; font-size:2.5em; color:#333; }

.controls { display:flex; justify-content:center; gap:15px; margin-bottom:30px; flex-wrap:wrap; align-items:center; }
.btn { padding:12px 24px; border:none; border-radius:8px; font-size:16px; cursor:pointer;
      transition:all 0.3s; font-weight:600; box-shadow:0 4px 6px rgba(0,0,0,0.1); }
.btn:hover { transform:translateY(-2px); box-shadow:0 6px 12px rgba(0,0,0,0.15); }
.btn-primary { background:#667eea; color:white; }
.btn-success { background:#10b981; color:white; }
.btn-warning { background:#f59e0b; color:white; }
.btn-secondary { background:#6b7280; color:white; }

.speed-control { display:flex; align-items:center; gap:10px; background:#f3f4f6; padding:8px 16px; border-radius:8px; }

.main-content { display:grid; grid-template-columns:1fr 420px; gap:30px; }
.grid-container { background:#f9fafb; padding:20px; border-radius:10px; overflow:auto; position:relative; }
.grid { display:grid; grid-template-columns:repeat(%d,60px); grid-template-rows:repeat(%d,60px);
        gap:2px; margin:0 auto; width:fit-content; position:relative; }
.cell { width:60px; height:60px; border:2px solid #d1d5db; border-radius:8px;
        display:flex; align-items:center; justify-content:center; font-size:24px;
        background:white; transition:all 0.3s; position:relative; }
.cell:hover { transform:scale(1.05); z-index:10; }

.cell.store { background:linear-gradient(135deg,#3b82f6,#2563eb); color:white; }
.cell.customer { background:linear-gradient(135deg,#10b981,#059669); color:white; }
.cell.tunnel { background:linear-gradient(135deg,#6b7280,#4b5563); color:white; }
.cell.blocked { background:linear-gradient(135deg,#ef4444,#dc2626); color:white; }
.cell.truck { background:linear-gradient(135deg,#fbbf24,#f59e0b); color:white;
              animation:pulse 1s infinite; box-shadow:0 0 20px rgba(251,191,36,0.6); }
@keyframes pulse { 0%%,100%% {transform:scale(1)} 50%% {transform:scale(1.1)} }

.tunnel-number { position:absolute; bottom:4px; right:4px; background:#ef4444; color:white;
                 font-size:10px; padding:2px 5px; border-radius:50%%; font-weight:bold; }
.cell-number { position:absolute; top:2px; right:4px; font-size:12px; font-weight:bold;
               background:rgba(255,255,255,0.9); color:#333; padding:2px 6px; border-radius:4px; }

.edge-overlay { position:absolute; top:0; left:0; width:100%%; height:100%%; pointer-events:none; }
.edge { stroke:#ccc; stroke-width:2; }
.edge-blocked { stroke:#ef4444; stroke-dasharray:6,6; }
.edge-label { position:absolute; font-size:11px; font-weight:bold; background:rgba(255,255,255,0.9);
              padding:2px 5px; border-radius:4px; pointer-events:none; }

.path-line { stroke-width:5; stroke-linecap:round; pointer-events:none; }
.path-forward { stroke:#10b981; }
.path-return { stroke:#3b82f6; }

.results-panel { background:#f9fafb; padding:20px; border-radius:10px; max-height:800px; overflow-y:auto; }
#pathSelector { background:#f3f4f6; padding:15px; border-radius:8px; margin-bottom:20px; max-height:300px; overflow-y:auto; }
.path-btn { display:block; width:100%%; margin-bottom:8px; padding:12px; border:none;
            background:#e5e7eb; border-radius:6px; cursor:pointer; text-align:left; transition:0.2s; }
.path-btn:hover { background:#d1d5db; }
.path-btn.selected { background:#10b981; color:white; }

#results { background:white; padding:15px; border-radius:8px; font-family:'Courier New',monospace;
           font-size:14px; line-height:1.6; margin-bottom:20px; box-shadow:0 2px 4px rgba(0,0,0,0.1); }
.store-assignment { margin-bottom:15px; padding:10px; background:#f3f4f6; border-radius:6px;
                    border-left:4px solid #667eea; }
.total-cost { font-size:18px; font-weight:bold; color:#10b981; margin-top:15px;
              padding:12px; background:#d1fae5; border-radius:6px; text-align:center; }

.legend { margin-top:20px; }
.legend-item { display:flex; align-items:center; margin-bottom:8px; gap:10px; }
.legend-box { width:40px; height:40px; border-radius:6px; display:flex;
              align-items:center; justify-content:center; font-size:18px; }

@media (max-width:1200px) { .main-content { grid-template-columns:1fr; } }
""".formatted(grid.getWidth(), grid.getHeight());
    }

    private static String generateGrid(Grid grid) {
        StringBuilder html = new StringBuilder("<div class=\"grid\" id=\"grid\">\n");
        Map<Tunnel, Integer> tunnelIds = new HashMap<>();
        final int[] counter = {1};

        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                Point p = new Point(x, y);
                html.append("<div class=\"cell\" data-x=\"").append(x).append("\" data-y=\"").append(y).append("\"");

                if (grid.getStores().contains(p)) {
                    int idx = grid.getStores().indexOf(p);
                    html.append(" data-type=\"store\"><i class=\"fas fa-store\"></i>")
                        .append("<span class=\"cell-number\">").append(idx + 1).append("</span>");
                } else if (grid.getCustomers().contains(p)) {
                    int idx = grid.getCustomers().indexOf(p);
                    html.append(" data-type=\"customer\"><i class=\"fas fa-home\"></i>")
                        .append("<span class=\"cell-number\">").append(idx + 1).append("</span>");
                } else {
                    Tunnel t = grid.getTunnelAt(p);
                    if (t != null) {
                        int id = tunnelIds.computeIfAbsent(t, k -> counter[0]++);
                        html.append(" data-type=\"tunnel\"><i class=\"fas fa-subway\"></i>")
                            .append("<span class=\"tunnel-number\">").append(id).append("</span>");
                    } else if (isBlockedCell(grid, p)) {
                        html.append(" data-type=\"blocked\"><i class=\"fas fa-ban\"></i>");
                    } else {
                        html.append(">");
                    }
                }
                html.append("</div>\n");
            }
        }

        int w = grid.getWidth() * 62;
        int h = grid.getHeight() * 62;
        html.append("<svg class=\"edge-overlay\" width=\"").append(w).append("\" height=\"").append(h).append("\">\n");

        // Horizontal edges
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth() - 1; x++) {
                Point from = new Point(x, y);
                Point to = new Point(x + 1, y);
                int cost = grid.getTrafficCost(from, to);
                boolean blocked = cost == 0;
                int cx = x * 62 + 31;
                int cy = y * 62 + 31;
                html.append("<line x1=\"").append(cx).append("\" y1=\"").append(cy)
                    .append("\" x2=\"").append(cx + 62).append("\" y2=\"").append(cy)
                    .append("\" class=\"edge").append(blocked ? " edge-blocked" : "").append("\"/>");
                if (blocked) {
                    html.append("<text x=\"").append(cx + 31).append("\" y=\"").append(cy + 5)
                        .append("\" class=\"edge-label\"><i class=\"fas fa-ban\" style=\"color:red\"></i></text>");
                } else if (cost > 0) {
                    html.append("<text x=\"").append(cx + 31).append("\" y=\"").append(cy + 5)
                        .append("\" class=\"edge-label\">").append(cost).append("</text>");
                }
            }
        }

        // Vertical edges
        for (int x = 0; x < grid.getWidth(); x++) {
            for (int y = 0; y < grid.getHeight() - 1; y++) {
                Point from = new Point(x, y);
                Point to = new Point(x, y + 1);
                int cost = grid.getTrafficCost(from, to);
                boolean blocked = cost == 0;
                int cx = x * 62 + 31;
                int cy = y * 62 + 31;
                html.append("<line x1=\"").append(cx).append("\" y1=\"").append(cy)
                    .append("\" x2=\"").append(cx).append("\" y2=\"").append(cy + 62)
                    .append("\" class=\"edge").append(blocked ? " edge-blocked" : "").append("\"/>");
                if (blocked) {
                    html.append("<text x=\"").append(cx + 5).append("\" y=\"").append(cy + 36)
                        .append("\" class=\"edge-label\"><i class=\"fas fa-ban\" style=\"color:red\"></i></text>");
                } else if (cost > 0) {
                    html.append("<text x=\"").append(cx + 5).append("\" y=\"").append(cy + 36)
                        .append("\" class=\"edge-label\">").append(cost).append("</text>");
                }
            }
        }

        html.append("</svg></div>\n");
        return html.toString();
    }

    // Fixed: Added missing method
    private static boolean isBlockedCell(Grid grid, Point cell) {
        // A cell is "blocked" if all its outgoing edges are blocked
        int blockedCount = 0;
        Point[] dirs = { new Point(1,0), new Point(-1,0), new Point(0,1), new Point(0,-1) };
        for (Point d : dirs) {
            Point neighbor = new Point(cell.getX() + d.getX(), cell.getY() + d.getY());
            if (grid.isValidPosition(neighbor) && grid.getTrafficCost(cell, neighbor) == 0) {
                blockedCount++;
            }
        }
        return blockedCount == 4; // isolated cell
    }

    // The rest of the methods are unchanged from the working version
    private static String generatePathSelector(Map<Integer, List<Integer>> assignment, Grid grid, Map<Point, String> pairResults) {
        StringBuilder sb = new StringBuilder();
        for (var entry : assignment.entrySet()) {
            int storeIdx = entry.getKey();
            for (int custIdx : entry.getValue()) {
                Point cust = grid.getCustomers().get(custIdx);
                String[] parts = pairResults.get(cust).split(";");
                int cost = Integer.parseInt(parts[1]);
                sb.append("<button class=\"path-btn\" data-route-id=\"")
                  .append(storeIdx).append("-").append(custIdx)
                  .append("\">Store ").append(storeIdx + 1)
                  .append(" to Customer ").append(custIdx + 1)
                  .append(" (Cost: ").append(cost).append(")</button>\n");
            }
        }
        return sb.toString();
    }

    private static String generateResults(Grid grid, Map<Integer, List<Integer>> assignment, Map<Point, String> pairResults) {
        StringBuilder sb = new StringBuilder();
        int totalCost = 0;
        for (var entry : assignment.entrySet()) {
            int storeIdx = entry.getKey();
            var custs = entry.getValue();
            sb.append("<div class=\"store-assignment\"><strong>Store ").append(storeIdx + 1)
              .append(" to Customers ").append(custs).append("</strong><br>");
            int storeCost = 0;
            for (int custIdx : custs) {
                Point dest = grid.getCustomers().get(custIdx);
                String[] parts = pairResults.get(dest).split(";");
                int cost = Integer.parseInt(parts[1]);
                storeCost += cost;
                sb.append("<div class=\"route-info\">Customer ").append(custIdx + 1)
                  .append(": Cost = ").append(cost).append("</div>");
            }
            sb.append("<div class=\"route-info\" style=\"color:#10b981;font-weight:bold;\">Store Total: ")
              .append(storeCost).append("</div></div>");
            totalCost += storeCost;
        }
        sb.append("<div class=\"total-cost\">Total Cost: ").append(totalCost).append("</div>");
        return sb.toString();
    }

    private static String generateJavaScript(Grid grid, Map<Integer, List<Integer>> assignment,
                                            Map<Point, String> pairResults, boolean autoPlay) {
        return """
const routes = %s;
let currentRoute = null;
let currentStep = 0;
let isPlaying = false;
let interval = null;
let speed = 800;

function resetGrid() {
    document.querySelectorAll('.cell').forEach(c => {
        c.classList.remove('truck', 'path-forward', 'path-return');
        const type = c.dataset.type;
        if (type) c.classList.add(type);
    });
    document.querySelectorAll('.path-line').forEach(l => l.remove());
}

function getCell(x, y) {
    return document.querySelector(`.cell[data-x="${x}"][data-y="${y}"]`);
}

function drawLine(from, to, isReturn) {
    const svg = document.querySelector('.edge-overlay');
    const line = document.createElementNS('http://www.w3.org/2000/svg', 'line');
    line.setAttribute('x1', from.x * 62 + 31);
    line.setAttribute('y1', from.y * 62 + 31);
    line.setAttribute('x2', to.x * 62 + 31);
    line.setAttribute('y2', to.y * 62 + 31);
    line.classList.add('path-line', isReturn ? 'path-return' : 'path-forward');
    svg.appendChild(line);
}

function showStep() {
    if (!currentRoute || currentStep >= currentRoute.steps.length) {
        stop();
        return;
    }

    const step = currentRoute.steps[currentStep];
    const cell = getCell(step.x, step.y);
    if (cell) {
        cell.classList.add('truck');
        const icon = cell.querySelector('i') || document.createElement('i');
        icon.className = 'fas fa-truck';
        if (!cell.querySelector('i')) cell.appendChild(icon);
    }

    if (currentStep > 0) {
        const prev = currentRoute.steps[currentStep - 1];
        const isReturn = currentStep > currentRoute.forwardSteps;
        drawLine(prev, step, isReturn);
    }

    currentStep++;
}

function play() {
    if (isPlaying || !currentRoute) return;
    isPlaying = true;
    document.getElementById('playBtn').style.display = 'none';
    document.getElementById('pauseBtn').style.display = 'inline-block';
    interval = setInterval(showStep, speed);
}

function stop() {
    isPlaying = false;
    clearInterval(interval);
    document.getElementById('playBtn').style.display = 'inline-block';
    document.getElementById('pauseBtn').style.display = 'none';
}

function reset() {
    stop();
    currentStep = 0;
    resetGrid();
    currentRoute = null;
    document.querySelectorAll('.path-btn').forEach(b => b.classList.remove('selected'));
}

// Button listeners
document.getElementById('nextBtn').addEventListener('click', () => currentRoute && showStep());
document.getElementById('playBtn').addEventListener('click', play);
document.getElementById('pauseBtn').addEventListener('click', stop);
document.getElementById('resetBtn').addEventListener('click', reset);
document.getElementById('speedSlider').addEventListener('input', (e) => {
    speed = parseInt(e.target.value);
    document.getElementById('speedValue').textContent = speed + 'ms';
    if (isPlaying) { stop(); play(); }
});

// Path selection
document.querySelectorAll('.path-btn').forEach(btn => {
    btn.addEventListener('click', () => {
        reset();
        const id = btn.dataset.routeId;
        currentRoute = routes.find(r => r.id === id);
        if (currentRoute) {
            document.querySelectorAll('.path-btn').forEach(b => b.classList.remove('selected'));
            btn.classList.add('selected');
            showStep();
        }
    });
});

%s
""".formatted(
    generateRoutesJSON(assignment, grid, pairResults),
    autoPlay ? "setTimeout(() => document.querySelector('.path-btn')?.click(), 500);" : ""
);
    }

    private static String generateRoutesJSON(Map<Integer, List<Integer>> assignment, Grid grid, Map<Point, String> pairResults) {
        StringBuilder json = new StringBuilder("[");
        boolean first = true;
        for (var entry : assignment.entrySet()) {
            int storeIdx = entry.getKey();
            Point store = grid.getStores().get(storeIdx);
            for (int custIdx : entry.getValue()) {
                Point cust = grid.getCustomers().get(custIdx);
                String result = pairResults.get(cust);
                String[] parts = result.split(";");
                String[] actions = parts[0].split(",");

                List<Point> steps = new ArrayList<>();
                steps.add(store);
                Point pos = store;

                int forwardCount = 0;
                for (String a : actions) {
                    if (a.equals("|RETURN|")) break;
                    pos = applyAction(pos, a, grid);
                    steps.add(pos);
                    forwardCount++;
                }

                // Add return path
                for (int i = steps.size() - 2; i >= 0; i--) {
                    steps.add(steps.get(i));
                }

                if (!first) json.append(",");
                json.append(String.format(
                    "{\"id\":\"%d-%d\",\"forwardSteps\":%d,\"steps\":[", storeIdx, custIdx, forwardCount
                ));
                for (int i = 0; i < steps.size(); i++) {
                    Point p = steps.get(i);
                    json.append(String.format("{\"x\":%d,\"y\":%d}", p.getX(), p.getY()));
                    if (i < steps.size() - 1) json.append(",");
                }
                json.append("]}");
                first = false;
            }
        }
        json.append("]");
        return json.toString();
    }

    private static Point applyAction(Point pos, String action, Grid grid) {
        int dx = 0, dy = 0;
        if (action.equals("up")) dy = 1;
        else if (action.equals("down")) dy = -1;
        else if (action.equals("left")) dx = -1;
        else if (action.equals("right")) dx = 1;
        else if (action.equals("tunnel")) {
            Tunnel t = grid.getTunnelAt(pos);
            if (t != null) return t.getOtherEnd(pos);
        }
        return new Point(pos.getX() + dx, pos.getY() + dy);
    }

    private static void openInBrowser(String path) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            Runtime rt = Runtime.getRuntime();
            if (os.contains("win")) {
                rt.exec("rundll32 url.dll,FileProtocolHandler " + path);
            } else if (os.contains("mac")) {
                rt.exec("open " + path);
            } else {
                rt.exec("xdg-open " + path);
            }
        } catch (Exception e) {
            System.err.println("Could not open browser: " + path);
        }
    }
}