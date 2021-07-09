package com.example.cumttraffic.activity;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.databinding.DataBindingUtil;

import com.example.cumttraffic.R;
import com.example.cumttraffic.constant.NetConstant;
import com.example.cumttraffic.databinding.ActivityLoginBinding;
import com.example.cumttraffic.utils.ModelPreference;
import com.example.cumttraffic.utils.ValidUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class LoginActivity extends BaseActivity implements View.OnClickListener {

    // Log打印的通用Tag
    private final String TAG = "LoginActivity";

    private ActivityLoginBinding loginBinding;

    private String encryptedPassword;
    private Boolean isLogged;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fullScreenConfig();

        ModelPreference.getBoolean(LoginActivity.this,"isLogged",false);

        isLogged = ModelPreference.getBoolean(LoginActivity.this,"isLogged",false);

        if(isLogged){
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }

        loginBinding = DataBindingUtil.setContentView(this, R.layout.activity_login);
        // 为点击事件设置监听器
        setOnClickListener();

        /*
        当输入框焦点失去时,检验输入数据，提示错误信息
        第一个参数：输入框对象
        第二个参数：输入数据类型
        第三个参数：输入不合法时提示信息
        */
        setOnFocusChangeErrMsg(loginBinding.etAccount, "phone", "手机号格式不正确");
        setOnFocusChangeErrMsg(loginBinding.etPassword, "password", "密码必须不少于6位");
    }

    // 为点击事件的UI对象设置监听器
    private void setOnClickListener() {
        loginBinding.btLogin.setOnClickListener(this); // 登录按钮
        loginBinding.tvToRegister.setOnClickListener(this); // 注册文字
        loginBinding.tvForgetPassword.setOnClickListener(this); // 忘记密码文字
    }

    /*
    当账号输入框失去焦点时，校验账号是否是中国大陆手机号
    当密码输入框失去焦点时，校验密码是否不少于6位
    如有错误，提示错误信息
     */
    private void setOnFocusChangeErrMsg(EditText editText, String inputType, String errMsg) {
        editText.setOnFocusChangeListener(
                new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        String inputStr = editText.getText().toString();
                        if (!hasFocus) {
                            switch (inputType) {
                                case "phone":
                                    if (!ValidUtils.isPhoneValid(inputStr)) {
                                        editText.setError(errMsg);
                                    }
                                    break;
                                case "password":
                                    if (!ValidUtils.isPasswordValid(inputStr)) {
                                        editText.setError(errMsg);
                                    }
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                }
        );
    }


    // 因为 implements View.OnClickListener 加上已经 setOnClickListener()
    // 所以OnClick方法可以写到onCreate方法外
    @SuppressLint("NonConstantResourceId")
    @Override
    public void onClick(View v) {
        // 获取用户输入的账号和密码以进行验证
        String account = loginBinding.etAccount.getText().toString();
        String password = loginBinding.etPassword.getText().toString();

        switch (v.getId()) {
            // 登录按钮 响应事件
            case R.id.bt_login:
                // 让密码输入框失去焦点,触发setOnFocusChangeErrMsg方法
                loginBinding.etPassword.clearFocus();
                // 发送URL请求之前,先进行校验
                if (!(ValidUtils.isPhoneValid(account) && ValidUtils.isPasswordValid(password))) {
                    Toast.makeText(this, "账号或密码错误", Toast.LENGTH_SHORT).show();
                    break;
                }
                /*
                   因为验证是耗时操作，所以独立成方法
                   在方法中开辟子线程，避免在当前UI线程进行耗时操作
                   否则会造成 ANR（application not responding）
                */
                asyncLogin(account, password);
                break;
            // 注册用户 响应事件
            case R.id.tv_to_register:
                /*
                  关于这里传参说明：给用户一个良好的体验，
                  如果在登录界面填写过的，就不需要再填了
                  所以Intent把填写过的数据传递给注册界面
                 */
                Intent intentToRegister = new Intent(this, com.example.cumttraffic.activity.RegisterActivity.class);
                intentToRegister.putExtra("account", account);
                startActivity(intentToRegister);
                break;

            // 以下功能目前都没有实现
            case R.id.tv_forget_password:
                // 跳转到修改密码界面
                break;
        }
    }


    /*
      okhttp异步POST请求 要求API level 21+
      account 本来想的是可以是 telphone或者username
      但目前只实现了telphone
    */
    private void asyncLogin(final String account, final String password) {
        /*
         发送请求属于耗时操作，所以开辟子线程执行
         上面的参数都加上了final，否则无法传递到子线程中
        */
        new Thread(new Runnable() {
            @Override
            public void run() {
                // okhttp异步POST请求； 总共5步
                // 1、初始化okhttpClient对象
                OkHttpClient okHttpClient = new OkHttpClient();
                // 2、构建请求体requestBody
                final String telphone = account;  // 为了让键和值名字相同，我把account改成了telphone，没其他意思
                RequestBody requestBody = new FormBody.Builder()
                        .add("telphone", telphone)
                        .add("password", password)
                        .build();
                // 3、发送请求，因为要传密码，所以用POST方式
                Request request = new Request.Builder()
                        .url(NetConstant.getLoginURL())
                        .post(requestBody)
                        .build();
                // 4、使用okhttpClient对象获取请求的回调方法，enqueue()方法代表异步执行
                okHttpClient.newCall(request).enqueue(new Callback() {
                    // 5、重写两个回调方法
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "请求URL失败： " + e.getMessage());
                        showToastInThread(com.example.cumttraffic.activity.LoginActivity.this, "请求URL失败, 请重试！");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {
                        // 先判断一下服务器是否异常
                        String responseStr = response.toString();
                        if (responseStr.contains("200")) {
                            /* 注意这里，同一个方法内 response.body().string()只能调用一次，多次调用会报错 */
                            /* 使用Gson解析response的JSON数据的第一步 */
                            String responseBodyStr = response.body().string();
                            /* 使用Gson解析response的JSON数据的第二步 */
                            JsonObject responseBodyJSONObject = (JsonObject) new JsonParser().parse(responseBodyStr);
                            // 如果返回的status为success，则getStatus返回true，登录验证通过
                            if (getStatus(responseBodyJSONObject).equals("success")) {
                            /*
                             更新token，下次自动登录
                             真实的token值应该是一个加密字符串
                             我为了让token+不为null，就随便传了一个字符串
                             这里的telphone和password每次都要重写的
                             否则无法实现修改密码
                            */
                                try {
                                    encryptedPassword = ValidUtils.encodeByMd5(password);
                                } catch (NoSuchAlgorithmException e) {
                                    e.printStackTrace();
                                }

                                ModelPreference.putBoolean(LoginActivity.this,"isLogged",true);
                                ModelPreference.putString(LoginActivity.this,"telphone",telphone);
                                ModelPreference.putString(LoginActivity.this,"encryptedPassword",encryptedPassword);

                                Intent it_login_to_main = new Intent(com.example.cumttraffic.activity.LoginActivity.this, MainActivity.class);
                                startActivity(it_login_to_main);
                                // 登录成功后，登录界面就没必要占据资源了
                                finish();

                            } else {
                                getResponseErrMsg(com.example.cumttraffic.activity.LoginActivity.this, responseBodyJSONObject);
                                Log.d(TAG, "账号或密码验证失败");
                            }
                        } else {
                            Log.d(TAG, "服务器异常");
                            showToastInThread(com.example.cumttraffic.activity.LoginActivity.this, responseStr);
                        }
                    }
                });

            }
        }).start();
    }

    /*
      使用Gson解析response的JSON数据
      本来总共是有三步的，一、二步在方法调用之前执行了
    */
    private String getStatus(JsonObject responseBodyJSONObject) {
        /* 使用Gson解析response的JSON数据的第三步
           通过JSON对象获取对应的属性值 */
        String status = responseBodyJSONObject.get("status").getAsString();
        // 登录成功返回的json为{ "status":"success", "data":null }
        // 只获取status即可，data为null
        return status;
    }

    /*
      使用Gson解析response返回异常信息的JSON中的data对象
      这也属于第三步，一、二步在方法调用之前执行了
     */
    private void getResponseErrMsg(Context context, JsonObject responseBodyJSONObject) {
        String errorCode = responseBodyJSONObject.get("data").getAsString();
        String errorMsg = responseBodyJSONObject.get("status").getAsString();
        Log.d(TAG, "errorCode: " + errorCode + " errorMsg: " + errorMsg);
        // 在子线程中显示Toast
        showToastInThread(context, errorMsg);
    }

}
