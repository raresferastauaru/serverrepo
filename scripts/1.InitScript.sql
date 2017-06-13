DROP DATABASE IF EXISTS MyCloudDB;
CREATE DATABASE MyCloudDB;

DROP TABLE IF EXISTS MyCloudDB.Users;
CREATE TABLE MyCloudDB.Users
(
	UserId 				INTEGER				AUTO_INCREMENT PRIMARY KEY,
	UserName 			NVARCHAR(128) 		NOT NULL UNIQUE,
	UserPassword 		NVARCHAR(44)  		NOT NULL
);

DROP TABLE IF EXISTS MyCloudDB.FileHashes;
CREATE TABLE MyCloudDB.FileHashes
(
	HashId 				INT					AUTO_INCREMENT PRIMARY KEY,
	RelativePath 		NVARCHAR(512) 		NOT NULL,
	OldRelativePath 	NVARCHAR(512) 		NOT NULL,
	HashCode 			INT 				NOT NULL,
	OldHashCode 		INT 				NOT NULL,
	UserId 				INT 				NOT NULL,
	LastChange		 	TIMESTAMP,

	CreationTime 		NVARCHAR(255),
	LastWriteTime 		NVARCHAR(255),
	IsReadOnly			NVARCHAR(5),
	IsDeleted			BOOLEAN 			Default 0,

	INDEX UserFK (UserId),
    FOREIGN KEY (UserId)
        REFERENCES MyCloudDB.Users(UserId)
        ON DELETE CASCADE
);
INSERT INTO MyCloudDB.Users VALUES(1, "rares", "PQ9pl92y+cTl0YEs9WrO18U3oFZMpUaxJScWoDnvy4Q=");					-- passw1
INSERT INTO MyCloudDB.Users VALUES(2, "tavi",  "MUv5mKIKHtTlAazRz1Lq6XP33UkcDJL9oqC2cTbTCFA=");					-- passw2

INSERT INTO MyCloudDB.Users VALUES(3, "usr1",  "MUv5mKIKHtTlAazRz1Lq6XP33UkcDJL9oqC2cTbTCFA=");					-- passw2
INSERT INTO MyCloudDB.Users VALUES(4, "usr2",  "MUv5mKIKHtTlAazRz1Lq6XP33UkcDJL9oqC2cTbTCFA=");					-- passw2
INSERT INTO MyCloudDB.Users VALUES(5, "usr3",  "MUv5mKIKHtTlAazRz1Lq6XP33UkcDJL9oqC2cTbTCFA=");					-- passw2


DROP TABLE IF EXISTS MyCloudDB.AssociatedEntities;
CREATE TABLE MyCloudDB.AssociatedEntities
(
	AssociatedEntitiesId 	INT 			AUTO_INCREMENT PRIMARY KEY,
	OdroidName 				NVARCHAR(32),
	OdroidIP 				NVARCHAR(15),
	OdroidPort 				NVARCHAR(5),
	UserName 				NVARCHAR(128),
	ConnectionTime			TIMESTAMP
);
INSERT INTO MyCloudDB.AssociatedEntities(OdroidName, OdroidIP, OdroidPort, UserName) VALUES("Odroid1", "192.168.100.31", "4444", "");
INSERT INTO MyCloudDB.AssociatedEntities(OdroidName, OdroidIP, OdroidPort, UserName) VALUES("Odroid2", "192.168.100.32", "4444", "");



