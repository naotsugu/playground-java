package org.example;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;

public class Main {
    static void main(String[] args) throws Throwable {

        Arena arena = Arena.global();
        SymbolLookup mathLib = SymbolLookup.libraryLookup(System.mapLibraryName("math_lib"), arena);

        MemorySegment addNumbersSymbol = mathLib.find("add")
            .orElseThrow(RuntimeException::new);

        MethodHandle addNumbers = Linker.nativeLinker().downcallHandle(
            addNumbersSymbol,
            FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT)
        );

        int x = 15;
        int y = 27;
        int result = (int) addNumbers.invoke(x, y);
        System.out.println(x + " + " + y + " = " + result);
    }
}
