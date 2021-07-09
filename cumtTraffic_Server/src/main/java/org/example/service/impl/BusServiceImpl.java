package org.example.service.impl;

import org.example.dao.BusDOMapper;
import org.example.dataobject.BusDO;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.service.BusService;
import org.example.service.model.BusModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.Map;

/**
 * Created by liuxin on 2021/5/15
 */
@Service
public class BusServiceImpl implements BusService {

    @Autowired
    private BusDOMapper busDOMapper;

    @Override
    public BusModel getBusById(Integer id) {
        //调用busdomapper获取到对应的巴士dataobject
        BusDO busDO = busDOMapper.selectByPrimaryKey(id);

        if(busDO==null){
            return null;
        }

        return convertFromDataObject(busDO);
    }

    @Override
    public BusModel getBusByBusId(String busId) {
        //调用busdomapper获取到对应的巴士dataobject
        BusDO busDO = busDOMapper.selectByBusId(busId);

        if(busDO==null){
            return null;
        }

        return convertFromDataObject(busDO);
    }

    @Override
    public List<Map> getAllBus() {
        return busDOMapper.scanAllBus();
    }

    @Override
    public void addNewBus(BusModel busModel) throws BusinessException {
        if(busModel == null){
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR);
        }

        BusDO busDO = convertFromModel(busModel);

        try{
            busDOMapper.insertSelective(busDO);
        }catch (DuplicateKeyException ex){
            // 手动回滚事务
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            throw new BusinessException(EmBusinessError.PARAMETER_VALIDATION_ERROR,"车辆重复添加");
        }

        //把增加busid后的busDO的id返回给BusModel
        busModel.setId(busDO.getId());

    }

    @Override
    public void updateBus(BusModel busModel) throws BusinessException {

        if(busModel == null){
            throw new BusinessException(EmBusinessError.BUS_NOT_EXIST);
        }

        BusDO busDO = convertFromModel(busModel);

        busDOMapper.updateByPrimaryKeySelective(busDO);
    }

    /*
     * 实现BusDo-->BusModel
     */
    private BusModel convertFromDataObject(BusDO busDO){
        if(busDO==null){
            return null;
        }

        BusModel busModel = new BusModel();
        BeanUtils.copyProperties(busDO,busModel);

        return busModel;
    }

    /*
    * 实现model-->dataobject:BusDO
    */
    private BusDO convertFromModel(BusModel busModel){
        if (busModel == null){
            return null;
        }
        BusDO busDO = new BusDO();
        BeanUtils.copyProperties(busModel,busDO);
        return busDO;
    }
}
