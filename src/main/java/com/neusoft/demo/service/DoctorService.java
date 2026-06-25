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

    /** 管理员端：查询医生列表（可按审核状态筛选），0待审核 1通过 2拒绝，为空则查询全部 */
    List<Doctor> list(Integer auditStatus);

    /** 管理员端：审核医生账号（通过/拒绝）,1通过 2拒绝 */
    void auditDoctor(Long id, Integer auditStatus);

    /** 管理员端：更新医生状态（启用/禁用） */
    void updateStatus(Long id, Integer status);

    /** 管理员端：更新医生角色（doctor / registrar / pharmacist / admin） */
    void updateRole(Long id, String role);
}
