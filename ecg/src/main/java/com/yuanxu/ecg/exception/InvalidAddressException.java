package com.yuanxu.ecg.exception;

public class InvalidAddressException extends Exception {
    private String invalidAddress;

    public InvalidAddressException(String address) {
        super("Invalid address:" + address);
        invalidAddress = address;
    }

    public String getInvalidAddress() {
        return invalidAddress;
    }
}
