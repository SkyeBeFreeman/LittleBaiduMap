package com.zhtian.experimentten;

/**
 * Created by zhtian on 2016/12/2.
 */

public class HotelInfomation {
    private String name;
    private String address;
    private String tel;

    public HotelInfomation(String name, String address, String tel) {
        this.name = name;
        this.address = address;
        this.tel = tel;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setTel(String tel) {
        this.tel = tel;
    }

    public String getTel() {
        return tel;
    }
}
