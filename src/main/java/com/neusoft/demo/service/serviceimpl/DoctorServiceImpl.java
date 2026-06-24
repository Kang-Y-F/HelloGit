package com.neusoft.demo.service.serviceimpl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.neusoft.demo.dto.DoctorAddDTO;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.mapper.RegisterOrderMapper;
import com.neusoft.demo.service.DoctorService;
import com.neusoft.demo.utils.JwtUtil;
import com.neusoft.demo.utils.PasswordUtil;
import com.neusoft.demo.vo.LoginVO;
import com.neusoft.demo.vo.DoctorVO;
import com.neusoft.demo.vo.QueueItemVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private RegisterOrderMapper registerOrderMapper;

    // ── 原有方法，完全不改 ────────────────────────────────────────

    @Override
    public LoginVO login(LoginDTO loginDTO) {

        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>()
                        .eq(Doctor::getUsername, loginDTO.getUsername())
        );

        // 用户不存在
        if (doctor == null) {
            return null;
        }

// 替换原来的 PasswordUtil.matches 调用
        boolean match;
        String stored = doctor.getPassword();
        if (stored != null && stored.startsWith("$2a$")) {
            match = PasswordUtil.matches(loginDTO.getPassword(), stored);
        } else {
            // MD5 兼容
            match = stored != null && stored.equalsIgnoreCase(md5Hex(loginDTO.getPassword()));
        }

        if (!match) {
            return null;
        }

        String role = doctor.getRole() != null ? doctor.getRole() : "doctor";
        String token = JwtUtil.createToken(doctor.getId(), role);

        LoginVO vo = new LoginVO();
        vo.setId(doctor.getId());
        vo.setName(doctor.getName());
        vo.setRole(role);
        vo.setToken(token);
        vo.setTitle(doctor.getTitle());
        vo.setUsername(doctor.getUsername());
        return vo;
    }

    private String md5Hex(String input) {
        try {
            var md = java.security.MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var sb = new StringBuilder();
            for (byte b : digest) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) { return ""; }
    }

    @Override
    public void addDoctor(DoctorAddDTO dto) {

        Doctor doctor = doctorMapper.selectOne(
                new LambdaQueryWrapper<Doctor>()
                        .eq(Doctor::getUsername, dto.getUsername())
        );

        if (doctor != null) {
            throw new RuntimeException("账号已存在");
        }

        Doctor d = new Doctor();

        d.setName(dto.getName());

        d.setUsername(dto.getUsername());
        d.setPassword(PasswordUtil.encode(dto.getPassword()));
        d.setDeptId(dto.getDeptId());

        d.setTitle(dto.getTitle());

        d.setSkills(dto.getSkills());

        doctorMapper.insert(d);
    }

    // ── 新增方法 ──────────────────────────────────────────────────

    @Override
    public List<QueueItemVO> getTodayQueue(Long doctorId) {
        return registerOrderMapper.selectTodayQueue(doctorId);
    }

    @Override
    public boolean callNext(Long registerOrderId) {
        return registerOrderMapper.update(null,
                new LambdaUpdateWrapper<RegisterOrder>()
                        .eq(RegisterOrder::getId, registerOrderId)
                        .eq(RegisterOrder::getStatus, 1)
                        .set(RegisterOrder::getStatus, 3)
        ) > 0;
    }

    @Override
    public boolean finishConsult(Long registerOrderId) {
        return registerOrderMapper.update(null,
                new LambdaUpdateWrapper<RegisterOrder>()
                        .eq(RegisterOrder::getId, registerOrderId)
                        .eq(RegisterOrder::getStatus, 3)
                        .set(RegisterOrder::getStatus, 4)
        ) > 0;
    }

    @Override
    public List<Doctor> list() {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getRole, "doctor");

        return doctorMapper.selectList(wrapper);

    }

    @Override
    public List<DoctorVO> listByDept(Long deptId) {

        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();

        wrapper.eq(Doctor::getDeptId, deptId);
        wrapper.eq(Doctor::getStatus, 1);
        wrapper.eq(Doctor::getRole,"doctor");

        List<Doctor> doctorList = doctorMapper.selectList(wrapper);

        List<DoctorVO> voList = new ArrayList<>();

        for (Doctor doctor : doctorList) {

            DoctorVO vo = new DoctorVO();

            vo.setId(doctor.getId());
            vo.setName(doctor.getName());
            vo.setTitle(doctor.getTitle());
            vo.setSkills(doctor.getSkills());
            vo.setAvatar(doctor.getAvatar());

            voList.add(vo);
        }

        return voList;
    }

    @Override
    public List<Doctor> getRecommendDoctor(Integer limit) {
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        // status = 1 正常在岗
        wrapper
                .eq(Doctor::getStatus, 1)
                .eq(Doctor::getRole,"doctor")
                .last("LIMIT " + limit); // 拼接分页，取前N条
        return doctorMapper.selectList(wrapper);
    }

    @Override
    public List<Doctor> searchDoctor(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of(); // 空关键词返回空集合
        }
        String likeStr = "%" + keyword + "%";
        LambdaQueryWrapper<Doctor> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Doctor::getStatus, 1)// 仅在岗医生
                .eq(Doctor::getRole,"doctor")
                .and(w -> w.like(Doctor::getName, likeStr)
                        .or().like(Doctor::getSkills, likeStr));
        return doctorMapper.selectList(wrapper);
    }
}
