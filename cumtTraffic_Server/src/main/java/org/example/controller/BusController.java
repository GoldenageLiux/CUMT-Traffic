package org.example.controller;

import org.example.controller.viewobject.BusVO;
import org.example.dao.BusDOMapper;
import org.example.dataobject.BusDO;
import org.example.error.BusinessException;
import org.example.error.EmBusinessError;
import org.example.response.CommonReturnType;
import org.example.service.BusService;
import org.example.service.model.BusModel;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Created by liuxin on 2021/5/15
 */

@Controller("bus")
@RequestMapping("/bus")
@CrossOrigin(allowCredentials = "true", allowedHeaders = "*")
public class BusController extends BaseController{

    @Autowired
    private BusService busService;

    @Autowired
    private BusDOMapper busDOMapper;

    @RequestMapping(value = "/get")
    @ResponseBody
    public CommonReturnType getBus(@RequestParam(name = "busId")String busId) throws BusinessException {

        //调用service服务获取对应busid的用户对象并返回前端
        BusModel busModel = busService.getBusByBusId(busId);
        if (busModel == null){
            throw new BusinessException(EmBusinessError.BUS_NOT_EXIST);
        }

        //将核心领域模型巴士对象转化为可供UI使用的viewobject
        BusVO busVO = convertFromModel(busModel);

        return CommonReturnType.create(busVO);
    }

    @RequestMapping(value = "/getall")
    @ResponseBody
    public CommonReturnType getAllBus() throws BusinessException {

        //调用service服务获取巴士用户对象并返回前端
        List<Map> busModels = busService.getAllBus();

        return CommonReturnType.create(busModels);
    }

    @RequestMapping(value = "/update")
    @ResponseBody
    public CommonReturnType upDateBus(
            @RequestParam(name = "busId") String busId,
            @RequestParam(name = "latitude") double latitude,
            @RequestParam(name = "longitude") double longitude) throws BusinessException {

        BusDO busDO = busDOMapper.selectByBusId(busId);
        BusModel busModel = new BusModel();
        busModel.setId(busDO.getId());
        busModel.setBusId(busId);
        busModel.setLatitude(latitude);
        busModel.setLongitude(longitude);

        busService.updateBus(busModel);
        //更新成功，返回success即可
        return CommonReturnType.create(null);
    }

    /*
    * 将BusModel转化为BusVO
    * */
    private BusVO convertFromModel(BusModel busModel) {
        if(busModel == null){
            return null;
        }

        BusVO busVO = new BusVO();
        BeanUtils.copyProperties(busModel,busVO);
        return busVO;
    }

}
