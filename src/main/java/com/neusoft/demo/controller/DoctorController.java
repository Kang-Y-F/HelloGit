package com.neusoft.demo.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.neusoft.demo.common.Result;
import com.neusoft.demo.dto.DoctorAddDTO;
import com.neusoft.demo.dto.LoginDTO;
import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.entity.RegisterOrder;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.mapper.RegisterOrderMapper;
import com.neusoft.demo.service.DoctorService;
import com.neusoft.demo.service.McpToolService;
import com.neusoft.demo.service.ScheduleService;
import com.neusoft.demo.utils.JwtUtil;
import com.neusoft.demo.vo.LoginVO;
import com.neusoft.demo.vo.ScheduleVO;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/doctor")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private DoctorMapper doctorMapper;

    @Autowired
    private RegisterOrderMapper registerOrderMapper;

    @Autowired
    private ChatClient chatClient;

    // 新增：可选注入MCP工具服务，MCP未就绪时自动降级，不影响原有分诊功能
    @Autowired(required = false)
    private McpToolService mcpToolService;

    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody LoginDTO loginDTO){

        try {
            LoginVO loginVO = doctorService.login(loginDTO);

            if (loginVO == null) {
                return Result.fail("账号或密码错误");
            }

            return Result.success(loginVO);
        } catch (RuntimeException e) {
            return Result.fail(e.getMessage());
        }
    }

    @PostMapping("/add")
    public Result<String> addDoctor(
            @RequestBody DoctorAddDTO dto){

        doctorService.addDoctor(dto);

        return Result.success("新增医生成功");
    }

    @GetMapping("/list")
    public Result<List<Doctor>> list(){

        List<Doctor> list = doctorService.list();

        return Result.success(list);

    }

    @GetMapping("/queue")
    public Result<?> queue(HttpServletRequest request) {
        return Result.success(doctorService.getTodayQueue(parseDoctorId(request)));
    }

    @PutMapping("/call/{id}")
    public Result<String> callNext(@PathVariable Long id) {
        return doctorService.callNext(id) ? Result.success("叫号成功") : Result.fail("叫号失败");
    }

    @PutMapping("/finish/{id}")
    public Result<String> finish(@PathVariable Long id) {
        return doctorService.finishConsult(id) ? Result.success("接诊完成") : Result.fail("操作失败");
    }

    // ── 新增接口 ──────────────────────────────────────────────────

    /** 医生个人资料 + 今日统计 */
    @GetMapping("/profile")
    public Result<?> profile(HttpServletRequest request) {
        Long doctorId = parseDoctorId(request);
        Doctor doctor = doctorMapper.selectById(doctorId);
        if (doctor == null) return Result.fail("医生不存在");
        doctor.setPassword(null);

        long todayTotal    = count(doctorId, null,  null);
        long todayFinished = countFinishedToday(doctorId);   // ← 改成专门的方法
        long todayUrgent   = count(doctorId, "priority", "1");

        Map<String, Object> result = new HashMap<>();
        result.put("doctor",        doctor);
        result.put("todayTotal",    todayTotal);
        result.put("todayFinished", todayFinished);
        result.put("todayUrgent",   todayUrgent);
        result.put("todayWaiting",  todayTotal - todayFinished);
        return Result.success(result);
    }

    // 新增：按"完成时间是今天"统计，而不是按挂号时间
    private long countFinishedToday(Long doctorId) {
        LambdaQueryWrapper<RegisterOrder> w = new LambdaQueryWrapper<RegisterOrder>()
                .eq(RegisterOrder::getDoctorId, doctorId)
                .eq(RegisterOrder::getStatus, 4)
                .apply("DATE(finish_time) = CURDATE()");
        return registerOrderMapper.selectCount(w);
    }

    /** 搜索患者 */
    @GetMapping("/search-patient")
    public Result<?> searchPatient(@RequestParam String keyword) {
        return Result.success(doctorMapper.searchPatient(keyword));
    }

    /**
     * AI 智能分诊
     * 根据症状描述，AI 给出初步分诊建议和检查推荐
     * 升级：优先走MCP工具增强（检索医学知识库），MCP不可用时自动降级为原有纯prompt模式
     */
    @PostMapping("/triage")
    public Result<?> triage(@RequestBody Map<String, String> body) {
        String symptoms = body.getOrDefault("symptoms", "");
        if (symptoms.isBlank()) return Result.fail("请输入症状描述");

        try {
            String response;
            if (mcpToolService != null) {
                try {
                    log.info("使用MCP增强智能分诊: symptoms={}", symptoms);
                    response = mcpToolService.triageWithMcp(symptoms);
                } catch (Exception e) {
                    log.warn("MCP智能分诊失败，降级为普通对话模式", e);
                    response = plainTriage(symptoms);
                }
            } else {
                response = plainTriage(symptoms);
            }
            return Result.success(response);
        } catch (Exception e) {
            return Result.fail("AI服务暂时不可用：" + e.getMessage());
        }
    }

    /** 原有的纯prompt分诊逻辑，作为MCP不可用时的降级兜底 */
    private String plainTriage(String symptoms) {
        String prompt = String.format("""
                你是一名专业的脑科分诊AI。患者描述如下症状：
                
                %s
                
                请按照以下格式给出分诊建议（简洁、专业）：
                【初步判断】
                （描述可能的病情方向，1-2句话）
                【建议检查】
                （列出建议优先做的检查项目，用顿号分隔）
                【就诊紧急度】
                （普通 / 较紧急 / 急诊，并说明原因）
                【注意事项】
                （患者就诊前的注意事项，1-2条）
                """, symptoms);

        return chatClient.prompt().user(prompt).call().content();
    }

    // ── 工具 ──────────────────────────────────────────────────────

    private Long parseDoctorId(HttpServletRequest request) {
        Claims claims = JwtUtil.parseToken(request.getHeader("token"));
        return claims.get("userId", Long.class);
    }

    private long count(Long doctorId, String field, String value) {
        LambdaQueryWrapper<RegisterOrder> w = new LambdaQueryWrapper<RegisterOrder>()
                .eq(RegisterOrder::getDoctorId, doctorId)
                .apply("DATE(create_time) = CURDATE()");
        if ("status".equals(field))   w.eq(RegisterOrder::getStatus,   Integer.parseInt(value));
        if ("priority".equals(field)) w.eq(RegisterOrder::getPriority, Integer.parseInt(value));
        return registerOrderMapper.selectCount(w);
    }

    @GetMapping("/{doctorId}/schedule")
    public Result<List<ScheduleVO>> getSchedule(@PathVariable Long doctorId) {

        return Result.success(scheduleService.getDoctorSchedule(doctorId));
    }

    @GetMapping("/listByDept/{deptId}")
    public Result<?> listByDept(@PathVariable Long deptId) {

        return Result.success(doctorService.listByDept(deptId));
    }

    /**
     * 首页推荐医生（在岗医生 取前6条）
     */
    @GetMapping("/recommend")
    public Result<List<Doctor>> getRecommendDoctor() {
        List<Doctor> list = doctorService.getRecommendDoctor(6);
        // 遍历清空密码
        list.forEach(doc -> doc.setPassword(null));
        return Result.success(list);
    }

    /**
     * 搜索医生（姓名、擅长技能 模糊查询）
     */
    @GetMapping("/search")
    public Result<List<Doctor>> searchDoctor(@RequestParam String keyword) {
        List<Doctor> list = doctorService.searchDoctor(keyword);
        list.forEach(doc -> doc.setPassword(null));
        return Result.success(list);
    }

    // ===================== 医生管理模块（管理员用） =====================

    /**
     * 查询医生列表（可按审核状态筛选）
     */
    @GetMapping("/page")
    public Result<?> queryDoctorList(@RequestParam(required = false) Integer auditStatus) {

        return Result.success(doctorService.list(auditStatus));
    }

    /**
     * 审核医生账号（通过/拒绝）
     */
    @PutMapping("/review/{id}")
    public Result<?> reviewDoctor(@PathVariable Long id, @RequestParam Integer auditStatus) {

        doctorService.auditDoctor(id, auditStatus);

        return Result.success("操作成功");
    }

    /**
     * 启用医生账号
     */
    @PutMapping("/enable/{id}")
    public Result<?> enableDoctor(@PathVariable Long id) {

        doctorService.updateStatus(id, 1);

        return Result.success("启用成功");
    }

    /**
     * 禁用医生账号
     */
    @PutMapping("/disable/{id}")
    public Result<?> disableDoctor(@PathVariable Long id) {

        doctorService.updateStatus(id, 0);

        return Result.success("禁用成功");
    }

    /**
     * 修改医生角色
     */
    @PutMapping("/assign-role/{id}")
    public Result<?> assignRole(@PathVariable Long id, @RequestParam String role) {

        doctorService.updateRole(id, role);

        return Result.success("角色修改成功");
    }
}