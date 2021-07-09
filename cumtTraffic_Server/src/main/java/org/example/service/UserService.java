package org.example.service;

import org.example.error.BusinessException;
import org.example.service.model.UserModel;

/**
 * Created by liuxin on 2021/5/3
 */
public interface UserService {
    //通过用户ID获取用户对象的方法
    UserModel getUserById(Integer id);
    void register(UserModel userModel) throws BusinessException;
    /*
    * telphone是用户注册的手机
    * password是用户加密后的密码
    * */
    UserModel validateLogin(String telphone, String encryptPassword) throws BusinessException;
    /* 用户获取手机验证码时需要用到 */
    public Boolean getUserByTelphone(String telephone);

}
