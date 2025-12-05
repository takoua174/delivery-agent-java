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
        return "<!DOCTYPE html>\n<html><head>\n<meta charset=\"UTF-8\">\n<title>Delivery Visualization</title>\n" +
               "<link rel=\"stylesheet\" href=\"https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css\">\n" +
               "<style>\n" + generateCSS(grid) + "</style>\n</head>\n<body>\n" +
               "<div class=\"container\">\n<h1><i class=\"fas fa-truck\"></i> Delivery Route Visualization</h1>\n" +
               "<div class=\"controls\">\n" +
               "<button id=\"nextBtn\" class=\"btn btn-primary\"><i class=\"fas fa-step-forward\"></i> Next Step</button>\n" +
               "<button id=\"playBtn\" class=\"btn btn-success\"><i class=\"fas fa-play\"></i> Auto Play</button>\n" +
               "<button id=\"pauseBtn\" class=\"btn btn-warning\" style=\"display:none;\"><i class=\"fas fa-pause\"></i> Pause</button>\n" +
               "<button id=\"resetBtn\" class=\"btn btn-secondary\"><i class=\"fas fa-redo\"></i> Reset</button>\n" +
               "<span class=\"speed-control\"><label>Speed: </label>\n" +
               "<input type=\"range\" id=\"speedSlider\" min=\"200\" max=\"2000\" value=\"1000\" step=\"200\">\n" +
               "<span id=\"speedValue\">1000ms</span></span>\n</div>\n" +
               "<div class=\"main-content\">\n<div class=\"grid-container\">\n" + generateGrid(grid) + "</div>\n" +
               "<div class=\"results-panel\">\n<h2>Search Results</h2>\n<div id=\"results\">\n" + 
               generateResults(grid, assignment, pairResults) + "</div>\n" +
               "<div class=\"legend\"><h3>Legend</h3>\n" +
               "<div class=\"legend-item\"><span class=\"legend-box store\"><i class=\"fas fa-store\"></i></span> Store</div>\n" +
               "<div class=\"legend-item\"><span class=\"legend-box customer\"><i class=\"fas fa-home\"></i></span> Customer</div>\n" +
               "<div class=\"legend-item\"><span class=\"legend-box tunnel\"><i class=\"fas fa-subway\"></i></span> Tunnel</div>\n" +
               "<div class=\"legend-item\"><span class=\"legend-box blocked\"><i class=\"fas fa-ban\"></i></span> Blocked</div>\n" +
               "<div class=\"legend-item\"><span class=\"legend-box truck\"><i class=\"fas fa-truck\"></i></span> Truck</div>\n" +
               "</div>\n</div>\n</div>\n</div>\n<script>\n" + 
               generateJavaScript(grid, assignment, pairResults, autoPlay) + 
               "</script>\n</body>\n</html>";
    }

    private static String generateCSS(Grid grid) {
        return "*{margin:0;padding:0;box-sizing:border-box}body{font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;" +
               "background:linear-gradient(135deg,#667eea 0%,#764ba2 100%);min-height:100vh;padding:20px}.container{" +
               "max-width:1400px;margin:0 auto;background:white;border-radius:15px;box-shadow:0 20px 60px rgba(0,0,0,0.3);" +
               "padding:30px}h1{color:#333;text-align:center;margin-bottom:30px;font-size:2.5em}h1 i{color:#667eea;" +
               "margin-right:15px}.controls{display:flex;justify-content:center;gap:15px;margin-bottom:30px;flex-wrap:wrap;" +
               "align-items:center}.btn{padding:12px 24px;border:none;border-radius:8px;font-size:16px;cursor:pointer;" +
               "transition:all 0.3s ease;font-weight:600;box-shadow:0 4px 6px rgba(0,0,0,0.1)}.btn:hover{" +
               "transform:translateY(-2px);box-shadow:0 6px 12px rgba(0,0,0,0.15)}.btn-primary{background:#667eea;color:white}" +
               ".btn-success{background:#10b981;color:white}.btn-warning{background:#f59e0b;color:white}" +
               ".btn-secondary{background:#6b7280;color:white}.speed-control{display:flex;align-items:center;gap:10px;" +
               "background:#f3f4f6;padding:8px 16px;border-radius:8px}.main-content{display:grid;" +
               "grid-template-columns:1fr 400px;gap:30px}.grid-container{background:#f9fafb;padding:20px;" +
               "border-radius:10px;overflow:auto}.grid{display:grid;grid-template-columns:repeat(" + grid.getWidth() + 
               ",60px);grid-template-rows:repeat(" + grid.getHeight() + ",60px);gap:2px;margin:0 auto;width:fit-content}" +
               ".cell{width:60px;height:60px;border:2px solid #d1d5db;border-radius:8px;display:flex;" +
               "align-items:center;justify-content:center;font-size:24px;background:white;transition:all 0.3s ease;" +
               "position:relative}.cell:hover{transform:scale(1.05);z-index:10}.cell.store{" +
               "background:linear-gradient(135deg,#3b82f6,#2563eb);color:white;box-shadow:0 4px 6px rgba(59,130,246,0.3)}" +
               ".cell.customer{background:linear-gradient(135deg,#10b981,#059669);color:white;" +
               "box-shadow:0 4px 6px rgba(16,185,129,0.3)}.cell.tunnel{background:linear-gradient(135deg,#6b7280,#4b5563);" +
               "color:white}.cell.blocked{background:linear-gradient(135deg,#ef4444,#dc2626);color:white}" +
               ".cell.truck{background:linear-gradient(135deg,#fbbf24,#f59e0b);color:white;animation:pulse 1s infinite;" +
               "box-shadow:0 0 20px rgba(251,191,36,0.6)}@keyframes pulse{0%,100%{transform:scale(1)}50%{transform:scale(1.1)}}" +
               ".cell-number{position:absolute;top:2px;right:4px;font-size:12px;font-weight:bold;" +
               "background:rgba(255,255,255,0.9);color:#333;padding:2px 6px;border-radius:4px}.results-panel{" +
               "background:#f9fafb;padding:20px;border-radius:10px;max-height:800px;overflow-y:auto}.results-panel h2{" +
               "color:#333;margin-bottom:20px;font-size:1.5em}#results{background:white;padding:15px;border-radius:8px;" +
               "font-family:'Courier New',monospace;font-size:14px;line-height:1.6;margin-bottom:20px;" +
               "box-shadow:0 2px 4px rgba(0,0,0,0.1)}.store-assignment{margin-bottom:15px;padding:10px;" +
               "background:#f3f4f6;border-radius:6px;border-left:4px solid #667eea}.store-assignment strong{" +
               "color:#667eea;font-size:16px}.route-info{margin-left:15px;margin-top:8px;color:#6b7280}" +
               ".total-cost{font-size:18px;font-weight:bold;color:#10b981;margin-top:15px;padding:10px;" +
               "background:#d1fae5;border-radius:6px;text-align:center}.legend{margin-top:20px}.legend h3{" +
               "margin-bottom:10px;color:#333}.legend-item{display:flex;align-items:center;margin-bottom:8px;gap:10px}" +
               ".legend-box{width:40px;height:40px;border-radius:6px;display:flex;align-items:center;" +
               "justify-content:center;font-size:18px}@media (max-width:1200px){.main-content{grid-template-columns:1fr}}";
    }

    private static String generateGrid(Grid grid) {
        StringBuilder html = new StringBuilder("<div class=\"grid\" id=\"grid\">\n");
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                Point cell = new Point(x, y);
                html.append("<div class=\"cell\" data-x=\"").append(x).append("\" data-y=\"").append(y).append("\"");
                List<Point> stores = grid.getStores();
                List<Point> customers = grid.getCustomers();
                if (stores.contains(cell)) {
                    int idx = stores.indexOf(cell);
                    html.append(" data-type=\"store\"><i class=\"fas fa-store\"></i><span class=\"cell-number\">")
                        .append(idx + 1).append("</span>");
                } else if (customers.contains(cell)) {
                    int idx = customers.indexOf(cell);
                    html.append(" data-type=\"customer\"><i class=\"fas fa-home\"></i><span class=\"cell-number\">")
                        .append(idx + 1).append("</span>");
                } else {
                    Tunnel tunnels = grid.getTunnelAt(cell);
                    if (tunnels != null) {
                        html.append(" data-type=\"tunnel\"><i class=\"fas fa-subway\"></i>");
                    } else if (isBlockedCell(grid, cell)) {
                        html.append(" data-type=\"blocked\"><i class=\"fas fa-ban\"></i>");
                    } else {
                        html.append(">");
                    }
                }
                html.append("</div>\n");
            }
        }
        html.append("</div>\n");
        return html.toString();
    }

    private static boolean isBlockedCell(Grid grid, Point cell) {
        for (Point neighbor : grid.getNeighbors(cell)) {
            if (grid.isBlocked(cell, neighbor)) return true;
        }
        return false;
    }

    private static String generateResults(Grid grid, Map<Integer, List<Integer>> assignment, 
                                         Map<Point, String> pairResults) {
        StringBuilder html = new StringBuilder();
        int totalCost = 0;
        for (Map.Entry<Integer, List<Integer>> entry : assignment.entrySet()) {
            int storeIdx = entry.getKey();
            List<Integer> custs = entry.getValue();
            html.append("<div class=\"store-assignment\"><strong>Store ").append(storeIdx + 1)
                .append(" → Customers ").append(custs).append("</strong>\n");
            int storeCost = 0;
            for (int custIdx : custs) {
                Point dest = grid.getCustomers().get(custIdx);
                String[] res = pairResults.get(dest).split(";");
                int cost = Integer.parseInt(res[1]);
                storeCost += cost;
                html.append("<div class=\"route-info\">Customer ").append(custIdx + 1)
                    .append(": Cost = ").append(cost).append("</div>\n");
            }
            html.append("<div class=\"route-info\" style=\"color:#10b981;font-weight:bold;\">Store Total: ")
                .append(storeCost).append("</div></div>\n");
            totalCost += storeCost;
        }
        html.append("<div class=\"total-cost\">Total Cost: ").append(totalCost).append("</div>\n");
        return html.toString();
    }

    private static String generateJavaScript(Grid grid, Map<Integer, List<Integer>> assignment, 
                                            Map<Point, String> pairResults, boolean autoPlay) {
        return "const gridData=" + generateGridJSON(grid) + ";const assignmentData=" + 
               generateAssignmentJSON(assignment, grid, pairResults) + ";let currentRouteIdx=0,currentStepIdx=0," +
               "isPlaying=false,playInterval=null,speed=1000;function resetGrid(){document.querySelectorAll('.cell')" +
               ".forEach(c=>{c.classList.remove('truck');const t=c.dataset.type;if(t)c.classList.add(t);const i=c.querySelector('i');" +
               "if(i&&i.classList.contains('fa-truck')){if(t==='store')i.className='fas fa-store';else if(t==='customer')" +
               "i.className='fas fa-home';else if(t==='tunnel')i.className='fas fa-subway';else if(t==='blocked')" +
               "i.className='fas fa-ban';else i.remove()}})}function getCellAt(x,y){return document.querySelector(" +
               "`.cell[data-x=\"${x}\"][data-y=\"${y}\"]`)}function applyAction(pos,action){let x=pos.x,y=pos.y;" +
               "if(action==='up')y--;else if(action==='down')y++;else if(action==='left')x--;else if(action==='right')x++;" +
               "else if(action==='tunnel'){const t=gridData.tunnels.filter(t=>(t.e1.x===x&&t.e1.y===y)||(t.e2.x===x&&t.e2.y===y));" +
               "if(t.length>0){const tu=t[0];if(tu.e1.x===x&&tu.e1.y===y){x=tu.e2.x;y=tu.e2.y}else{x=tu.e1.x;y=tu.e1.y}}}" +
               "return{x,y}}function nextStep(){if(currentRouteIdx>=assignmentData.length){stopPlay();alert('All routes completed!');" +
               "return}const r=assignmentData[currentRouteIdx];if(currentStepIdx>=r.steps.length){currentRouteIdx++;" +
               "currentStepIdx=0;resetGrid();if(currentRouteIdx<assignmentData.length)nextStep();return}" +
               "const s=r.steps[currentStepIdx];const c=getCellAt(s.x,s.y);if(c){c.classList.add('truck');" +
               "const i=c.querySelector('i');if(i)i.className='fas fa-truck';else c.innerHTML='<i class=\"fas fa-truck\"></i>'+c.innerHTML}" +
               "if(currentStepIdx>0){const ps=r.steps[currentStepIdx-1];const pc=getCellAt(ps.x,ps.y);if(pc){" +
               "pc.classList.remove('truck');const t=pc.dataset.type;const i=pc.querySelector('i');if(i&&t){" +
               "if(t==='store')i.className='fas fa-store';else if(t==='customer')i.className='fas fa-home';" +
               "else if(t==='tunnel')i.className='fas fa-subway';else if(t==='blocked')i.className='fas fa-ban'}}}" +
               "currentStepIdx++}function startPlay(){if(isPlaying)return;isPlaying=true;" +
               "document.getElementById('playBtn').style.display='none';document.getElementById('pauseBtn').style.display='inline-block';" +
               "playInterval=setInterval(nextStep,speed)}function stopPlay(){isPlaying=false;" +
               "document.getElementById('playBtn').style.display='inline-block';document.getElementById('pauseBtn').style.display='none';" +
               "if(playInterval){clearInterval(playInterval);playInterval=null}}function reset(){stopPlay();currentRouteIdx=0;" +
               "currentStepIdx=0;resetGrid()}document.getElementById('nextBtn').addEventListener('click',nextStep);" +
               "document.getElementById('playBtn').addEventListener('click',startPlay);" +
               "document.getElementById('pauseBtn').addEventListener('click',stopPlay);" +
               "document.getElementById('resetBtn').addEventListener('click',reset);" +
               "document.getElementById('speedSlider').addEventListener('input',e=>{speed=parseInt(e.target.value);" +
               "document.getElementById('speedValue').textContent=speed+'ms';if(isPlaying){stopPlay();startPlay()}});" +
               (autoPlay ? "setTimeout(startPlay,500);" : "");
    }

    private static String generateGridJSON(Grid grid) {
        StringBuilder json = new StringBuilder("{width:").append(grid.getWidth()).append(",height:")
            .append(grid.getHeight()).append(",tunnels:[");
        for (Tunnel t : grid.getTunnels()) {
            json.append("{e1:{x:").append(t.getEntrance1().getX()).append(",y:").append(t.getEntrance1().getY())
                .append("},e2:{x:").append(t.getEntrance2().getX()).append(",y:").append(t.getEntrance2().getY())
                .append("}},");
        }
        if (!grid.getTunnels().isEmpty()) json.setLength(json.length() - 1);
        json.append("]}");
        return json.toString();
    }

    private static String generateAssignmentJSON(Map<Integer, List<Integer>> assignment, Grid grid, 
                                                 Map<Point, String> pairResults) {
        StringBuilder json = new StringBuilder("[");
        for (Map.Entry<Integer, List<Integer>> entry : assignment.entrySet()) {
            int storeIdx = entry.getKey();
            Point store = grid.getStores().get(storeIdx);
            for (int custIdx : entry.getValue()) {
                Point cust = grid.getCustomers().get(custIdx);
                String result = pairResults.get(cust);
                String[] res = result.split(";");
                String[] actions = res[0].split(",");
                json.append("{storeIdx:").append(storeIdx).append(",custIdx:").append(custIdx)
                    .append(",steps:[{x:").append(store.getX()).append(",y:").append(store.getY()).append("}");
                Point pos = store;
                for (String action : actions) {
                    pos = applyActionForJSON(pos, action, grid);
                    json.append(",{x:").append(pos.getX()).append(",y:").append(pos.getY()).append("}");
                }
                json.append("]},");
            }
        }
        if (json.length() > 1) json.setLength(json.length() - 1);
        json.append("]");
        return json.toString();
    }

    private static Point applyActionForJSON(Point pos, String action, Grid grid) {
        int dx = 0, dy = 0;
        if (action.equals("up")) dy = -1;
        else if (action.equals("down")) dy = 1;
        else if (action.equals("left")) dx = -1;
        else if (action.equals("right")) dx = 1;
        else if (action.equals("tunnel")) {
            Tunnel tunnel = grid.getTunnelAt(pos);
            if (tunnel != null) return tunnel.getOtherEnd(pos);
        }
        return new Point(pos.getX() + dx, pos.getY() + dy);
    }

    private static void openInBrowser(String path) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + path);
            } else if (os.contains("mac")) {
                Runtime.getRuntime().exec("open " + path);
            } else if (os.contains("nix") || os.contains("nux")) {
                Runtime.getRuntime().exec("xdg-open " + path);
            }
        } catch (IOException e) {
            System.err.println("Could not open browser. Please open manually: " + path);
        }
    }
}