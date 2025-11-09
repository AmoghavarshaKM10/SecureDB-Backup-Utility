package com.dbbackuputil.service;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.dbbackuputil.model.BackUpRequest;
import com.dbbackuputil.utility.BackUpHelper;

import lombok.extern.slf4j.Slf4j;

/**
 * @author amoghavarshakm
 */

@Slf4j
@Service
public class BackUpServiceImpl implements BackUpService {

	@Override
	public String validateConnectionAndCredentails(BackUpRequest input) {
		log.info("validateConnectionAndCredentails service entry()");
		String message = null;
		try {
			// String username = BackUpHelper.decrypt(input.getPassword());
			String dbName = input.getDbName();
			String host = input.getUrl();
			String dbType = input.getDbType();
			String username = input.getDbUser();
			String password = input.getPassword();
			boolean connection = testDBConnection(dbType, host, dbName, username, password);
			if (connection) {
				message = "Success";
			} else {
				message = "Failure";
			}
		} catch (Exception ex) {
			log.error("validateConnectionAndCredentails service error:{}", ex.getMessage());
			throw new RuntimeException(ex);
		}
		return message;
	}

	@Async
	@Override
	public void startBackup(BackUpRequest backUpInput) {
		String message = null;
		try {
			// String username = BackUpHelper.decrypt(input.getPassword());
			String dbName = backUpInput.getDbName();
			String host = backUpInput.getUrl();
			String dbType = backUpInput.getDbType();
			String username = backUpInput.getDbUser();
			String password = backUpInput.getPassword();
			String backupFolder = null;
			boolean backUpType = true;
			boolean connection = backupDatabase(dbType, host, dbName, username, password,backupFolder,backUpType);
			if (connection) {
				message = "Success";
			} else {
				message = "Failure";
			}
		} catch (Exception ex) {
			log.error("validateConnectionAndCredentails service error:{}", ex.getMessage());
			throw new RuntimeException(ex);
		}
	}

	public static boolean testDBConnection(String dbType, String host, String dbName, String username,
			String password) {

		List<String> command = new ArrayList<>();
		String hostname = host;
		String port = null;

		if (host.contains(":")) {
			String[] parts = host.split(":");
			hostname = parts[0];
			port = parts[1];
		}

		switch (dbType.toLowerCase()) {
		case "mysql":

			break;
		case "postgres":
			command = Arrays.asList("psql", "-h", hostname, "-p", port, "-U", username, "-d", dbName, "-c",
					"SELECT 1;");
			break;
		default:
			throw new UnsupportedOperationException("DB type not supported: " + dbType);
		}

		return runProcess(command, dbType, password, 10);
	}

	private static boolean runProcess(List<String> command, String dbType, String password, int timeoutSeconds) {
		try {
			ProcessBuilder pb = new ProcessBuilder(command);

			// PostgreSQL password via environment variable
			if ("postgres".equalsIgnoreCase(dbType)) {
				pb.environment().put("PGPASSWORD", password);
			}

			pb.redirectErrorStream(true);
			Process process = pb.start();

			// Optional: read output for debug
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				while ((line = reader.readLine()) != null) {
					System.out.println(line); // log output
				}
			}

			if (timeoutSeconds > 0) {
				boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
				if (!finished) {
					process.destroyForcibly();
					throw new RuntimeException("DB CLI command timed out");
				}
			} else {
				process.waitFor();
			}

			int exitCode = process.exitValue();
			return exitCode == 0;

		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return false;
		}
	}

	public static boolean backupDatabase(String dbType, String host, String dbName, String username, String password,
			String backupFolder, boolean fullBackup) throws Exception {
		List<String> command = new ArrayList<>();
		String hostname = host;
		String port = null;

		if (host.contains(":")) {
			String[] parts = host.split(":");
			hostname = parts[0];
			port = parts[1];
		}

		
		String timestamp = String.valueOf(System.currentTimeMillis());
		String tempBackupPath = null;
		String zipFilePath = backupFolder + "/" + dbName + "_backup_" + timestamp + ".zip";

		switch (dbType.toLowerCase()) {
		case "postgres":
			if (fullBackup) {
				tempBackupPath = "/tmp/" + dbName + "_full_backup.dump";
				command = Arrays.asList("pg_dump", "-h", hostname, port != null ? "-p" : "", port != null ? port : "",
						"-U", username, "-F", "c", "-b", "-v", "-f", tempBackupPath, dbName);
			} else {

				tempBackupPath = "/tmp/" + dbName + "_incremental_wal";
				Files.createDirectories(Paths.get(tempBackupPath));
				command = Arrays.asList("cp", "-r", "/var/lib/postgresql/wal_archive/*", tempBackupPath);
			}
			command.removeIf(String::isEmpty);
			break;

		case "mysql":
			if (fullBackup) {
				tempBackupPath = "/tmp/" + dbName + "_full_backup.sql";
				command = Arrays.asList("mysqldump", "-h", hostname, port != null ? "-P" : "", port != null ? port : "",
						"-u", username, "-p" + password, dbName, "--single-transaction", "-r", tempBackupPath);
			} else {
				tempBackupPath = "/tmp/" + dbName + "_incremental_binlog.sql";
// Copy binary log or use mysqlbinlog command
				command = Arrays.asList("mysqlbinlog", "mysql-bin.000001", "-r", tempBackupPath);
			}
			command.removeIf(String::isEmpty);
			break;

		case "mongodb":
			tempBackupPath = "/tmp/" + dbName + "_backup_" + (fullBackup ? "full" : "incremental");
			command = new ArrayList<>(Arrays.asList("mongodump", "--host", hostname, port != null ? "--port" : "",
					port != null ? port : "", "--db", dbName, "--out", tempBackupPath));
			if (!fullBackup) {
				command.add("--oplog");
			}
			command.removeIf(String::isEmpty);
			break;

		default:
			throw new UnsupportedOperationException("DB type not supported: " + dbType);
		}

// Run backup
		boolean success = runProcess(command, dbType, password, 300);
		if (!success)
			return false;

// Zip full backup only
		if (fullBackup) {
			File zipFile = new File(zipFilePath);
			if (dbType.equalsIgnoreCase("mongodb")) {
			//	zipFolder(new File(tempBackupPath), zipFile);
			} else {
			//	zipFile(tempBackupPath, zipFilePath);
			}
			System.out.println("Backup zipped at: " + zipFilePath);
			//deleteRecursively(new File(tempBackupPath));
		} else {
			System.out.println("Incremental backup stored at: " + tempBackupPath);
		}

		return true;
	}

}
