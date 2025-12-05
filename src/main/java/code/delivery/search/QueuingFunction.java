package code.search;

import java.util.List;
import java.util.Queue;

public interface QueuingFunction {
    Queue<Node> insert(List<Node> expanded, Queue<Node> frontier);
}