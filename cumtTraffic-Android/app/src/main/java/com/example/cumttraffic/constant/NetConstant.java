package com.example.cumttraffic.constant;

public class NetConstant {
    public static final String baseService = "http://1.116.180.130:8091";
    private static final String getOtpCodeURL     = baseService + "/user/getotp";
    private static final String loginURL          = baseService + "/user/login";
    private static final String registerURL       = baseService + "/user/register";
    private static final String allBusURL         = baseService + "/bus/getall";

    public static String getGetOtpCodeURL() {
        return getOtpCodeURL;
    }

    public static String getLoginURL() {
        return loginURL;
    }

    public static String getRegisterURL() {
        return registerURL;
    }

    public static String getAllBusURL() {
        return allBusURL;
    }
}
