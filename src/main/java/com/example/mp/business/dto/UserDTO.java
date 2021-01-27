package com.example.mp.business.dto;

import lombok.Data;

@Data
public class UserDTO {
    /** user */
    private Integer id;
    /** user */
    private String name;
    /** user */
    private String sex;
    /** user */
    private String headImg;
    /** user_address */
    private String tel;
    /** user_address */
    private String address;
    /** area */
    private String province;
    /** area */
    private String city;
    /** area */
    private String area;
}