package com.laodu.p300;

/**
 * @Description
 * @Author zhou
 * @Date 2026/10/11
 */
public class CountClass {
    public static void main(String[] args) {
        countWord("asldjowwejo19208401283A");
    }



    public static void countWord(String str){

        int numCount = 0;
        int LowerCaseCharCount = 0;
        int UpperCaseCharCount = 0;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c >= '0' && c <= '9') {
                numCount++;
            } else if (c >= 'a' && c <= 'z') {
                LowerCaseCharCount++;
            } else if (c >= 'A' && c <= 'Z') {
                UpperCaseCharCount++;
            }
        }
        System.out.println("数字个数：" + numCount);
        System.out.println("小写字母个数：" + LowerCaseCharCount);
        System.out.println("大写字母个数：" + UpperCaseCharCount);


    }
}
