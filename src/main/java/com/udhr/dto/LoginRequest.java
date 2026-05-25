package com.udhr.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String staffNumber;
    private String password;
}
