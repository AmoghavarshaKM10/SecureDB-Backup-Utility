package com.dbbackuputil.service;

import com.dbbackuputil.model.BackUpRequest;

/**
 * @author amoghavarshakm
 */

public interface BackUpService {
	
	String validateConnectionAndCredentails(BackUpRequest input);
	
	void startBackup(BackUpRequest backUpInput);
	

}
