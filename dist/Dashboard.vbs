Dim shell
Dim fileSystem
Dim scriptDirectory
Dim javaHome
Dim javaExecutable
Dim command

Set shell = WScript.CreateObject("WScript.Shell")
Set fileSystem = CreateObject("Scripting.FileSystemObject")
scriptDirectory = fileSystem.GetParentFolderName(WScript.ScriptFullName)
shell.CurrentDirectory = scriptDirectory

javaHome = shell.Environment.Item("JAVA_HOME")
If javaHome = "" Then
	javaExecutable = "javaw"
Else
	javaExecutable = fileSystem.BuildPath(javaHome, "bin\javaw.exe")
End If

command = Chr(34) & javaExecutable & Chr(34) & " --enable-native-access=ALL-UNNAMED -Dfile.encoding=UTF-8 -jar libs\Dashboard.jar"
shell.Run command, 0, False
