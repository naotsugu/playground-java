package org.example;

import com.example.math_lib.math_lib_h;

public class Main {
    static void main(String[] args) {
        int x = 15;
        int y = 27;
        int result = math_lib_h.add(x, y);
        System.out.println(x + " + " + y + " = " + result);
    }
}
