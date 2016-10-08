DROP PROCEDURE IF EXISTS MyCloudDB.UpdateFileHashRelativePath;

CREATE PROCEDURE MyCloudDB.UpdateFileHashRelativePath (IN relPath VARCHAR(255), IN hCode INT, IN usrId INT)
BEGIN
	UPDATE MyCloudDB.FileHashes
	SET RelativePath = newRelativePath
	WHERE RelativePath = oldRelativePath;
END