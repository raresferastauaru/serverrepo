DROP PROCEDURE IF EXISTS MyCloudDB.UpdateFileHashDirectoryRelativePath;

CREATE PROCEDURE MyCloudDB.UpdateFileHashDirectoryRelativePath (IN relPath VARCHAR(255), IN newRelPath VARCHAR(255), IN usrId INT)
BEGIN
	UPDATE MyCloudDB.FileHashes
	SET OldRelativePath = RelativePath
	WHERE RelativePath LIKE CONCAT(relPath,"%") AND UserId = usrId;

	UPDATE MyCloudDB.FileHashes
	SET RelativePath = REPLACE(RelativePath, relPath, newRelPath)
	WHERE RelativePath LIKE CONCAT(relPath,"%") AND UserId = usrId;
END
