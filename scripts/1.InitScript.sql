DROP DATABASE IF EXISTS MyCloudDB;
CREATE DATABASE MyCloudDB;

DROP TABLE IF EXISTS MyCloudDB.Users;
CREATE TABLE MyCloudDB.Users
(
	UserId 					INTEGER AUTO_INCREMENT PRIMARY KEY,
	UserName 				NVARCHAR(128),
	UserPassword 		NVARCHAR(256)
);

DROP TABLE IF EXISTS MyCloudDB.FileHashes;
CREATE TABLE MyCloudDB.FileHashes
(
	HashId 					INT AUTO_INCREMENT PRIMARY KEY,
	RelativePath 		NVARCHAR(255) NOT NULL,
	OldRelativePath NVARCHAR(255) NOT NULL,
	HashCode 				INT NOT NULL,
	OldHashCode 		INT NOT NULL,
	UserId 					INT NOT NULL,
	LastChange		 	TIMESTAMP,

	CreationTime 		NVARCHAR(255),
	LastWriteTime 	NVARCHAR(255),
	IsReadOnly			NVARCHAR(5),

	INDEX UserFK (UserId),
    FOREIGN KEY (UserId)
        REFERENCES MyCloudDB.Users(UserId)
        ON DELETE CASCADE
);
