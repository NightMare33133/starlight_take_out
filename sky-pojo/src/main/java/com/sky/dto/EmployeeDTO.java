package com.sky.dto;

import lombok.Data;

import java.io.Serializable;

@Data
//Serializable代表这个类的对象可以被序列化，
// 序列化是指将对象转换为字节流的过程，以便在网络上传输或者保存到文件中。
// 实现Serializable接口的类可以被Java的序列化机制自动处理，使得对象能够被正确地序列化和反序列化。
public class EmployeeDTO implements Serializable {

    private Long id;

    private String username;

    private String name;

    private String phone;

    private String sex;

    private String idNumber;

}
