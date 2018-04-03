@echo off
@REM Set file path for logging the output of this script
SET logging.file=C:/temp/portalapimigration.log

java -jar target/PortalMigrationUtil-1.0.jar %*