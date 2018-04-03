@echo off
@REM Set file path for logging the output of this script
@REM SET logging.file=C:/temp/portalapimigration.log
SET logging.file=portalapimigration.log
java -jar target/PortalMigrationUtil-1.0.jar %*