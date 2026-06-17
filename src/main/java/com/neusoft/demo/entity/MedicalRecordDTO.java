package com.neusoft.demo.entity;

import lombok.Data;

/**
 * 创建病历请求参数
 */
@Data
public class MedicalRecordDTO {

    private Long userId;

    private Long registerOrderId;

    private String chiefComplaint;

    private String presentHistory;

    private String checkResult;

    private String diagnosis;
}
