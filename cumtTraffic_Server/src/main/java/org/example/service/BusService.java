package org.example.service;

import org.example.error.BusinessException;
import org.example.service.model.BusModel;

import java.util.List;
import java.util.Map;

/**
 * Created by liuxin on 2021/5/15
 */
public interface BusService {
    //通过巴士ID获取巴士对象的方法
    BusModel getBusById(Integer id);

    //通过巴士BusId获取巴士对象的方法
    BusModel getBusByBusId(String BusId);

    //获取所有巴士对象的方法
    public List<Map> getAllBus();

    //添加新巴士的方法
    void addNewBus(BusModel busModel) throws BusinessException;

    //更新巴士
    void updateBus(BusModel busModel) throws BusinessException;

}
