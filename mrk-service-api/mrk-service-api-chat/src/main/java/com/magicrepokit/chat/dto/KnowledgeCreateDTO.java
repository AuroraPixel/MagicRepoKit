package com.magicrepokit.chat.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@ApiModel(value = "知识库创建实体",description = "知识库创建")
public class KnowledgeCreateDTO {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @ApiModelProperty(value = "父id")
    private Long parentId;

    /**
     * 知识库名称
     */
    @ApiModelProperty(value = "知识库名称")
    private String name;

    /**
     * 知识库描述(类型为文件夹不填)
     */
    @ApiModelProperty(value = "知识库描述")
    private String description;

    /**
     * 级别类型[1:文件夹 2:文件]
     */
    @ApiModelProperty(value = "级别类型[1:文件夹 2:文件]")
    private Integer type;
}