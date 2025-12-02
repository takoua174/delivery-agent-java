package code.delivery.utils;

import code.delivery.DeliveryPlanner;  // For assignment map (if needed)
import code.delivery.models.Grid;
import code.delivery.models.Point;  // Your Point
import code.delivery.models.Tunnel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.*;
import java.util.List;  // For List
import java.util.Arrays;  // For Arrays.asList

public class VisualizationUtil {
    private static JFrame frame;
    private static JPanel gridPanel;
    private static JTextArea resultsArea;
    private static JButton nextButton;
    private static int currentStep = 0;
    private static List<String> currentPath;
    private static Point currentTruckPos;
    private static Grid currentGrid;
    private static Map<Integer, List<Integer>> currentAssignment;

    public static void showGridAndResults(Grid grid, Map<Integer, List<Integer>> assignment, Map<Point, String> pairResults, boolean autoPlay) {
        currentGrid = grid;
        currentAssignment = assignment;
        currentPath = null;

        frame = new JFrame("Delivery Visualization");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        // Grid Panel
        gridPanel = new JPanel(new GridLayout(grid.getHeight(), grid.getWidth(), 1, 1));
        renderStaticGrid(grid);

        // Controls
        JPanel controls = new JPanel();
        nextButton = new JButton("Next Step");
        nextButton.addActionListener(e -> animateStep(pairResults));
        controls.add(nextButton);
        JButton playButton = new JButton("Auto-Play");
        playButton.addActionListener(e -> autoPlay(1000));
        controls.add(playButton);

        // Results Area
        resultsArea = new JTextArea(10, 50);
        resultsArea.setEditable(false);
        resultsArea.setText(buildResultsText(assignment, pairResults));
        JScrollPane scroll = new JScrollPane(resultsArea);

        frame.add(gridPanel, BorderLayout.CENTER);
        frame.add(controls, BorderLayout.NORTH);
        frame.add(scroll, BorderLayout.SOUTH);

        frame.pack();
        frame.setVisible(true);

        if (autoPlay) {
            autoPlay(2000);
        }
    }

    private static void renderStaticGrid(Grid grid) {
        gridPanel.removeAll();
        for (int y = 0; y < grid.getHeight(); y++) {
            for (int x = 0; x < grid.getWidth(); x++) {
                Point cell = new Point(x, y);
                JLabel label = new JLabel(" ", SwingConstants.CENTER);
                label.setPreferredSize(new Dimension(40, 40));
                label.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                // Static elements
                if (grid.getStores().contains(cell)) {
                    label.setText("S");
                    label.setBackground(Color.BLUE);
                } else if (grid.getCustomers().contains(cell)) {
                    label.setText("C");
                    label.setBackground(Color.GREEN);
                } else {
                    // Tunnels
                    List<Tunnel> tunnelsAtCell = grid.getTunnelsAt(cell);
                    boolean isTunnel = !tunnelsAtCell.isEmpty();
                    if (isTunnel) {
                        label.setText("~");
                        label.setBackground(Color.GRAY);
                    } else {
                        label.setBackground(Color.WHITE);
                    }
                    // Blocks
                    if (isBlockedCell(grid, cell)) {
                        label.setText("#");
                        label.setBackground(Color.RED);
                    }
                }
                label.setOpaque(true);
                gridPanel.add(label);
            }
        }
        gridPanel.revalidate();
        gridPanel.repaint();
    }

    private static boolean isBlockedCell(Grid grid, Point cell) {
        for (Point neighbor : grid.getNeighbors(cell)) {  // Now defined
            if (grid.isBlocked(cell, neighbor)) return true;
        }
        return false;
    }

    private static void animateStep(Map<Point, String> pairResults) {
        if (currentPath == null || currentStep >= currentPath.size()) {
            if (currentAssignment.isEmpty()) return;
            Integer storeIdx = currentAssignment.keySet().iterator().next();
            List<Integer> custs = currentAssignment.get(storeIdx);
            if (custs.isEmpty()) return;
            Integer custIdx = custs.get(0);
            Point dest = currentGrid.getCustomers().get(custIdx);
            String result = pairResults.get(dest);
            String[] res = result.split(";");
            currentPath = Arrays.asList(res[0].split(","));
            currentTruckPos = currentGrid.getStores().get(storeIdx);
            currentStep = 0;
            currentAssignment.remove(storeIdx);
            nextButton.setText("Next Truck");
        }

        if (currentStep < currentPath.size()) {
            String action = currentPath.get(currentStep);
            currentTruckPos = applyAction(currentTruckPos, action);
            JLabel truckLabel = findCellLabel(currentTruckPos);
            truckLabel.setText("T");
            truckLabel.setBackground(Color.YELLOW);
            gridPanel.revalidate();
            gridPanel.repaint();
            currentStep++;
        }
    }

    private static Point applyAction(Point pos, String action) {
        int dx = 0, dy = 0;
        switch (action) {
            case "up": dy = -1; break;
            case "down": dy = 1; break;
            case "left": dx = -1; break;
            case "right": dx = 1; break;
            case "tunnel":
                List<Tunnel> tunnels = currentGrid.getTunnelsAt(pos);
                if (!tunnels.isEmpty()) {
                    Point other = tunnels.get(0).getOtherEnd(pos);
                    return other;
                }
        }
        return new Point(pos.getX() + dx, pos.getY() + dy);
    }

    private static JLabel findCellLabel(Point pos) {
        int index = pos.getY() * currentGrid.getWidth() + pos.getX();
        return (JLabel) gridPanel.getComponent(index);
    }

    private static void autoPlay(int delayMs) {
        javax.swing.Timer timer = new javax.swing.Timer(delayMs, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                animateStep(null);
                if (currentStep >= currentPath.size() && currentAssignment.isEmpty()) {
                    ((javax.swing.Timer) e.getSource()).stop();
                }
            }
        });
        timer.start();
    }

    private static String buildResultsText(Map<Integer, List<Integer>> assignment, Map<Point, String> pairResults) {
        StringBuilder text = new StringBuilder("Search Results:\n");
        for (Map.Entry<Integer, List<Integer>> entry : assignment.entrySet()) {
            text.append("Store ").append(entry.getKey() + 1).append(" -> Dests ").append(entry.getValue()).append(":\n");
            for (int custIdx : entry.getValue()) {
                Point dest = currentGrid.getCustomers().get(custIdx);
                text.append("  ").append(pairResults.get(dest)).append("\n");
            }
        }
        text.append("\nTotal Cost: calculated");
        return text.toString();
    }
}