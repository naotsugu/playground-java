package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class App {
    public static void main(String[] args) {
        List<String> org = List.of("a", "b", "c", "d", "e", "f");
        List<String> rev = List.of("d", "a", "c", "f", "e", "a");
        List<Change> changes = buildRevision(buildPath(org, rev));
        changes.forEach(System.out::println);
    }

    record PathNode(int i, int j, boolean snake, boolean bootstrap, PathNode prev) {
        PathNode() {
            this(0, -1, true, true, null);
        }
        PathNode(int i, int j, boolean snake, PathNode prev) {
            this(i, j, snake, false,
                snake ? prev : prev == null ? null : prev.prevSnake());
        }
        PathNode prevSnake() {
            return bootstrap ? null
                : (!snake && prev != null)
                    ? prev.prevSnake()
                    : this;
        }
    }

    record Change(Type type, int startOriginal, int endOriginal, int startRevised, int endRevised) { }
    enum Type { CHANGE, DELETE, INSERT }

    private static PathNode buildPath(final List<String> orig, final List<String> rev) {

        final int n = orig.size();
        final int m = rev.size();

        final int max = n + m + 1;
        final int size = 1 + 2 * max;
        final int middle = size / 2;
        final PathNode[] diagonal = new PathNode[size];

        diagonal[middle + 1] = new PathNode();
        for (int d = 0; d < max; d++) {
            for (int k = -d; k <= d; k += 2) {
                final int kMiddle = middle + k;
                final int kPlus = kMiddle + 1;
                final int kMinus = kMiddle - 1;
                PathNode prev;
                int i;

                if ((k == -d) || (k != d && diagonal[kMinus].i < diagonal[kPlus].i)) {
                    i = diagonal[kPlus].i;
                    prev = diagonal[kPlus];
                } else {
                    i = diagonal[kMinus].i + 1;
                    prev = diagonal[kMinus];
                }

                diagonal[kMinus] = null; // no longer used

                int j = i - k;

                PathNode node = new PathNode(i, j, false, prev);

                while (i < n && j < m && Objects.equals(orig.get(i), rev.get(j))) {
                    i++;
                    j++;
                }

                if (i != node.i) {
                    node = new PathNode(i, j, true, node);
                }

                diagonal[kMiddle] = node;

                if (i >= n && j >= m) {
                    return diagonal[kMiddle];
                }
            }
            diagonal[middle + d - 1] = null;
        }
        throw new IllegalStateException("could not find a diff path");
    }

    private static List<Change> buildRevision(PathNode path) {

        List<Change> changes = new ArrayList<>();
        if (path.snake) {
            path = path.prev;
        }
        while (path != null && path.prev != null && path.prev.j >= 0) {
            if (path.snake) {
                throw new IllegalStateException("illegal path");
            }
            int i = path.i;
            int j = path.j;

            path = path.prev;
            Type type = (path.i == i && path.j != j)
                ? Type.INSERT
                : (path.i != i && path.j == j)
                    ? Type.DELETE
                    : Type.CHANGE;
            changes.addFirst(new Change(type, path.i, i, path.j, j));

            if (path.snake) {
                path = path.prev;
            }
        }
        return changes;
    }
}
