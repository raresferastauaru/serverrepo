DROP PROCEDURE IF EXISTS MyCloudDB.UpdateFileHashRelativePath;

CREATE PROCEDURE MyCloudDB.UpdateFileHashRelativePath (IN relPath VARCHAR(255), IN newRelPath VARCHAR(255), IN usrId INT)
BEGIN
	UPDATE MyCloudDB.FileHashes
	SET OldRelativePath = RelativePath, RelativePath = newRelPath
	WHERE RelativePath = relPath AND UserId = usrId;
END
