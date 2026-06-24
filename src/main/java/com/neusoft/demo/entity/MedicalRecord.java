package com.neusoft.demo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("medical_record")
public class MedicalRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long doctorId;

    private String chiefComplaint;

    private String presentHistory;

    private String checkResult;

    private String diagnosis;

    private String aiDiagnosis;

    private String aiCheckAdvice;

    private String aiDrugAdvice;

    /** 0未确认 1已确认 2修改后确认 3驳回 */
    private Integer aiConfirmStatus;

    private LocalDateTime createTime;

    /** 关联挂号单ID */
    private Long registerOrderId;
}
