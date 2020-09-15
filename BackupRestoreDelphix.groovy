package com.datical.hammer.scripts.backup

import com.datical.db.project.DatabaseDef
import com.datical.db.project.Project
///import com.datical.hammer.repl.DaticalDBHelper
import com.datical.hammer.repl.command.DaticalDBHelper
import com.datical.hammer.scripts.backup.BackupRestore
import com.datical.hammer.scripts.util.PackagerProperties
import com.datical.hammer.scripts.util.ScriptLogging
import com.datical.hammer.scripts.util.ScriptUtils
import groovy.sql.Sql
import org.apache.commons.lang3.StringUtils

import java.sql.Connection
import java.text.SimpleDateFormat
import java.util.regex.Pattern


/*******************************************************************************
 * Has methods to perform Oracle database backup and restore using the expdp
 * and impdp command line tools, which need to be available on the OS Path.
 *
 * The filename that is specified for backup/restore/verify can take several
 * forms.
 * 1. ORACLE_DIRECTORY_NAME/filename.dmp
 * 2. RemoteFileSystemPath/filename.dmp
 * 3. ./filename.dmp
 * 4. filename.dmp
 *
 * The filename that is passed in to this method is largely computed by
 * deployPackager.groovy (as of 2/5/2018). deployPackager computes this
 * by looking at deployPackager.properties for the databaseBackupRestoreLocation
 * property.
 *
 * The way this works with deployPackager and database_backup_restore needs
 * to be documented better.
 ******************************************************************************/
class BackupRestoreDelphix implements BackupRestore {

  private ScriptLogging logger = new ScriptLogging('BackupRestoreDelphix')
  private ScriptUtils scriptUtils = new ScriptUtils()
  private DaticalDBHelper daticalDB

  // If a backup restore implementation needs custom per project settings, these could be kept
  // in the deployPackager properties file.
  String packagerPropertiesFile = "deployPackager.properties"

  String defaultExportFilename
  String defaultExportLogFilename
  String jobNameBase

  BackupRestoreDelphix(DaticalDBHelper daticalDB) {
    this.daticalDB = daticalDB
    def startTime = new Date()
    def timestamp            = new SimpleDateFormat("yyyyMMdd_HHmmss_S").format(startTime)
    defaultExportFilename    = "expdp_${timestamp}.dmp"
    defaultExportLogFilename = "expdp_${timestamp}.log"
    def jobnameTimestamp     = new SimpleDateFormat("yyyyMMdd_HHmmss").format(startTime)
    // The jobNameBase cannot be longer than 30 characters on Oracle
    jobNameBase              = "datical_pkg_${jobnameTimestamp}"
    logger.debug "  using BackupRestoreDelphix"
  }


  /***************************************************************************************************************
   * Backup the database specified by the dbDef. Returns a list of [Integer, StringBuffer, StringBuffer], where
   * the Integer is a return code (0 is success, non-zero is an error), the first StringBuffer contains the
   * stderr from any external process, and the second StringBuffer contains the stdout from any external process.
   *
   * @param dbDef               A DatabaseDef object that can be used to create a connection to a database server.
   *                            The dbDef will be automatically constructed based on the name supplied by the
   *                            user on the command line.
   * @param schemaList          A List of Strings, where each String is the name of a schema to backup. This list
   *                            is created by examining the datical.project file, or can be supplied by the user
   *                            on the command line. It may also be calculated by deploy packager from entries in
   *                            various metadata.properties files.
   * @param filename            This is a String used to name the backup file. This value is specified by the user
   *                            on the command line with the option 'backup=filename'
   * @return                    A List of [Integer, StringBuffer, StringBuffer]
   * *************************************************************************************************************/
  @Override
  List runBackup(DatabaseDef dbDef, List<String> schemaList, String filename) {
    def i
    def rc = 0
    def stdout = new StringBuffer()
    def stderr = new StringBuffer()
    def sid_service

    // Delphix snapshot needs these three inputs:
    //	1. Delphix engine name --> This will have to be passed in as another argument
    //	2. Database service name --> We already have this available via ${databaseDef}
    //	3. Database type (e.g., "vdb") --> For now, we will hardcode "vdb" as this is the only database we support

    try {
      ProcessBuilder processBuilder = new ProcessBuilder("");
      logger.printInfo "Starting Delphix Snapshot"
      def snapshotCommand = "/opt/datical/dxtoolkit2/dx_snapshot_db"
      if (StringUtils.isNotBlank(dbDef.sid)) {
        sid_service = dbDef.sid
      }
      else if (StringUtils.isNotBlank(dbDef.serviceName)) {
        sid_service=dbDef.serviceName
      }
      else {
         logger.printError "No SID or Service was specified in the connection (${dbDef.name})."
        return [1,null,null]
      }
      def delphixEngineName = "delphix-vm-n-6"
      def delphixDBType = "vdb"
      String[] snapshotOptions = [
        "-engine", "${delphixEngineName}",
        "-name", dbDef.name,
        "-type", "${delphixDBType}",
        "-configfile", "./dxtools.conf"
      ]

      // Build command list:
      def commands = processBuilder.command()
      commands.clear()
      commands.add(snapshotCommand.toString())
      // commands.add(connectString.toString())
      // expdpOptions.eachWithIndex { item, index ->
      snapshotOptions.eachWithIndex { item, index ->
        commands.add(item.toString())
      }

      logger.debug "executing ${snapshotCommand} command jay:\n " +
          "${snapshotCommand} "+buildConnectionString(dbDef, false/*no password*/)+"${snapshotOptions.join(" ")}"

      Process proc = processBuilder.start()
      proc.waitForProcessOutput(stdout, stderr)
      rc = proc.exitValue()
    }
    catch (Exception ex) {
      stderr = ex
      rc = 1
    }

    def message = """
       rc    : ${rc}
       stderr: ${stderr}
       stdout: ${stdout}
----------------------------- end of Delphix snpahot output ---------------------------------
"""
    logger.printInfo message
    if (rc != 0) {
      logger.printError message
    }
    else {
      logger.printInfo "  Successfully exported the database '${dbDef.name}'"
      logger.debug stdout.toString()
    }
    return [rc,stderr,stdout]
  }

  private String buildConnectionString(final DatabaseDef dbDef, boolean showPassword) {
    String password = showPassword ? dbDef.password : "*******"
    String connectString = "${dbDef.username}/\"${password}\"@${dbDef.hostname}:${dbDef.port}"

    if (StringUtils.isNotBlank(dbDef.sid)) {
      return connectString+":"+dbDef.sid
    }
    if (StringUtils.isNotBlank(dbDef.serviceName)) {
      return connectString+"/"+dbDef.serviceName
    }
    // return null
	return [rc,stderr,stdout]
  }

  /**
   * Restore the database specified by the dbDef using the filename specified. Returns a list of
   * [Integer, StringBuffer, StringBuffer], where the Integer is a return code (0 is success, non-zero is an
   * error), the first StringBuffer contains the stderr from any external process, and the second StringBuffer
   * contains the stdout from any external process.
   *
   * @param dbDef               A DatabaseDef object that can be used to create a connection to a database server.
   *                            The dbDef will be automatically constructed based on the name supplied by the
   *                            user on the command line.
   * @param schemaList          A List of Strings, where each String is the name of a schema to backup. This list
   *                            is created by examining the datical.project file, or can be supplied by the user
   *                            on the command line. It may also be calculated by deploy packager from entries in
   *                            various metadata.properties files.
   * @param filename            This is a string that should be used to find the file that will be restored. This value
   *                            is specified by the user on the command line with the option 'restore=filename'
   *
   * @return                    A List of [Integer, StringBuffer, StringBuffer]
   *
   * *************************************************************************************************************/
  @Override
  List runRestore(DatabaseDef dbDef, List<String> schemaList, String filename) {
    def i
    def rc = 0
    def stdout = new StringBuffer()
    def stderr = new StringBuffer()
    def sid_service


    try {
      ProcessBuilder processBuilder = new ProcessBuilder("")
      logger.printInfo "Starting Delphix Rewind"
      def rewindCommand = "/opt/datical/dxtoolkit2/dx_rewind_db"
      String connectString
      String printableConnectString
      if (StringUtils.isNotBlank(dbDef.sid)) {
        sid_service = dbDef.sid
      }
      else if (StringUtils.isNotBlank(dbDef.serviceName)) {
        sid_service=dbDef.serviceName
      }
      else {
        logger.printError "No SID or Service was specified in the connection (${dbDef.name})."
        return [1,null,null]
      }

      def delphixEngineName = "delphix-vm-n-6"
      def delphixDBType = "vdb"
      String[] rewindOptions = [
          "-engine", "${delphixEngineName}",
          "-name" , dbDef.name,
          "-type" ,"${delphixDBType}",
          "-configfile", "./dxtools.conf"
      ]

      def commands = processBuilder.command()
      commands.clear()
      commands.add(rewindCommand.toString())
      rewindOptions.eachWithIndex { item, index ->
        commands.add(item.toString())
      }

      logger.debug "executing Delphix rewind command:\n   " +
              "${rewindCommand} "+buildConnectionString(dbDef, false/*no password*/)+"${rewindOptions.join(" ")}"

      Process proc = processBuilder.start()
      proc.waitForProcessOutput(stdout, stderr)
      rc = proc.exitValue()
    }
    catch (Exception ex) {
      stderr = ex
      rc = 1
    }

    def message = """
      rc    : ${rc}
      stderr: ${stderr}
      stdout: ${stdout}
----------------------------- end of Delphix rewind output ---------------------------------
"""
    logger.printInfo message
    if (rc != 0) {
      logger.printError message
    }
    else {
      logger.debug stdout.toString()
    }

    if (rc == 5) {
      logger.printWarn "\n*** Warnings occurred during the import of the restore file: ${filename} ***"
      rc = 0
    }

    return [rc,stderr,stdout]
  }

  /***************************************************************************************************************
   * This method is called just after the runRestore method. Does nothing in this implementation.
   *
   * @param project             This will be populated with the Project object that deployPackager/backup/restore
   *                            is currently working with.
   * @param dbDef               A DatabaseDef object that can be used to create a connection to a database server.
   *                            The dbDef will be automatically constructed based on the name supplied by the
   *                            user on the command line.
   *
   * returns an integer. Zero indicates success, non-zero is failure.
   * *************************************************************************************************************/
  @Override
  int postRestore(Project project, DatabaseDef databaseDef) {
    return 0
  }

  /***************************************************************************************************************
   * Verify that the backup restore class is able to perform the operations. Returns a list of [Integer, StringBuffer, StringBuffer], where
   * the Integer is a return code (0 is success, non-zero is an error), the first StringBuffer contains the
   * stderr from any external process, and the second StringBuffer contains the stdout from any external process.
   *
   * @param dbDef               A DatabaseDef object that can be used to create a connection to a database server.
   *                            The dbDef will be automatically constructed based on the name supplied by the
   *                            user on the command line.
   * @param forBackup           This will be true if the system is preparing to do a backup. It will be false if
   *                            the system is preparing to do a restore.
   * @param filename            A String that refers to the backup file. The value is specified by the user on the
   *                            command line with the option 'backup=filename' or 'restore=filename'. This name
   *
   *
   * *************************************************************************************************************/
  @Override
  List verify(DatabaseDef dbDef, boolean forBackup, String filename) {
    def errorCode = 0
    StringBuffer messages = new StringBuffer()

    return [errorCode, messages.toString()]
  }



  /***************************************************************************************************************
   * This method is called just prior to the runRestore method. This implementation uses this method to drop
   * drop the schemas that are about to be restored.
   *
   * @param project             This will be populated with the Project object that deployPackager/backup/restore
   *                            is currently working with.
   * @param dbDef               A DatabaseDef object that can be used to create a connection to a database server.
   *                            The dbDef will be automatically constructed based on the name supplied by the
   *                            user on the command line.
   * @param schemaList          A List of Strings, where each String is the name of a schema to backup. This list
   *                            is created by examining the datical.project file, or can be supplied by the user
   *                            on the command line. It may also be calculated by deploy packager from entries in
   *                            various metadata.properties files.
   * *************************************************************************************************************/
  @Override
  int preRestore(Project project, DatabaseDef dbDef, List<String> schemaList) {

    return 0
  }


   /***************************************************************************************************************
   * The DatabaseBackupRestore class calls this method to allow an engine to return the name of the file to be
   * used for backup, restore, and verify operations. It is up to each engine to determine the best way to do
   * this. Most of the engines that are supplied by Datical use a property in deployPackager.properties
   *
   * @param project             This will be populated with the Project object that deployPackager/backup/restore
   *                            is currently working with.
   * @param dbDef               A DatabaseDef object that can be used to create a connection to a database server.
   *                            The dbDef will be automatically constructed based on the name supplied by the
   *                            user on the command line.
   * @param forBackup           If the filename is different for backup and restore, this parameter can be
   *                            used to control which filename is returned. If forBackup is set to true, then
   *                            the implementation should return a filename for a backup operation, and if
   *                            this is set to false it should return a filename for a restore operation.
   *
   * Returns a String. This String will then be used in the calls to runBackup, verify, and runRestore.
   * *************************************************************************************************************/
  @Override
  String getFilename(Project project, DatabaseDef dbDef, boolean forBackup) {
    return "backupFile.dmp"
  }
}
