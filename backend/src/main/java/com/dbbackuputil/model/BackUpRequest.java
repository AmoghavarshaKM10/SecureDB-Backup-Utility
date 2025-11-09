package com.dbbackuputil.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 
 * @author amoghavarshakm
 * 
 * 
 */

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BackUpRequest {
	
	private String dbName;
	private String url;
	private String password;
	private String backupType;
	private String backUpLocation;
	private String dbUser;
	private String dbType;
}
