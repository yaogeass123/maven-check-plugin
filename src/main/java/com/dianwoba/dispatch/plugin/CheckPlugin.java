package com.dianwoba.dispatch.plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * @author Administrator
 */

@Mojo(name = "check", defaultPhase = LifecyclePhase.COMPILE)
public class CheckPlugin extends AbstractMojo {

    private static final String PART = "@Value[(]\"(.)*:(.)*\"[)]";

    private static final String PART_RIGHT = "@Value[(]\"\\$\\{(\\w|-)+:(\\w|-)+}\"[)]";

    private static final String PREFIX = "@Value";

    private static StringBuilder rightStr;

    private static StringBuilder wrongStr;

    @Parameter(property = "path", defaultValue = "NULL")
    private String path;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        rightStr = new StringBuilder();
        wrongStr = new StringBuilder();
        doCheck(path);
        String wrongResult = wrongStr.toString();
        String rightResult = rightStr.toString();
        System.out.println(rightResult);
        if (!"".equals(wrongResult)) {
            System.out.println(wrongResult);
            throw new MojoFailureException("有不符合要求的value注入：\n" + wrongResult);
        }
    }

    private void doCheck(String path) {
        File[] files = new File(path).listFiles();
        if (Objects.isNull(files)) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                doCheck(file.getPath());
                continue;
            }
            if (file.isFile() && file.getName().endsWith("java")) {
                try {
                    checkFormat(file);
                } catch (IOException e) {
                    System.out.println("读取文件失败：" + file.getName());
                }
            }
        }
    }

    private void checkFormat(File file) throws IOException {
        FileInputStream stream = new FileInputStream(file);
        InputStreamReader inputStreamReader = new InputStreamReader(stream);
        BufferedReader read = new BufferedReader(inputStreamReader);
        String line;
        while ((line = read.readLine()) != null) {
            if (valid(line)) {
                Matcher part = Pattern.compile(PART).matcher(line);
                if (part.find()) {
                    Matcher right = Pattern.compile(PART_RIGHT).matcher(line);
                    if (right.find()) {
                        rightStr.append("Right: ").append(line).append("\n");
                    } else {
                        wrongStr.append("Wrong: ").append(line).append("\n");
                    }
                }
            }
        }
        read.close();
        inputStreamReader.close();
        stream.close();
    }

    private boolean valid(String line) {
        line = line.trim();
        return line.startsWith(PREFIX);
    }
}
