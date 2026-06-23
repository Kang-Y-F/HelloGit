package com.neusoft.demo.dto;
import lombok.Data;
import java.util.List;

@Data
public class DispenseDTO {
    private Long patientId;
    private List<Long> prescriptionIds;
    private String remark;
}

