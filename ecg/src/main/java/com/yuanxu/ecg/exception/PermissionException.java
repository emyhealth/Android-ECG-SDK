package com.yuanxu.ecg.exception;

public class PermissionException extends Exception {

    private String missingPermission;

    public PermissionException(String missingPermission) {
        super("no permission:" + missingPermission);
        this.missingPermission = missingPermission;
    }

    public String getMissingPermission() {
        return missingPermission;
    }
}
