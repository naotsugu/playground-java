package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class App {
    public static void main(String[] args) {
        List<String> orig = List.of("a", "b", "c", "d", "e", "f");
        List<String> rev = List.of("d", "a", "c", "f", "e", "a");
        List<Change> changes = buildRevision(buildPath(orig, rev), orig, rev);
        changes.forEach(System.out::println);
    }

    record PathNode(int i, int j, boolean snake, boolean bootstrap, PathNode prev) {
        PathNode(int i, int j, boolean snake, boolean bootstrap, PathNode prev) {
            this.i = i;
            this.j = j;
            this.snake = snake;
            this.bootstrap = bootstrap;
            this.prev = snake ? prev : prev == null ? null : prev.previousSnake();
        }
        PathNode previousSnake() {
            if (bootstrap) {
                return null;
            }
            if (!snake && prev != null) {
                return prev.previousSnake();
            }
            return this;
        }
    }

    record Change(char deltaType, int startOriginal, int endOriginal, int startRevised, int endRevised) { }

    private static PathNode buildPath(final List<String> orig, final List<String> rev) {

        final int N = orig.size();
        final int M = rev.size();

        final int MAX = N + M + 1;
        final int size = 1 + 2 * MAX;
        final int middle = size / 2;
        final PathNode[] diagonal = new PathNode[size];

        diagonal[middle + 1] = new PathNode(0, -1, true, true, null);
        for (int d = 0; d < MAX; d++) {
            for (int k = -d; k <= d; k += 2) {
                final int kmiddle = middle + k;
                final int kplus = kmiddle + 1;
                final int kminus = kmiddle - 1;
                PathNode prev;
                int i;

                if ((k == -d) || (k != d && diagonal[kminus].i < diagonal[kplus].i)) {
                    i = diagonal[kplus].i;
                    prev = diagonal[kplus];
                } else {
                    i = diagonal[kminus].i + 1;
                    prev = diagonal[kminus];
                }

                diagonal[kminus] = null; // no longer used

                int j = i - k;

                PathNode node = new PathNode(i, j, false, false, prev);

                while (i < N && j < M && Objects.equals(orig.get(i), rev.get(j))) {
                    i++;
                    j++;
                }

                if (i != node.i) {
                    node = new PathNode(i, j, true, false, node);
                }

                diagonal[kmiddle] = node;

                if (i >= N && j >= M) {
                    return diagonal[kmiddle];
                }
            }
            diagonal[middle + d - 1] = null;
        }
        throw new IllegalStateException("could not find a diff path");
    }

    private static List<Change> buildRevision(PathNode actualPath, List<String> orig, List<String> rev) {

        PathNode path = actualPath;
        List<Change> changes = new ArrayList<>();
        if (path.snake) {
            path = path.prev;
        }
        while (path != null && path.prev != null && path.prev.j >= 0) {
            if (path.snake()) {
                throw new IllegalStateException("bad diffpath: found snake when looking for diff");
            }
            int i = path.i;
            int j = path.j;

            path = path.prev;
            int ianchor = path.i;
            int janchor = path.j;

            if (ianchor == i && janchor != j) {
                changes.add(new Change('I', ianchor, i, janchor, j));
            } else if (ianchor != i && janchor == j) {
                changes.add(new Change('D', ianchor, i, janchor, j));
            } else {
                changes.add(new Change('C', ianchor, i, janchor, j));
            }

            if (path.snake()) {
                path = path.prev;
            }
        }
        return changes;
    }
}
