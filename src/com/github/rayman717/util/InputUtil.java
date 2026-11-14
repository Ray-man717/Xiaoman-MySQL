package com.github.rayman717.util;

import java.util.Scanner;

public class InputUtil {
    public static String input(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
    }
}
