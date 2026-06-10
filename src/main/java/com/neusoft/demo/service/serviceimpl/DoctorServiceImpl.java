package com.neusoft.demo.service.serviceimpl;

import com.neusoft.demo.entity.Doctor;
import com.neusoft.demo.mapper.DoctorMapper;
import com.neusoft.demo.service.DoctorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DoctorServiceImpl implements DoctorService {

    @Autowired
    private DoctorMapper doctorMapper;

    @Override
    public List<Doctor> list() {

        return doctorMapper.selectList(null);
    }
}
