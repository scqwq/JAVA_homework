package ui;

import java.util.Scanner;

public final class ConsoleUtils {
    private static final Scanner SCANNER = new Scanner(System.in);

    private ConsoleUtils() {
    }

    public static void printHeader(String title) {
        System.out.println();
        System.out.println("==== " + title + " ====");
    }

    public static String readLine(String prompt) {
        System.out.print(prompt + ": ");
        return SCANNER.nextLine().trim();
    }

    public static int readInt(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException exception) {
                System.out.println("请输入整数。");
            }
        }
    }

    public static long readLong(String prompt) {
        while (true) {
            String value = readLine(prompt);
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException exception) {
                System.out.println("请输入数字。");
            }
        }
    }

    public static void waitForEnter() {
        System.out.print("按回车继续...");
        SCANNER.nextLine();
    }
}

