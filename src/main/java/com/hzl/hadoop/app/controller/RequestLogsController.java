package com.hzl.hadoop.app.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 请求日志
 *
 * @author chenshun
 * @email sunlightcs@gmail.com
 * @date 2021-11-19 16:18:12
 */
@RestController
@RequestMapping("quick/test")
public class RequestLogsController {

	/**
	 * 信息
	 */
	@GetMapping("/info")
	public ResponseEntity<Boolean> info() {

		return new ResponseEntity(true, HttpStatus.OK);
	}


}
