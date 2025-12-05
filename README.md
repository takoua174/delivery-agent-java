$files = Get-ChildItem -Path src -Recurse -Filter *.java | ForEach-Object { $_.FullName }
$files | Out-File -FilePath temp_files.txt -Encoding ASCII
javac --% -d bin -sourcepath src @temp_files.txt
java -cp bin code.Main

execute with maven :
mvn -DskipTests compile
mvn -DskipTests exec:java
