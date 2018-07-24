CREATE DEFINER=`root`@`localhost` FUNCTION `commit_url`(commit_hash char(40), project_url varchar(1000)) RETURNS varchar(1000) CHARSET utf8
BEGIN
RETURN CONCAT("[" , substring(commit_hash, 1, 7) , "](", project_url, "/commit/", commit_hash, ")");
END