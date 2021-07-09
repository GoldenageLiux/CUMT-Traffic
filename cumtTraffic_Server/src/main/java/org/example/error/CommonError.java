package org.example.error;

/**
 * Created by liuxin on 2021/5/4
 */
public interface CommonError {
    public int getErrCode();
    public String getErrMsg();
    public CommonError setErrMsg(String errMsg);
}
