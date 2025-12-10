package com.iflytek.astron.workflow.components.id;


/**
 * @author YiHui
 * @date 2023/8/30
 */
public class IdUtil {
    /**
     * 默认的id生成器
     */
    public static IdGenerator DEFAULT_ID_PRODUCER = new IdGenerator();

    /**
     * 生成全局id
     *
     * @return
     */
    public static Long genId() {
        return DEFAULT_ID_PRODUCER.nextId();
    }
}
