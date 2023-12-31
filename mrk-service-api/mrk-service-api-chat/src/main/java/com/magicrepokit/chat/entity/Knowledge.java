package com.magicrepokit.chat.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.magicrepokit.mb.base.BaseEntity;
import io.swagger.annotations.ApiModel;
import lombok.*;

@EqualsAndHashCode(callSuper = true)
@TableName(value = "mrk_gpt_knowledge",autoResultMap = true)
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@ApiModel(value = "知识库管理表",description = "知识库管理表")
public class Knowledge extends BaseEntity {
    /**
     * 知识库父级id
     */
    private Long parentId;

    /**
     * 知识库名称
     */
    private String name;

    /**
     * 知识库描述
     */
    private String description;

    /**
     * 级别类型[1:文件夹 2:文件]
     */
    private Integer type;
}
