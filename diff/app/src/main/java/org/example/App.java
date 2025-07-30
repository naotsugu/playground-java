package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class App {

    public static void main(String[] args) {
        List<String> org = List.of("a", "b", "c", "d", "e", "f");
        List<String> rev = List.of("a", "b", "b", "d", "c", "e", "f");
        PathNode path = buildPath(org, rev);
        List<Change> changes = buildRevision(path);
        changes.forEach(System.out::println);
        print(changes, org, rev);
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

    record Change(Type type, int startOrg, int endOrg, int startRev, int endRev) {
        enum Type { CHANGE, DELETE, INSERT }
    }


    private static <T> PathNode buildPath(final List<T> org, final List<T> rev) {

        final int n = org.size();
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

                while (i < n && j < m && Objects.equals(org.get(i), rev.get(j))) {
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
            Change.Type type = (path.i == i && path.j != j)
                ? Change.Type.INSERT
                : (path.i != i && path.j == j)
                    ? Change.Type.DELETE
                    : Change.Type.CHANGE;
            changes.addFirst(new Change(type, path.i, i, path.j, j));

            if (path.snake) {
                path = path.prev;
            }
        }
        return changes;
    }

    private static <T> void print(List<Change> changes, final List<T> org, final List<T> rev) {
        final String R = "\u001b[00;31m";
        final String G = "\u001b[00;32m";
        final String E = "\u001b[00m";
        final String FMT_D = R + " %04d       : -  %s" + E + "\n";
        final String FMT_I = G + "       %04d : +  %s" + E + "\n";
        final String FMT_N =     " %04d  %04d :    %s" + "\n";

        int i = 0;
        int j = 0;
        while (i < org.size() && j < rev.size()) {
            if (changes.isEmpty()) {
                System.out.printf(FMT_N, i, j, rev.get(j++));
                i++;
                continue;
            }
            Change c = changes.getFirst();
            if (c.type == Change.Type.CHANGE && c.startOrg <= i && i < c.endOrg) {
                if (c.endRev - 1 == i) changes.removeFirst();
                System.out.printf(FMT_D, i, org.get(i++));
                System.out.printf(FMT_I, j, rev.get(j++));
            } else if (c.type == Change.Type.INSERT && c.startRev <= j && j < c.endRev) {
                if (c.endRev - 1 == j) changes.removeFirst();
                System.out.printf(FMT_I, j, rev.get(j++));
            } else if (c.type == Change.Type.DELETE && c.startOrg <= i && i < c.endOrg) {
                if (c.endOrg - 1 == i) changes.removeFirst();
                System.out.printf(FMT_D, i, org.get(i++));
            } else {
                System.out.printf(FMT_N, i, j, rev.get(j++));
                i++;
            }
        }
    }
}
