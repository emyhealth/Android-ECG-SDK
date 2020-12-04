package com.yuanxu.ecg.handle;

public abstract class BaseHandler {
    protected BaseHandler next;

    public BaseHandler setNext(BaseHandler next) {
        this.next = next;
        return this;
    }

    public BaseHandler getNext(){
        return next;
    }

    public void handleResponse(String hexResponse) {
        if (handle(hexResponse)) {
            return;
        }
        if (next != null) {
            next.handleResponse(hexResponse);
        }
    }

    /**
     * 处理下位机响应信息
     *
     * @param hexResponse 十六进制字符串形式回复msg
     * @return 是否处理，true表示能处理，false反之
     */
    protected abstract boolean handle(String hexResponse);
}
