package ui;

import java.io.Console;
import java.util.Scanner;

/**
 * 控制台工具类，封装常用的终端输入输出操作。
 * <p>
 * 为各菜单类提供统一的标题打印、菜单选项、成功/错误提示、分隔线、表格及读取输入等功能。
 * 所有输出仅使用 ASCII 符号与中文字符，确保在 GBK/UTF-8 等常见编码下均不易出现乱码。
 */
public final class ConsoleUtils {
    // 根据环境变量或控制台字符集初始化的 Scanner，用于正确读取中文输入。
    private static final Scanner SCANNER = createScanner();
    // 标题行的默认显示宽度（按终端列数计算，中文字符占 2 列）。
    private static final int HEADER_WIDTH = 44;

    // 私有构造器，防止实例化；所有方法均为静态工具方法。
    private ConsoleUtils() {
    }

    // 创建 Scanner：优先使用 CONSOLE_ENCODING 环境变量，其次使用交互式控制台的字符集。
    private static Scanner createScanner() {
        try {
            String envEncoding = System.getenv("CONSOLE_ENCODING");
            if (envEncoding != null && !envEncoding.isBlank()) {
                return new Scanner(System.in, envEncoding);
            }
            Console console = System.console();
            if (console != null) {
                return new Scanner(System.in, console.charset());
            }
        } catch (Exception ignored) {
            // 初始化失败时回退到默认 Scanner。
        }
        return new Scanner(System.in);
    }

    // 打印居中的主标题，上下用 = 号线包围。
    public static void printHeader(String title) {
        String centered = " " + title + " ";
        int padding = HEADER_WIDTH - displayWidth(centered);
        int left = padding / 2;
        int right = padding - left;
        System.out.println();
        System.out.println(repeat('=', left) + centered + repeat('=', right));
    }

    // 打印子标题，用于列表或操作分区。
    public static void printSubHeader(String title) {
        System.out.println("-- " + title + " --");
    }

    // 打印分隔线，用于区隔菜单选项与输入提示。
    public static void printSeparator() {
        System.out.println(repeat('-', HEADER_WIDTH));
    }

    // 打印菜单选项，统一为 [编号] 文本 的格式。
    public static void printOption(int number, String text) {
        System.out.printf("  [%d] %s%n", number, text);
    }

    // 打印成功提示。
    public static void printSuccess(String message) {
        System.out.println("[OK] " + message);
    }

    // 打印错误提示。
    public static void printError(String message) {
        System.out.println("[!] " + message);
    }

    // 打印信息提示。
    public static void printInfo(String message) {
        System.out.println("[i] " + message);
    }

    // 打印空行。
    public static void printBlankLine() {
        System.out.println();
    }

    // 提示用户输入一行字符串并返回去除首尾空白后的结果。
    public static String readLine(String prompt) {
        System.out.print(prompt + ": ");
        return SCANNER.nextLine().trim();
    }

    // 循环读取用户输入，直到获得合法整数为止。
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

    // 循环读取用户输入，直到获得合法长整型数字为止。
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

    // 提示用户按回车键继续，常用于分页或操作完成后暂停。
    public static void waitForEnter() {
        System.out.print("按回车继续...");
        SCANNER.nextLine();
    }

    // 计算字符串在终端中的显示宽度：中文字符按 2 列，ASCII 按 1 列。
    public static int displayWidth(String text) {
        int width = 0;
        for (int i = 0; i < text.length(); ) {
            int codePoint = text.codePointAt(i);
            width += isFullWidth(codePoint) ? 2 : 1;
            i += Character.charCount(codePoint);
        }
        return width;
    }

    // 在字符串右侧补充空格，使其显示宽度达到指定值，便于对齐表格列。
    public static String padRight(String text, int width) {
        int pad = width - displayWidth(text);
        return text + repeat(' ', Math.max(pad, 0));
    }

    // 重复指定字符若干次，用于构造标题线和分隔线。
    private static String repeat(char c, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder builder = new StringBuilder(count);
        for (int i = 0; i < count; i++) {
            builder.append(c);
        }
        return builder.toString();
    }

    // 打印 Unicode 表格，自动根据表头和内容计算列宽并处理中英文对齐。
    // 建议使用 UTF-8 编码的终端以获得最佳显示效果。
    public static void printTable(String[] headers, java.util.List<String[]> rows) {
        if (headers == null || headers.length == 0) {
            return;
        }

        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = displayWidth(headers[i]);
        }
        for (String[] row : rows) {
            int limit = Math.min(row.length, widths.length);
            for (int i = 0; i < limit; i++) {
                widths[i] = Math.max(widths[i], displayWidth(row[i]));
            }
        }

        String top = buildHorizontalLine(widths, "\u250C", "\u252C", "\u2510");
        String middle = buildHorizontalLine(widths, "\u251C", "\u253C", "\u2524");
        String bottom = buildHorizontalLine(widths, "\u2514", "\u2534", "\u2518");

        System.out.println(top);
        System.out.println(buildRow(headers, widths));
        System.out.println(middle);

        if (rows.isEmpty()) {
            System.out.println("\u2502 " + padRight("（无数据）", top.length() - 4) + " \u2502");
            System.out.println(bottom);
            return;
        }

        for (String[] row : rows) {
            System.out.println(buildRow(row, widths));
        }
        System.out.println(bottom);
    }

    // 根据列宽构造表格横线，使用指定的左、中、右连接符。
    private static String buildHorizontalLine(int[] widths, String left, String cross, String right) {
        StringBuilder line = new StringBuilder(left);
        for (int i = 0; i < widths.length; i++) {
            line.append(repeat('\u2500', widths[i] + 2));
            line.append(i < widths.length - 1 ? cross : right);
        }
        return line.toString();
    }

    // 构造一行单元格内容，两侧使用竖线包围。
    private static String buildRow(String[] cells, int[] widths) {
        StringBuilder row = new StringBuilder("\u2502");
        int limit = Math.min(cells.length, widths.length);
        for (int i = 0; i < limit; i++) {
            row.append(' ').append(padRight(cells[i], widths[i])).append(" \u2502");
        }
        for (int i = limit; i < widths.length; i++) {
            row.append(' ').append(padRight("", widths[i])).append(" \u2502");
        }
        return row.toString();
    }

    // 判断码点是否为全角字符，主要包括 CJK 统一表意文字、全角标点、平假名、片假名、韩文音节等。
    private static boolean isFullWidth(int codePoint) {
        return (codePoint >= 0x4E00 && codePoint <= 0x9FFF)
                || (codePoint >= 0x3400 && codePoint <= 0x4DBF)
                || (codePoint >= 0xF900 && codePoint <= 0xFAFF)
                || (codePoint >= 0xFF00 && codePoint <= 0xFFEF)
                || (codePoint >= 0x3000 && codePoint <= 0x303F)
                || (codePoint >= 0x3040 && codePoint <= 0x309F)
                || (codePoint >= 0x30A0 && codePoint <= 0x30FF)
                || (codePoint >= 0xAC00 && codePoint <= 0xD7AF);
    }
}
