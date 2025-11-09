package com.dbbackuputil.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import com.dbbackuputil.model.BackUpRequest;
import com.dbbackuputil.service.BackUpService;

import lombok.extern.slf4j.Slf4j;

/** 
 * 
 * @author amoghavarshakm
 * 
 */

@Controller
@Slf4j
@RequestMapping("dbBackUpUtil")
public class DBBackUpController {
	
	private BackUpService backUpService;
	
	public DBBackUpController(BackUpService backUpService) {
		this.backUpService = backUpService;
	}
	
	
	@GetMapping("/validateConnection")
	public ResponseEntity<?> validateCredentailsAndConnection(@RequestBody BackUpRequest inputRequest){
		log.info("validateCredentailsAndConnection entry for User:{}");
		ResponseEntity<?> response = null;
		try {
			inputRequest.setDbUser("admin");
			String status = backUpService.validateConnectionAndCredentails(inputRequest);
			response = ResponseEntity.status(HttpStatus.OK).body(status);
		} catch (Exception ex) {
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
			log.error("validateCredentailsAndConnection Failed with Exceptions:{}",ex.getMessage());
			throw ex;
		}
		log.info("validateCredentailsAndConnection exit");
		return response;
	}

	
	
	
	@PostMapping("/backupRequest")
	public ResponseEntity<?> startRequest(@RequestBody BackUpRequest inputRequest) {
		log.info("startBackUpRequest entry for User:{}");
		ResponseEntity<?> response = null;
		try {
			
			response = ResponseEntity.status(HttpStatus.ACCEPTED).body("BackUp Started");
		} catch (Exception ex) {
			response = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to Start BackUp");
			//adit to db as well;
			log.error("startBackupRequest Failed with Exceptions:{}",ex.getMessage());
			throw ex;
		}
		log.info("startBackUpRequest exit");
		return response;
	}
	
	@PostMapping("/backUpDashBoard")
	public ResponseEntity<?> getDashBoardDetails(){
		return null;
		
	}
	
	@GetMapping("/downloadBackup")
	public ResponseEntity<?> downloadBackUp(){
		return null;
		
	}
	
	@PostMapping("/restore")
	public ResponseEntity<?> restoreBackup() {
		return null;
		
	}
 
}

