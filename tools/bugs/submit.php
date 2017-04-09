<?php
include 'conn.php';

$os_in = filter_input(INPUT_POST, 'os');
$java_in = filter_input(INPUT_POST, 'java');
$version_in = filter_input(INPUT_POST, 'version');
$log_in = filter_input(INPUT_POST, 'log');

//Find existing bug report
$dbh = con(); //Get connection
$stmt = $dbh->prepare("SELECT * FROM ".table()." where log = ?");
if ($stmt->execute(array($log_in))) {
	while ($row = $stmt->fetch()) {
		$foundRow = $row;
		break;
	}
}

if (empty($foundRow)) { //New bug report
	$count = '1';
	$stmt = $dbh->prepare("INSERT INTO ".table()." (os,java,version,log,count) VALUES (:os, :java, :version, :log, :count)");
	$stmt->bindParam(':os', $os_in);
	$stmt->bindParam(':java', $java_in);
	$stmt->bindParam(':version', $version_in);
	$stmt->bindParam(':log', $log_in);
	$stmt->bindParam(':count', $count);
	$stmt->execute();
	$id = $dbh->lastInsertId();

	print $id;

	$to      = 'nkr@niklaskr.dk';
	$subject = "New " . name() . " bug report";
	$message = name() . " bug report\r\n"
				."BugID: " . $id . "\r\n"
				.buglink()."#bugid".$id."\r\n"
				."\r\n"
				.$log_in
				;
	$headers = 'From: nkr@niklaskr.dk' . "\r\n" .
			 'X-Mailer: PHP/' . phpversion();
	
	mail($to, $subject, $message, $headers);
} else { //Old bug report - Add: count, os, java, version - Update: status
	$count = $foundRow['count'];
	$count++;
	$id = $foundRow['id'];
	if ($foundRow['status'] == 4) {
		$status = -1;
	} else {
		$status = $foundRow['status'];
	}
	$os = add($foundRow['os'], $os_in);
	$java = add($foundRow['java'], $java_in);
	$version = add($foundRow['version'], $version_in);
	$stmt = $dbh->prepare("UPDATE ".table()." SET os=:os,java=:java,version=:version,count=:count,status=:status,date=current_timestamp WHERE id=:id");
	$stmt->bindParam(':os', $os);
	$stmt->bindParam(':java', $java);
	$stmt->bindParam(':version', $version);
	$stmt->bindParam(':count', $count);
	$stmt->bindParam(':id', $id);
	$stmt->bindParam(':status', $status);
	$stmt->execute();
	
	print $id;
}

function add($in, $add) {
	$out = explode(";", $in); //Split to array
	$out[] = $add; //Add new
	$out = array_unique($out); //Remove dubs
	return implode(";", $out); //Return string
}
?>