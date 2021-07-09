package org.example.service.impl;

import org.example.dao.UserDOMapper;
import org.example.dao.UserPasswordDOMapper;
import org.example.dataobject.UserDO;
import org.example.dataobject.UserPasswordDO;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.service.UserService;
import org.example.service.model.UserModel;
import org.example.validator.ValidationResult;
import org.example.validator.ValidatorImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;
import org.springframework.util.StringUtils;

/**
 * Created by liuxin on 2021/5/3
 */
@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDOMapper userDOMapper;

    @Autowired
    private UserPasswordDOMapper userPasswordDOMapper;

    @Autowired
    private ValidatorImpl validator;

    @Override
    public UserModel getUserById(Integer id) {
        //调用userdomapper获取到对应的用户dataobject
        UserDO userDO = userDOMapper.selectByPrimaryKey(id);

        if(userDO == null){
            return null;
        }
        //通过用户id获取对应的用户加密密码信息
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());

        return convertFromDataObject(userDO, userPasswordDO);
    }

    /**
     * 用户注册服务的实现
     * 加上@Transactional注解是为了避免出现用户信息插入不全，程序意外结束
     *
     * @param userModel 用户信息
     */
    @Override
    @Transactional
    public void register(UserModel userModel) throws BusinessException {
        // 先进行整体判空处理，这样代码才健壮一些
        if (userModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        ValidationResult result = validator.validate(userModel);
        if (result.isHasErrors()){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,result.getErrMsg());
        }

        // model --->  dataobject:UserDO
        // 之所以使用insertSelective方法，这样可以避免使用null字段，而使用设计数据库时的默认值
        UserDO userDO = convertFromModel(userModel);
        try{
            userDOMapper.insertSelective(userDO);
        }catch (DuplicateKeyException ex){
            // 手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"手机号重复注册");
        }

        //把增加userid后的userDo的id返回给UserModel
        userModel.setId(userDO.getId());

        // model --->  dataobject:UserPasswordDO
        UserPasswordDO userPasswordDO = convertPasswordFromModel(userModel);
        userPasswordDOMapper.insertSelective(userPasswordDO);
    }

    @Override
    public UserModel validateLogin(String telphone, String encryptPassword) throws BusinessException {
        //通过用户的手机获取用户信息
        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if(userDO == null){
            throw new BusinessException(EmBusinessError.USER_NOT_EXIST);
        }
        UserPasswordDO userPasswordDO = userPasswordDOMapper.selectByUserId(userDO.getId());
        UserModel userModel = convertFromDataObject(userDO,userPasswordDO);

        //比对用户信息内加密的密码是否和传输进来的密码相匹配
        if(!StringUtils.pathEquals(encryptPassword,userModel.getEncryptPassword())){
            throw new BusinessException(EmBusinessError.USER_LOGIN_FAIL);
        }
        return userModel;
    }

    @Override
    public Boolean getUserByTelphone(String telphone) {
        UserDO userDO = userDOMapper.selectByTelphone(telphone);
        if (userDO == null){
            return  false;
        } else {
            return  true;
        }
    }

    /**
     * 实现model --->  dataobject:UserDO
     *
     * @param userModel Model
     * @return UserDO
     */
    private UserDO convertFromModel(UserModel userModel){
        // 每一层都进行判空，这样代码才处处健壮
        if(userModel == null){
            return null;
        }
        UserDO userDO = new UserDO();
        // source是userModel，target是userDO，
        // 这样在copy过程中，userModel中多余的属性会被自动丢弃
        BeanUtils.copyProperties(userModel, userDO);

        return userDO;
    }

    /**
     * 实现model --->  dataobject:UserPasswordDO
     *
     * @param userModel Model
     * @return UserDO
     */
    private UserPasswordDO convertPasswordFromModel(UserModel userModel){
        if (userModel == null){
            return  null;
        }
        UserPasswordDO userPasswordDO = new UserPasswordDO();
        /*
        对于userPasswordDO，我们不能像userDO那样进行copy
        因为我们在整合DO为model时，id是从userDO那里copy过来的，
        强行copy会导致user_password表中id不一致
        还有一点是，应该先有userDO中的属性，才会有userPasswordDO中的属性
        所以这里的userPasswordDO的id属性不需要设置，自动递增即可
         */
        userPasswordDO.setEncryptPassword(userModel.getEncryptPassword());
        /*
         user_password表总共三个字段，id不用我们管，可不要漏掉user_id字段
         否则无法根据外键进行查询密码了
         之所以userModel可以getId，在register方法中有提到
         */
        userPasswordDO.setUserId(userModel.getId());
        return userPasswordDO;
    }

    private UserModel convertFromDataObject(UserDO userDO, UserPasswordDO userPasswordDO){
        if(userDO == null){
            return null;
        }
        UserModel userModel = new UserModel();
        BeanUtils.copyProperties(userDO,userModel);

        if(userPasswordDO != null){
            userModel.setEncryptPassword(userPasswordDO.getEncryptPassword());
        }

        return userModel;
    }

}
