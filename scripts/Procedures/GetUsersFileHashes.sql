DROP PROCEDURE IF EXISTS MyCloudDB.GetUsersFileHashes;
CREATE OR REPLACE PROCEDURE MyCloudDB.GetUsersFileHashes()
BEGIN
	SELECT * FROM MyCloudDB.FileHashes;
END

# Trebuie sa aduci UserId-ul ca parametru si sa filtrezi dupa el !!!