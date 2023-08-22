package com.great_systems.honestsigngenerator;

import java.util.List;

public class CompareContains {
    private static CompareContains instance;
    private static int code_int;
    private static byte code_byte;
    private static String code_string;

    private CompareContains() {
    }

    public static CompareContains code(int code) {
        if (instance == null) {
            instance = new CompareContains();
        }
        code_int = code;
        return instance;
    }

    public static CompareContains code(byte code) {
        if (instance == null) {
            instance = new CompareContains();
        }
        code_byte = code;
        return instance;
    }

    public static CompareContains code(String code) {
        if (instance == null) {
            instance = new CompareContains();
        }
        code_string = code;
        return instance;
    }

    public boolean include(int ...list) {
        for (int j : list) {
            if (code_int == j)
                return true;
        }
        return false;
    }

    public boolean include(byte ...list) {
        for (byte j : list) {
            if (code_byte == j)
                return true;
        }
        return false;
    }

    public boolean notInclude(int ...list) {
        for (int j : list) {
            if (code_int == j)
                return false;
        }
        return true;
    }

    public boolean notInclude(byte ...list) {
        for (byte j : list) {
            if (code_byte == j)
                return false;
        }
        return true;
    }

    public boolean include(boolean ignore_case, String ...list) {
        for (String s : list) {
            if (ignore_case) {
                if (code_string.equalsIgnoreCase(s))
                    return true;
            } else {
                if (code_string.equals(s))
                    return true;
            }
        }
        return false;
    }

    public boolean include(boolean ignore_case, List<String> list) {
        for (String s : list) {
            if (ignore_case) {
                if (code_string.toLowerCase().contains(s.toLowerCase()))
                    return true;
            } else {
                if (code_string.equals(s))
                    return true;
            }
        }
        return false;
    }

    public boolean notInclude(boolean ignore_case, String ...list) {
        for (String s : list) {
            if (ignore_case) {
                if (code_string.equalsIgnoreCase(s))
                    return false;
            } else {
                if (code_string.equals(s))
                    return false;
            }
        }
        return true;
    }

}
