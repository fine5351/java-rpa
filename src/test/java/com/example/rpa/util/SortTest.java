package com.example.rpa.util;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class SortTest {
    public static void main(String[] args) {
        String[] filenames = {
            "3.8-記憶是夢的開場白-10.mp4",
            "3.8-記憶是夢的開場白-2.mp4",
            "2025-12-16-深境螺旋.mp4",
            "2025-12-16-深境螺旋-2.mp4",
            "abc.mp4",
            "abc-1.mp4",
            "abc-10.mp4",
            "abc-2.mp4"
        };

        File[] files = new File[filenames.length];
        for (int i = 0; i < filenames.length; i++) {
            files[i] = new File(filenames[i]);
        }

        Arrays.sort(files, NATURAL_SORT_ORDER);

        System.out.println("Sorted files:");
        for (File f : files) {
            System.out.println(f.getName());
        }
    }

    private static final Comparator<File> NATURAL_SORT_ORDER = new Comparator<File>() {
        @Override
        public int compare(File f1, File f2) {
            String s1 = f1.getName();
            String s2 = f2.getName();
            int n1 = s1.length();
            int n2 = s2.length();
            int i1 = 0;
            int i2 = 0;
            while (i1 < n1 && i2 < n2) {
                char c1 = s1.charAt(i1);
                char c2 = s2.charAt(i2);
                if (Character.isDigit(c1) && Character.isDigit(c2)) {
                    long num1 = 0;
                    while (i1 < n1 && Character.isDigit(s1.charAt(i1))) {
                        num1 = num1 * 10 + (s1.charAt(i1) - '0');
                        i1++;
                    }
                    long num2 = 0;
                    while (i2 < n2 && Character.isDigit(s2.charAt(i2))) {
                        num2 = num2 * 10 + (s2.charAt(i2) - '0');
                        i2++;
                    }
                    if (num1 != num2) {
                        return Long.compare(num1, num2);
                    }
                    continue;
                }
                if (c1 != c2) {
                    return c1 - c2;
                }
                i1++;
                i2++;
            }
            return n1 - n2;
        }
    };
}
