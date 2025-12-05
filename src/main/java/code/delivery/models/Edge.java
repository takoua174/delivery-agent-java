package code.delivery.models;

import java.util.Objects;

public class Edge {
    private final Point from, to;

    public Edge(Point from, Point to) {
        this.from = from;
        this.to = to;
    }

    public Point getFrom() { return from; }
    public Point getTo() { return to; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Edge edge = (Edge) o;
        return Objects.equals(from, edge.from) && Objects.equals(to, edge.to);
    }

    @Override
    public int hashCode() {
        return Objects.hash(from, to);
    }
}