import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class TollReconcile {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Random RANDOM = new Random(42);
    // 车牌省份简称
    private static final String[] PROVINCES = {"京", "沪", "粤", "苏", "浙", "鲁", "川", "鄂", "湘", "豫"};
    private static final String[] LETTERS = {"A", "B", "C", "D", "E", "F", "G", "H", "J", "K"};

    public static void main(String[] args) throws Exception {
        String baseDir = System.getProperty("user.dir");
        String tollPath = baseDir + "/toll.csv";
        String bankPath = baseDir + "/bank.csv";
        String reportPath = baseDir + "/reconcile-report.md";

        System.out.println("=== 高速公路收费流水对账工具 ===");
        System.out.println();

        // 1. 生成示例 CSV
        System.out.println("[步骤1] 生成示例数据...");
        generateSampleData(tollPath, bankPath);
        System.out.println("        toll.csv 已生成: " + tollPath);
        System.out.println("        bank.csv 已生成: " + bankPath);
        System.out.println();

        // 2. 读取 CSV
        System.out.println("[步骤2] 读取流水文件...");
        Map<String, TollRecord> tollMap = loadCsv(tollPath);
        Map<String, TollRecord> bankMap = loadCsv(bankPath);
        System.out.println("        toll.csv 记录数: " + tollMap.size());
        System.out.println("        bank.csv 记录数: " + bankMap.size());
        System.out.println();

        // 3. 执行对账
        System.out.println("[步骤3] 执行对账比对...");
        List<TollRecord> onlyInToll = new ArrayList<>();
        List<TollRecord> onlyInBank = new ArrayList<>();
        List<DiffRecord> amountMismatch = new ArrayList<>();

        for (TollRecord t : tollMap.values()) {
            TollRecord b = bankMap.get(t.txnId);
            if (b == null) {
                onlyInToll.add(t);
            } else if (t.amount != b.amount) {
                amountMismatch.add(new DiffRecord(t, b));
            }
        }

        for (TollRecord b : bankMap.values()) {
            if (!tollMap.containsKey(b.txnId)) {
                onlyInBank.add(b);
            }
        }

        int matched = tollMap.size() - onlyInToll.size() - amountMismatch.size();
        System.out.println("        匹配成功: " + matched + " 条");
        System.out.println("        仅 toll 存在: " + onlyInToll.size() + " 条");
        System.out.println("        仅 bank 存在: " + onlyInBank.size() + " 条");
        System.out.println("        金额不一致: " + amountMismatch.size() + " 条");
        System.out.println();

        // 4. 控制台输出对账结果
        System.out.println("[步骤4] 控制台输出对账结果");
        System.out.println("----------------------------------------");
        printSection("仅 toll 存在（收费系统有，银行无）", onlyInToll);
        printSection("仅 bank 存在（银行有，收费系统无）", onlyInBank);
        printDiffSection("金额不一致", amountMismatch);
        System.out.println("----------------------------------------");
        System.out.println();

        // 5. 生成对账报告（含中文）
        System.out.println("[步骤5] 生成对账报告 reconcile-report.md...");
        generateReport(reportPath, onlyInToll, onlyInBank, amountMismatch, matched);
        System.out.println("        报告已生成: " + reportPath);
        System.out.println();
        System.out.println("=== 对账完成 ===");
    }

    // ========== 数据结构 ==========
    static class TollRecord {
        String txnId;
        String plate;
        double amount;
        String time;

        TollRecord(String txnId, String plate, double amount, String time) {
            this.txnId = txnId;
            this.plate = plate;
            this.amount = amount;
            this.time = time;
        }
    }

    static class DiffRecord {
        TollRecord toll;
        TollRecord bank;

        DiffRecord(TollRecord toll, TollRecord bank) {
            this.toll = toll;
            this.bank = bank;
        }
    }

    // ========== 文件读写 ==========
    static Map<String, TollRecord> loadCsv(String path) throws IOException {
        Map<String, TollRecord> map = new LinkedHashMap<>();
        List<String> lines = Files.readAllLines(Paths.get(path), StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) continue;
            String[] parts = line.split(",", -1);
            if (parts.length < 4) continue;
            String txnId = parts[0].trim();
            String plate = parts[1].trim();
            double amount = Double.parseDouble(parts[2].trim());
            String time = parts[3].trim();
            map.put(txnId, new TollRecord(txnId, plate, amount, time));
        }
        return map;
    }

    // ========== 示例数据生成 ==========
    static void generateSampleData(String tollPath, String bankPath) throws IOException {
        List<TollRecord> tollList = new ArrayList<>();
        List<TollRecord> bankList = new ArrayList<>();

        // 生成40条两边都有的正常记录
        for (int i = 1; i <= 40; i++) {
            String txnId = String.format("T%03d", i);
            String plate = generatePlate();
            double amount = 5.0 + RANDOM.nextInt(45); // 5~49元
            String time = generateTime(i);
            tollList.add(new TollRecord(txnId, plate, amount, time));
            bankList.add(new TollRecord(txnId, plate, amount, time));
        }

        // 3条仅toll存在
        for (int i = 41; i <= 43; i++) {
            String txnId = String.format("T%03d", i);
            String plate = generatePlate();
            double amount = 10.0 + RANDOM.nextInt(40);
            String time = generateTime(i);
            tollList.add(new TollRecord(txnId, plate, amount, time));
        }

        // 3条仅bank存在
        for (int i = 44; i <= 46; i++) {
            String txnId = String.format("T%03d", i);
            String plate = generatePlate();
            double amount = 10.0 + RANDOM.nextInt(40);
            String time = generateTime(i);
            bankList.add(new TollRecord(txnId, plate, amount, time));
        }

        // 4条金额不一致（两边都有但金额不同）
        for (int i = 47; i <= 50; i++) {
            String txnId = String.format("T%03d", i);
            String plate = generatePlate();
            double amountToll = 20.0 + RANDOM.nextInt(30);
            double amountBank = amountToll + (RANDOM.nextBoolean() ? 5.0 : -5.0); // 相差5元
            if (amountBank < 0) amountBank = amountToll + 5.0;
            String time = generateTime(i);
            tollList.add(new TollRecord(txnId, plate, amountToll, time));
            bankList.add(new TollRecord(txnId, plate, amountBank, time));
        }

        // 补齐两边各 50 条（再加 3 条两边都有的正常记录）
        for (int i = 51; i <= 53; i++) {
            String txnId = String.format("T%03d", i);
            String plate = generatePlate();
            double amount = 5.0 + RANDOM.nextInt(45);
            String time = generateTime(i);
            tollList.add(new TollRecord(txnId, plate, amount, time));
            bankList.add(new TollRecord(txnId, plate, amount, time));
        }
        // toll: 40 + 3 + 4 + 3 = 50；bank: 40 + 3 + 4 + 3 = 50

        writeCsv(tollPath, tollList);
        writeCsv(bankPath, bankList);
    }

    static void writeCsv(String path, List<TollRecord> list) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("交易号,车牌,金额,时间\n");
        for (TollRecord r : list) {
            sb.append(r.txnId).append(",")
              .append(r.plate).append(",")
              .append(r.amount).append(",")
              .append(r.time).append("\n");
        }
        // 显式以 UTF-8 写文件
        Files.write(Paths.get(path), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    static String generatePlate() {
        String province = PROVINCES[RANDOM.nextInt(PROVINCES.length)];
        String letter = LETTERS[RANDOM.nextInt(LETTERS.length)];
        String nums = String.format("%05d", RANDOM.nextInt(100000));
        return province + letter + nums;
    }

    static String generateTime(int offsetMinutes) {
        LocalDateTime t = LocalDateTime.of(2025, 6, 1, 8, 0, 0);
        t = t.plusMinutes(offsetMinutes);
        return t.format(FMT);
    }

    // ========== 控制台输出 ==========
    static void printSection(String title, List<TollRecord> list) {
        System.out.println(title + " (" + list.size() + " 条)");
        if (list.isEmpty()) {
            System.out.println("  (无)");
        } else {
            System.out.printf("  %-8s %-12s %-8s %-20s%n", "交易号", "车牌", "金额", "时间");
            for (TollRecord r : list) {
                System.out.printf("  %-8s %-12s %-8.2f %-20s%n", r.txnId, r.plate, r.amount, r.time);
            }
        }
        System.out.println();
    }

    static void printDiffSection(String title, List<DiffRecord> list) {
        System.out.println(title + " (" + list.size() + " 条)");
        if (list.isEmpty()) {
            System.out.println("  (无)");
        } else {
            System.out.printf("  %-8s %-12s %-12s %-12s %-20s%n", "交易号", "车牌", "toll金额", "bank金额", "时间");
            for (DiffRecord d : list) {
                System.out.printf("  %-8s %-12s %-12.2f %-12.2f %-20s%n",
                    d.toll.txnId, d.toll.plate, d.toll.amount, d.bank.amount, d.toll.time);
            }
        }
        System.out.println();
    }

    // ========== 报告生成 ==========
    static void generateReport(String path, List<TollRecord> onlyInToll,
                               List<TollRecord> onlyInBank, List<DiffRecord> amountMismatch,
                               int matched) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("# 高速公路收费流水对账报告\n\n");
        sb.append("生成时间: ").append(LocalDateTime.now().format(FMT)).append("\n\n");
        sb.append("## 汇总\n\n");
        sb.append("| 项目 | 数量 |\n");
        sb.append("|------|------|\n");
        sb.append("| 匹配成功 | ").append(matched).append(" |\n");
        sb.append("| 仅 toll 存在 | ").append(onlyInToll.size()).append(" |\n");
        sb.append("| 仅 bank 存在 | ").append(onlyInBank.size()).append(" |\n");
        sb.append("| 金额不一致 | ").append(amountMismatch.size()).append(" |\n\n");

        sb.append("## 仅 toll 存在（收费系统有，银行无）\n\n");
        sb.append("| 交易号 | 车牌 | 金额 | 时间 |\n");
        sb.append("|--------|------|------|------|\n");
        for (TollRecord r : onlyInToll) {
            sb.append("| ").append(r.txnId).append(" | ")
              .append(r.plate).append(" | ")
              .append(String.format("%.2f", r.amount)).append(" | ")
              .append(r.time).append(" |\n");
        }
        sb.append("\n");

        sb.append("## 仅 bank 存在（银行有，收费系统无）\n\n");
        sb.append("| 交易号 | 车牌 | 金额 | 时间 |\n");
        sb.append("|--------|------|------|------|\n");
        for (TollRecord r : onlyInBank) {
            sb.append("| ").append(r.txnId).append(" | ")
              .append(r.plate).append(" | ")
              .append(String.format("%.2f", r.amount)).append(" | ")
              .append(r.time).append(" |\n");
        }
        sb.append("\n");

        sb.append("## 金额不一致\n\n");
        sb.append("| 交易号 | 车牌 | toll金额 | bank金额 | 时间 |\n");
        sb.append("|--------|------|----------|----------|------|\n");
        for (DiffRecord d : amountMismatch) {
            sb.append("| ").append(d.toll.txnId).append(" | ")
              .append(d.toll.plate).append(" | ")
              .append(String.format("%.2f", d.toll.amount)).append(" | ")
              .append(String.format("%.2f", d.bank.amount)).append(" | ")
              .append(d.toll.time).append(" |\n");
        }
        sb.append("\n");

        sb.append("---\n");
        sb.append("*报告由 TollReconcile 自动生成*\n");

        // 显式以 UTF-8 写文件
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(path), StandardCharsets.UTF_8))) {
            writer.write(sb.toString());
        }
    }
}
