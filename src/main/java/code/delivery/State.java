package code.delivery;

import code.delivery.models.Point;
import java.util.Objects;

public class State {
    private final Point currentLocation;

    public State(Point location) {
        this.currentLocation = location;
    }

    public Point getCurrentLocation() { return currentLocation; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        State state = (State) o;
        return Objects.equals(currentLocation, state.currentLocation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(currentLocation);
    }

    @Override
    public String toString() {
        return currentLocation.toString();
    }
}