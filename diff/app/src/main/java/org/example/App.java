package org.example;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class App {

    public static void main(String[] args) {
        Source<String> org = Source.of(List.of("a", "b", "c", "d", "e", "f"));
        Source<String> rev = Source.of(List.of("a", "b", "b", "d", "c", "e", "g", "h"));
        Node path = buildPath(org, rev);
        List<Change> changes = buildChanges(path);
        changes.forEach(System.out::println);
        print(changes, org, rev);
    }

    interface Source<T> {
        T get(int index);
        int size();
        static <T> Source<T> of(List<T> list) {
            return new Source<T>() {
                @Override public T get(int index) { return list.get(index); }
                @Override public int size() { return list.size(); }
            };
        }
    }

    // snakes that is a rightward or downward step followed by zero or more diagonal ones
    record Node(int i, int j, boolean snake, Node prev) {
        static Node of() { return new Node(0, -1, true, null); }
        static Node snakeOf(int i, int j, Node prev) { return new Node(i, j, true, prev); }
        static Node stepOf(int i, int j, Node prev) {
            return new Node(i, j, false, prev == null ? null : prev.prevSnake());
        }
        private Node prevSnake() {
            return isBootstrap() ? null
                : (!snake && prev != null)
                ? prev.prevSnake()
                : this;
        }
        private boolean isBootstrap() { return (i < 0 || j < 0); }
    }

    record Change(Type type, int orgFrom, int orgTo, int revFrom, int revTo) {
        enum Type { CHANGE, DELETE, INSERT }
    }


    private static <T> Node buildPath(final Source<T> org, final Source<T> rev) {

        final int n = org.size();
        final int m = rev.size();

        final int max = n + m + 1;
        final int size = 1 + 2 * max;
        final int mid = size / 2;
        final Node[] diagonal = new Node[size];
        diagonal[mid + 1] = Node.of();

        for (int d = 0; d < max; d++) {
            for (int k = -d; k <= d; k += 2) {
                final int mk = mid + k;
                final Node prev;
                int i;

                if ((k == -d) || (k != d && diagonal[mk - 1].i < diagonal[mk + 1].i)) {
                    i = diagonal[mk + 1].i;
                    prev = diagonal[mk + 1];
                } else {
                    i = diagonal[mk - 1].i + 1;
                    prev = diagonal[mk - 1];
                }
                diagonal[mk - 1] = null; // no longer used

                int j = i - k;

                Node node = Node.stepOf(i, j, prev);

                while (i < n && j < m && Objects.equals(org.get(i), rev.get(j))) {
                    i++;
                    j++;
                }

                if (i != node.i) {
                    node = Node.snakeOf(i, j, node);
                }

                diagonal[mk] = node;

                if (i >= n && j >= m) {
                    return diagonal[mk];
                }
            }
            diagonal[mid + d - 1] = null;
        }
        throw new IllegalStateException("could not find a diff path");
    }

    private static List<Change> buildChanges(Node path) {

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

    private static <T> void print(List<Change> changes, final Source<T> org, final Source<T> rev) {
        final String R = "\u001b[00;31m";
        final String G = "\u001b[00;32m";
        final String E = "\u001b[00m";
        final String FMT_D = R + " %04d       : -  %s" + E + "\n";
        final String FMT_I = G + "       %04d : +  %s" + E + "\n";
        final String FMT_N =     " %04d  %04d :    %s" + "\n";

        int i = 0;
        int j = 0;
        while (i < org.size() || j < rev.size()) {
            if (changes.isEmpty()) {
                System.out.printf(FMT_N, i, j, rev.get(j++));
                i++;
                continue;
            }
            Change c = changes.getFirst();
            if (c.type == Change.Type.CHANGE && (c.orgFrom <= i && i < c.orgTo || c.revFrom <= j && j < c.revTo)) {
                if (c.orgTo - 1 == i && c.revTo - 1 == j) changes.removeFirst();
                if (org.size() > i) {
                    System.out.printf(FMT_D, i, org.get(i++));
                }
                if (rev.size() > j) {
                    System.out.printf(FMT_I, j, rev.get(j++));
                }
            } else if (c.type == Change.Type.INSERT && c.revFrom <= j && j < c.revTo) {
                if (c.revTo - 1 == j) changes.removeFirst();
                System.out.printf(FMT_I, j, rev.get(j++));
            } else if (c.type == Change.Type.DELETE && c.orgFrom <= i && i < c.orgTo) {
                if (c.orgTo - 1 == i) changes.removeFirst();
                System.out.printf(FMT_D, i, org.get(i++));
            } else {
                System.out.printf(FMT_N, i, j, rev.get(j++));
                i++;
            }
        }
    }
}
