package code.delivery.models;

public class Tunnel {
    private final Point entrance1, entrance2;

    public Tunnel(Point entrance1, Point entrance2) {
        this.entrance1 = entrance1;
        this.entrance2 = entrance2;
    }

    public int getCost() {
        return entrance1.manhattanDistance(entrance2);
    }

    public Point getOtherEnd(Point entrance) {
        return entrance.equals(entrance1) ? entrance2 : entrance1;
    }

    public boolean hasEntrance(Point p) {
        return p.equals(entrance1) || p.equals(entrance2);
    }

    public Point getEntrance1() { return entrance1; }
    public Point getEntrance2() { return entrance2; }
}