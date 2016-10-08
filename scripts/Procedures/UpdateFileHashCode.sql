DROP PROCEDURE IF EXISTS MyCloudDB.UpdateFileHashCode;

CREATE PROCEDURE MyCloudDB.UpdateFileHashCode (IN relPath VARCHAR(255), IN hCode INT, IN usrId INT)
BEGIN
	UPDATE MyCloudDB.FileHashes
	SET OldHashCode = HashCode
	WHERE RelativePath = relPath;

	UPDATE  MyCloudDB.FileHashes 
	SET HashCode = hCode
	WHERE RelativePath = relPath;

	INSERT INTO MyCloudDB.FileHashes(RelativePath, HashCode, OldHashCode, UserId)
	SELECT relPath as RelativePath, hCode as HashCode, hCode as OldHashCode, usrId as UserId
	WHERE NOT EXISTS (SELECT * FROM MyCloudDB.FileHashes WHERE RelativePath = relPath);
END