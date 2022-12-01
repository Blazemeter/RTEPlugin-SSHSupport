package com.blazemeter.jmeter.rte.recorder.emulator;

public class CommandUtils {
  public static String getUnicodeString(String c) {
    return getUnicodeString(c.charAt(0));
  }

  /**
   * Convert a character to its unicode value.
   * @param c character to convert
   * @return unicode value of the character. If the character is not a printable character, returns
   * -1.
   */
  public static String getUnicodeString(char c) {
    if (!isControlCode(c)) {
      return "-1";
    }

    //If it is a control character, we return the string representation of the character to properly
    //store it in the sample
    String hexString = Integer.toHexString(c | 0x10000);
    String unicodeString = "\\u" + hexString.substring(1);
    return unicodeString;
  }

  public static boolean isControlCode(char c) {
    return Character.isISOControl(c);
  }

  public static boolean isControlCode(String c) {
    if (c == null || c.length() != 1) {
      return false;
    }

    return Character.isISOControl(c.charAt(0));
  }

  public static boolean isControlUnicode(String unicodeString) {
    if (unicodeString == null || unicodeString.length() != 6) {
      return false;
    }

    return unicodeString.startsWith("\\u");
  }

  public static char getCharFromUnicodeString(String unicodeString) {
    if (unicodeString == null || unicodeString.isEmpty()) {
      return 0;
    }

    if (unicodeString.length() == 1) {
      return unicodeString.charAt(0);
    }

    if (unicodeString.length() == 6 && unicodeString.startsWith("\\u")) {
      return (char) Integer.parseInt(unicodeString.substring(2), 16);
    }
    return 0;
  }
}
