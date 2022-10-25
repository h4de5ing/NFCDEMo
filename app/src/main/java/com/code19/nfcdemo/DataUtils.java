package com.code19.nfcdemo;

public class DataUtils {
    public static String bytesToHexString(byte[] src, boolean isPrefix) {
        StringBuilder stringBuilder = new StringBuilder();
        if (isPrefix) stringBuilder.append("0x");
        if (src == null || src.length <= 0) {
            return null;
        }
        char[] buffer = new char[2];
        for (byte aSrc : src) {
            buffer[0] = Character.toUpperCase(Character.forDigit((aSrc >>> 4) & 0x0F, 16));
            buffer[1] = Character.toUpperCase(Character.forDigit(aSrc & 0x0F, 16));
            //System.out.println(buffer);
            stringBuilder.append(buffer);
        }
        return stringBuilder.toString();
    }

    public static String saveHex2String(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        char[] HEX = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
        for (byte aData : data) {
            int value = aData & 0xff;
            sb.append(HEX[value / 16]).append(HEX[value % 16]).append(" ");
        }
        return sb.toString();
    }
}
