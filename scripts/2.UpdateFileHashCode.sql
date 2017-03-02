DROP PROCEDURE IF EXISTS MyCloudDB.UpdateFileHashCode;
CREATE PROCEDURE MyCloudDB.UpdateFileHashCode (IN usrId INT, IN relPath VARCHAR(255), IN hCode INT, IN creationTime VARCHAR(255), IN lastWriteTime VARCHAR(255), IN isReadOnly VARCHAR(5))
BEGIN
	UPDATE MyCloudDB.FileHashes
	SET OldHashCode = HashCode, HashCode = hCode, IsDeleted = 0
	WHERE RelativePath = relPath AND UserId = usrId;

	INSERT INTO MyCloudDB.FileHashes(RelativePath, OldRelativePath, HashCode, OldHashCode, UserId, CreationTime, LastWriteTime, IsReadOnly)
	SELECT relPath as RelativePath, relPath as OldRelativePath, hCode as HashCode, hCode as OldHashCode, usrId as UserId,
				 creationTime as CreationTime, lastWriteTime as LastWriteTime, isReadOnly as IsReadOnly
	WHERE NOT EXISTS (SELECT * FROM MyCloudDB.FileHashes WHERE RelativePath = relPath AND UserId = usrId);
END
