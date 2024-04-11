package com.mrli.openai;

import lombok.Data;

/**
 * 删除对象后的响应结果
 */
@Data
public class DeleteResult {
    /**
     * 删除的对象的ID。
     */
    String id;

    /**
     * 被删除对象的类型，例如"file"或"model"
     */
    String object;

    /**
     * 删除是否成功的标志
     */
    boolean deleted;
}
