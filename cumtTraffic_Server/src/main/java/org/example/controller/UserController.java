package org.example.controller;

import com.alibaba.druid.util.StringUtils;
import org.example.controller.viewobject.UserVO;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.response.CommonReturnType;
import org.example.response.OtpCode;
import org.example.service.UserService;
import org.example.service.model.UserModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;

/**
 * Created by liuxin on 2021/5/3
 */

@Controller("user")
@RequestMapping("/user")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class UserController extends BaseController{

    @Autowired
    private UserService userService;

    @Autowired
    private HttpServletRequest httpServletRequest;

    private HttpSession session;

    //用户登陆接口
    @RequestMapping(value = "/login",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType login(
            @RequestParam(name = "telphone") String telphone,
            @RequestParam(name = "password") String password
    ) throws BusinessException, UnsupportedEncodingException, NoSuchAlgorithmException {
        //入参校验
        if(StringUtils.isEmpty(telphone) || StringUtils.isEmpty(password)){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        //用户未注册
        boolean hasRegistered = userService.getUserByTelphone(telphone);
        if (!hasRegistered) {
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        //用户登陆服务,校验用户登陆是否合法
        UserModel userModel = userService.validateLogin(telphone,this.EncodeByMd5(password));

        //将登陆凭证加入到用户登陆成功的session内
        this.httpServletRequest.getSession().setAttribute("LOGIN",true);
        this.httpServletRequest.getSession().setAttribute("LOGIN_USER",userModel);

        return CommonReturnType.create(null);
    }


    //用户注册接口
    @RequestMapping(value = "/register",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType register(
            @RequestParam(name = "telphone") String telphone,
            @RequestParam(name = "otpCode") String otpCode,
            @RequestParam(name = "name") String name,
            @RequestParam(name = "age") String ageStr,
            @RequestParam(name = "gender") String genderStr,
            @RequestParam(name = "password") String password
    ) throws UnsupportedEncodingException, NoSuchAlgorithmException, BusinessException {

        boolean hasRegistered = userService.getUserByTelphone(telphone);
        if (hasRegistered) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "手机号已重复注册");
        }

        // 从Session中获取对应手机号的验证码
        // otpCode是用户填写的，inSessionOtpCode是系统生成的
        if (session == null) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "无效的验证码，请重新获取");
        }

        String inSessionOtpCode = (String) session.getAttribute(telphone);

        if (!StringUtils.equals(otpCode, inSessionOtpCode)) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "短信验证码错误");
        }

        // 类型转换，适配数据库
        int age = Integer.parseInt(ageStr);
        Byte gender = Byte.parseByte(genderStr);

        // 验证码通过后，进行注册流程
        UserModel userModel = new UserModel();
        userModel.setName(name);
        userModel.setGender(gender);
        userModel.setAge(age);
        userModel.setTelphone(telphone);
        userModel.setRegisterMode("byphone");
        userModel.setEncryptPassword(this.EncodeByMd5(password));

        userService.register(userModel);
        // 注册成功，只返回success即可
        return CommonReturnType.create(null);
    }

    public String EncodeByMd5(String str) throws NoSuchAlgorithmException, UnsupportedEncodingException {
        //确定一个计算方法
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        Base64.Encoder base64Encoder = Base64.getEncoder();
        //加密字符串
        String newStr = base64Encoder.encodeToString(md5.digest(str.getBytes(StandardCharsets.UTF_8)));
        return newStr;
    }

    //用户获取otp短信接口
    /**
     * 获取otp验证码
     * 三步走
     * 1、生成验证码
     * 2、存到 Session 中
     * 3、返回验证码
     *
     * 注意这里POST请求要求传递请求头 content-type:application/x-www-form-urlencoded
     *
     * @param telphone
     * @return
     * @throws BusinessException
     */
    @RequestMapping(value = "/getotp",method = {RequestMethod.POST},consumes = {CONTENT_TYPE_FORMED})
    @ResponseBody
    public CommonReturnType getOtp(@RequestParam(name = "telphone")String telphone) throws BusinessException {

        /* 0、用户获取验证码时，检测是否已存在注册用户 */
        boolean hasRegistered = userService.getUserByTelphone(telphone);
        if (hasRegistered) {
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR, "手机号已重复注册");
        }

        //需要按照一定规则生成OTP验证码
        Random random = new Random();
        int randomInt = random.nextInt(99999);
        randomInt += 100000;
        String otpCode = String.valueOf(randomInt);

        //将otp验证码同对应用户的手机号相关联,使用httpsession的方式绑定他的手机号与OTPCODE
        session = httpServletRequest.getSession();
        session.setAttribute(telphone,otpCode);

        //将otp验证码通过短信通道发送给用户,省略
        System.out.println("telphone = " + telphone + " & otpCode = " + otpCode);

        // 4、将信息抽象为类
        OtpCode otpCodeObj = new OtpCode(telphone, otpCode);
        // 5、返回正确信息，方便前端获取
        return CommonReturnType.create(otpCodeObj, "successGetOtpCode");
    }

    @RequestMapping("/get")
    @ResponseBody
    public CommonReturnType getUser(@RequestParam(name = "id")Integer id) throws BusinessException {
        //调用service服务获取对应id的用户对象并返回给前端
        UserModel userModel = userService.getUserById(id);

        //若获取的对应用户信息不存在
        if(userModel == null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }

        //将核心领域模型用户对象转化为可供UI使用的viewobject
        UserVO userVO = convertFromModel(userModel);

        //返回通用对象
        return CommonReturnType.create(userVO);
    }

    /**
     * 将UserModel转为UserVO
     *
     * @param userModel Model
     * @return UserVO
     */
    private UserVO convertFromModel(UserModel userModel){
        if(userModel == null){
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(userModel, userVO);
        return  userVO;
    }



}
