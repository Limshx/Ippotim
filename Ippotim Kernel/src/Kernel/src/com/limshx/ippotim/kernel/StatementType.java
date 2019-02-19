package com.limshx.ippotim.kernel;

public enum StatementType {
    DEFINE("定义"), ASSIGN("赋值"), INPUT("输入"), OUTPUT("输出"), IF("如果"), ELSE("否则"), WHILE("循环"), COMMENT("注释"), BREAK("跳出"), CONTINUE("继续"), CALL("调用"), RETURN("返回"), HEAD("");
    public String name;

    StatementType(String name) {
        this.name = name;
    }
}
