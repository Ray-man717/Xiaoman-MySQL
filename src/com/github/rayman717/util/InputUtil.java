package com.github.rayman717.util;

import java.util.Scanner;

/** 控制台输入工具。 */
public class InputUtil {
    /** 打印提示并读入一行（直接回车返回空串）。 */
    public static String input(Scanner sc, String prompt) {
        System.out.print(prompt);
        return sc.nextLine();
    }
}
