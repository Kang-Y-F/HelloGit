package com.neusoft.demo.service;

import com.neusoft.demo.dto.DoctorAddDTO;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.vo.DoctorVO;
import com.neusoft.demo.vo.LoginVO;
import com.neusoft.demo.vo.QueueItemVO;

import java.util.List;

public interface DoctorService {

    /** 医生登录（原有，不改） */
    LoginVO login(LoginDTO loginDTO);

    /** 新增医生（原有，不改） */
    void addDoctor(DoctorAddDTO dto);

    List<Doctor> list();

    /** 今日候诊队列（急诊优先） */
    List<QueueItemVO> getTodayQueue(Long doctorId);

    /** 叫号：候诊 → 就诊中 */
    boolean callNext(Long registerOrderId);

    /** 完成接诊：就诊中 → 已完成 */
    boolean finishConsult(Long registerOrderId);

    /** 根据科室查看医生列表 */
    List<DoctorVO> listByDept(Long deptId);

    /** 小程序首页推荐医生（在岗医生，取前6条） */
    List<Doctor> getRecommendDoctor(Integer limit);

    /** 搜索医生 */
    List<Doctor> searchDoctor(String keyword);
}
