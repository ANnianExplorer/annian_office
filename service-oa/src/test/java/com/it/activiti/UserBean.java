package com.it.activiti;

import org.springframework.stereotype.Component;

@Component
public class UserBean {

    public String getUsername(int id) {
        if(id == 1) {
            return "Tom2";
        }
        if(id == 2) {
            return "Jack2";
        }
        return "admin";
    }
}