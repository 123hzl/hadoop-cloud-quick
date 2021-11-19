package com.hzl.hadoop.workflow.mapper;

import org.apache.ibatis.annotations.Mapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.hzl.hadoop.workflow.entity.ApproveHistoryGatewayEntity;


/**
 * 网关审批历史
 * 
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2021-11-04 14:56:40
 */
@Mapper
public interface ApproveHistoryGatewayMapper extends BaseMapper<ApproveHistoryGatewayEntity> {
	
}
