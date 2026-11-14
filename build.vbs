' 
' Copyright (c) 2013 L2jMobius
' 
' Permission is hereby granted, free of charge, to any person obtaining a copy
' of this software and associated documentation files (the "Software"), to deal
' in the Software without restriction, including without limitation the rights
' to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
' copies of the Software, and to permit persons to whom the Software is
' furnished to do so, subject to the following conditions:
' 
' The above copyright notice and this permission notice shall be
' included in all copies or substantial portions of the Software.
' 
' THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
' IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
' FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
' AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
' WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR
' IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
' 

' One click build tool.
' Double click it and the project compiles, nothing has to be set up first.
' Neither Eclipse nor a working JDK installation is needed.
' A missing JDK is downloaded and properly installed,
' Apache Ant is downloaded into %LOCALAPPDATA%\L2jMobius since only the build needs it.
' The finished zip is written to the build folder next to this script.

Option Explicit

Const ANT_VERSION = "1.10.18"
Const TITLE = "L2J Mobius Build"

Dim gFso, gShell, gStateFile, gHtaFile, gSentinelFile, gProgressOpen, gFailure
Dim gProjectDir, gProjectName, gBuildDir, gToolsDir, gBuildFile

Set gFso = CreateObject("Scripting.FileSystemObject")
Set gShell = CreateObject("WScript.Shell")
gProgressOpen = False

gProjectDir = gFso.GetParentFolderName(WScript.ScriptFullName)
gProjectName = gFso.GetFileName(gProjectDir)
gBuildDir = gFso.BuildPath(gProjectDir, "build")
' Ant is shared by every project, so it lives outside the build folders any one of them may wipe.
gToolsDir = gFso.BuildPath(gShell.ExpandEnvironmentStrings("%LOCALAPPDATA%"), "L2jMobius")
gBuildFile = gFso.BuildPath(gProjectDir, "build.xml")
gSentinelFile = gFso.BuildPath(gFso.GetSpecialFolder(2).Path, "l2jmobius_jdk_install.txt")

If Not gFso.FileExists(gBuildFile) Then
	MsgBox "build.xml was not found next to this script.", vbCritical, TITLE
	WScript.Quit 1
End If

' Any error raised below Main lands here, VBScript has no other way to catch it.
' Without this the progress window would keep polling forever and half downloads would stay behind.
On Error Resume Next
Main()
If Err.Number <> 0 Then
	gFailure = Err.Description
	On Error GoTo 0
	ProgressStop()
	RemoveLeftovers()
	MsgBox "Build stopped." & vbCrLf & vbCrLf & gFailure, vbCritical, TITLE
	WScript.Quit 1
End If
On Error GoTo 0

Sub Main()
	Dim required, jdkHome, antHome, logFile, binDir, zipFile, result

	required = RequiredJdk()

	jdkHome = FindJdk(required)
	If jdkHome = "" Then
		If MsgBox("JDK " & required & " was not found." & vbCrLf & vbCrLf & "Download and install Liberica JDK " & required & " (about 250 MB)?" & vbCrLf & vbCrLf & "Windows will ask for administrator rights. The JDK lands in Program Files and sets PATH and JAVA_HOME, so the game and login servers can run afterwards.", vbYesNo + vbQuestion, TITLE) <> vbYes Then
			WScript.Quit 1
		End If

		ProgressStart()
		jdkHome = InstallJdk(required)
	End If

	antHome = FindAnt()
	If antHome = "" Then
		If MsgBox("Apache Ant was not found." & vbCrLf & vbCrLf & "Download Apache Ant " & ANT_VERSION & " (about 9 MB) into" & vbCrLf & gToolsDir & "\ant ?", vbYesNo + vbQuestion, TITLE) <> vbYes Then
			WScript.Quit 1
		End If

		ProgressStart()
		antHome = InstallAnt()
	End If

	ProgressStart()
	ProgressSet 0, "Compiling " & gProjectName & " ..."

	logFile = gFso.BuildPath(gBuildDir, gProjectName & "-build.log")
	result = RunAnt(jdkHome, antHome, logFile)
	ProgressStop()

	' build.xml only cleans dist, the loose classes in bin are of no use once the zip exists.
	binDir = gFso.BuildPath(gBuildDir, "bin")
	If gFso.FolderExists(binDir) Then gFso.DeleteFolder binDir, True

	zipFile = gFso.BuildPath(gBuildDir, gProjectName & ".zip")
	If result = 0 And gFso.FileExists(zipFile) Then
		gFso.DeleteFile logFile
		MsgBox "Build finished." & vbCrLf & vbCrLf & zipFile, vbInformation, TITLE
	Else
		MsgBox "Build failed. The log is opened for inspection." & vbCrLf & vbCrLf & logFile, vbCritical, TITLE
		gShell.Run "notepad.exe " & Quoted(logFile), 1, False
	End If
End Sub

Function Quoted(value)
	Quoted = """" & value & """"
End Function

Function Trimmed(path)
	Trimmed = path
	Do While Len(Trimmed) > 3 And Right(Trimmed, 1) = "\"
		Trimmed = Left(Trimmed, Len(Trimmed) - 1)
	Loop
End Function

Function ReadAllText(path)
	Dim stream
	Set stream = gFso.OpenTextFile(path, 1)
	ReadAllText = stream.ReadAll()
	stream.Close
End Function

' build.xml guards itself with a JDK<n>.present property, that n is the version to install.
Function RequiredJdk()
	Dim expression, matches
	' The fallback matches every project in the workspace, it only covers a reworded build.xml.
	RequiredJdk = "25"
	Set expression = New RegExp
	expression.Pattern = "JDK([0-9]+)\.present"
	Set matches = expression.Execute(ReadAllText(gBuildFile))
	If matches.Count > 0 Then RequiredJdk = matches(0).SubMatches(0)
End Function

' WScript.Shell.Run cannot hand back what a program printed, so the output detours through a temp file.
Function Capture(command)
	Dim tempFile
	Capture = ""
	tempFile = gFso.BuildPath(gFso.GetSpecialFolder(2).Path, gFso.GetTempName())
	gShell.Run "cmd /c " & Quoted(command & " > " & Quoted(tempFile) & " 2>&1"), 0, True
	If gFso.FileExists(tempFile) Then
		If gFso.GetFile(tempFile).Size > 0 Then Capture = ReadAllText(tempFile)
		gFso.DeleteFile tempFile
	End If
End Function

Function JdkFeature(home)
	Dim javac, expression, matches
	JdkFeature = 0
	javac = gFso.BuildPath(home, "bin\javac.exe")
	If Not gFso.FileExists(javac) Then Exit Function

	Set expression = New RegExp
	expression.Pattern = "([0-9]+)"
	Set matches = expression.Execute(Capture(Quoted(javac) & " -version"))
	If matches.Count > 0 Then JdkFeature = CLng(matches(0).SubMatches(0))
End Function

Function FindJdk(required)
	Dim candidates, home, index, lines
	FindJdk = ""
	candidates = Array(gShell.ExpandEnvironmentStrings("%JAVA_HOME%"))
	For index = 0 To UBound(candidates)
		home = Trimmed(candidates(index))
		If home <> "" And InStr(home, "%") = 0 Then
			If JdkFeature(home) >= CLng(required) Then
				FindJdk = home
				Exit Function
			End If
		End If
	Next

	lines = Split(Replace(Capture("where javac"), vbCr, ""), vbLf)
	For index = 0 To UBound(lines)
		If InStr(LCase(lines(index)), "javac.exe") > 0 Then
			home = gFso.GetParentFolderName(gFso.GetParentFolderName(lines(index)))
			If JdkFeature(home) >= CLng(required) Then
				FindJdk = home
				Exit Function
			End If
		End If
	Next
End Function

Function FindAnt()
	Dim candidates, home, index, lines
	FindAnt = ""
	candidates = Array(gFso.BuildPath(gToolsDir, "ant"), gShell.ExpandEnvironmentStrings("%ANT_HOME%"))
	For index = 0 To UBound(candidates)
		home = Trimmed(candidates(index))
		If home <> "" And InStr(home, "%") = 0 Then
			If gFso.FileExists(gFso.BuildPath(home, "bin\ant.bat")) Then
				FindAnt = home
				Exit Function
			End If
		End If
	Next

	lines = Split(Replace(Capture("where ant.bat"), vbCr, ""), vbLf)
	For index = 0 To UBound(lines)
		If InStr(LCase(lines(index)), "ant.bat") > 0 Then
			FindAnt = gFso.GetParentFolderName(gFso.GetParentFolderName(lines(index)))
			Exit Function
		End If
	Next
End Function

Sub EnsureFolder(path)
	If gFso.FolderExists(path) Then Exit Sub

	EnsureFolder gFso.GetParentFolderName(path)
	gFso.CreateFolder path
End Sub

Function Megabytes(bytes)
	Megabytes = FormatNumber(bytes / 1048576, 1) & " MB"
End Function

' Pulls the file in range requests, so the progress bar can move while it downloads.
Sub Download(url, destination, label)
	Dim http, stream, total, received, chunkSize, status, passes, value
	chunkSize = 4194304
	Set http = CreateObject("MSXML2.ServerXMLHTTP.6.0")

	' A HEAD that fails or answers without a length is no reason to stop, the bar just pulses instead.
	total = 0
	On Error Resume Next
	http.Open "HEAD", url, False
	http.Send
	If http.Status = 200 Then
		value = http.getResponseHeader("Content-Length")
		If IsNumeric(value) Then total = CDbl(value)
	End If
	On Error GoTo 0

	Set stream = CreateObject("ADODB.Stream")
	stream.Type = 1
	stream.Open

	received = 0
	passes = 0
	Do
		http.Open "GET", url, False
		If total > 0 Then http.setRequestHeader "Range", "bytes=" & CStr(received) & "-" & CStr(received + chunkSize - 1)
		http.Send

		status = http.Status
		If status <> 200 And status <> 206 Then Err.Raise 5, "Download", "HTTP " & status & " for " & url

		stream.Write http.responseBody
		received = stream.Size
		passes = passes + 1

		If total > 0 Then
			ProgressSet Int(received * 100 / total), label & " " & Megabytes(received) & " / " & Megabytes(total)
		Else
			ProgressSet (passes * 7) Mod 100, label & " " & Megabytes(received)
		End If

		If status = 200 Then Exit Do
	Loop While received < total

	If gFso.FileExists(destination) Then gFso.DeleteFile destination
	stream.SaveToFile destination, 2
	stream.Close
End Sub

' Both archives carry everything under one versioned top folder, that folder becomes the target.
Function Unpack(archive, targetDir)
	Dim stageDir, child, extracted, tar
	stageDir = targetDir & ".stage"
	If gFso.FolderExists(stageDir) Then gFso.DeleteFolder stageDir, True
	gFso.CreateFolder stageDir

	' The full path keeps a GNU tar on the PATH from taking over, that one cannot read zip files.
	tar = gShell.ExpandEnvironmentStrings("%SystemRoot%\System32\tar.exe")
	gShell.Run Quoted(tar) & " -xf " & Quoted(archive) & " -C " & Quoted(stageDir), 0, True

	extracted = stageDir
	For Each child In gFso.GetFolder(stageDir).SubFolders
		extracted = child.Path
		Exit For
	Next

	If extracted = stageDir And gFso.GetFolder(stageDir).Files.Count = 0 Then Err.Raise 5, "Unpack", "Nothing was extracted from " & archive

	If gFso.FolderExists(targetDir) Then gFso.DeleteFolder targetDir, True
	gFso.MoveFolder extracted, targetDir
	If gFso.FolderExists(stageDir) Then gFso.DeleteFolder stageDir, True

	Unpack = targetDir
End Function

' The Liberica release index answers in plain text, newest build first.
' Liberica publishes no ARM64 build for Windows, so x64 it is, that one runs under emulation.
Function LibericaUrl(required)
	Dim http, query, lines
	query ="https://api.bell-sw.com/v1/liberica/releases?version-feature=" & required & "&version-modifier=latest&os=windows&arch=x86&bitness=64&package-type=msi&bundle-type=jdk&output=text&fields=downloadUrl"

	Set http = CreateObject("MSXML2.ServerXMLHTTP.6.0")
	http.Open "GET", query, False
	http.Send
	If http.Status <> 200 Then Err.Raise 5, "LibericaUrl", "HTTP " & http.Status & " from the Liberica release index"

	lines = Split(Replace(http.responseText, vbCr, ""), vbLf)
	LibericaUrl = Trim(lines(0))
	If LibericaUrl = "" Then Err.Raise 5, "LibericaUrl", "No Liberica JDK " & required & " build is listed for Windows"
End Function

' A real install, not an unpacked folder, so the JDK survives a cleaned workspace and the servers keep running.
' msiexec needs administrator rights, which means ShellExecute with the runas verb. That call cannot be
' waited on, so the elevated shell writes the exit code to a sentinel file and this side watches for it.
Function InstallJdk(required)
	Dim installer, arguments, waited, code
	installer = gFso.BuildPath(gToolsDir, "jdk.msi")

	EnsureFolder gToolsDir
	Download LibericaUrl(required), installer, "Downloading JDK " & required & " ..."
	If gFso.FileExists(gSentinelFile) Then gFso.DeleteFile gSentinelFile

	' The installer offers all of its features, naming three leaves out the jar association and the JavaSoft keys.
	arguments = "/v:on /c msiexec.exe /i " & Quoted(installer) & " /qn /norestart ADDLOCAL=LibericaInstallation,SetupPath,SetupJavaHome & echo !errorlevel!> " & Quoted(gSentinelFile)

	ProgressSet 0, "Installing JDK " & required & " ..."
	On Error Resume Next
	CreateObject("Shell.Application").ShellExecute "cmd.exe", arguments, "", "runas", 0
	If Err.Number <> 0 Then
		Err.Clear
		On Error GoTo 0
		Err.Raise 5, "InstallJdk", "The JDK installer needs administrator rights and the request was turned down."
	End If
	On Error GoTo 0

	waited = 0
	Do While Not gFso.FileExists(gSentinelFile)
		WScript.Sleep 500
		waited = waited + 500
		If waited > 1200000 Then Err.Raise 5, "InstallJdk", "The JDK installer is still running after 20 minutes."
		ProgressSet (waited \ 1000 * 3) Mod 100, "Installing JDK " & required & " ..."
	Loop

	' Trim leaves the line break that echo appends, and a stray one would read as a failed install.
	code = Trim(Replace(Replace(ReadAllText(gSentinelFile), vbCr, ""), vbLf, ""))
	gFso.DeleteFile gSentinelFile
	gFso.DeleteFile installer
	If code <> "0" Then Err.Raise 5, "InstallJdk", "msiexec ended with code " & code

	' The installer wrote JAVA_HOME for the whole machine, this process still carries the old environment.
	InstallJdk = Trimmed(gShell.RegRead("HKLM\SYSTEM\CurrentControlSet\Control\Session Manager\Environment\JAVA_HOME"))
End Function

Function InstallAnt()
	Dim label, archive, failed
	label = "Downloading Apache Ant " & ANT_VERSION & " ..."
	archive = gFso.BuildPath(gToolsDir, "ant.zip")
	EnsureFolder gToolsDir

	failed = False
	On Error Resume Next
	Download "https://dlcdn.apache.org/ant/binaries/apache-ant-" & ANT_VERSION & "-bin.zip", archive, label
	If Err.Number <> 0 Then
		failed = True
		Err.Clear
	End If
	On Error GoTo 0

	' The dlcdn mirror only carries current releases, the archive mirror carries every one.
	If failed Then Download "https://archive.apache.org/dist/ant/binaries/apache-ant-" & ANT_VERSION & "-bin.zip", archive, label

	ProgressSet 100, "Extracting Apache Ant " & ANT_VERSION & " ..."
	InstallAnt = Unpack(archive, gFso.BuildPath(gToolsDir, "ant"))
	gFso.DeleteFile archive
End Function

' A failed run can stop between a download and its cleanup, so the next run would find these half done.
Sub RemoveLeftovers()
	Dim path
	On Error Resume Next
	For Each path In Array(gFso.BuildPath(gToolsDir, "jdk.msi"), gFso.BuildPath(gToolsDir, "ant.zip"), gSentinelFile)
		If gFso.FileExists(path) Then gFso.DeleteFile path
	Next
	path = gFso.BuildPath(gToolsDir, "ant.stage")
	If gFso.FolderExists(path) Then gFso.DeleteFolder path, True
	On Error GoTo 0
End Sub

Function RunAnt(jdkHome, antHome, logFile)
	Dim command
	EnsureFolder gBuildDir

	gShell.Environment("PROCESS")("JAVA_HOME") = jdkHome
	gShell.Environment("PROCESS")("ANT_HOME") = antHome

	' build.xml aims at ..\build, the command line wins over the property it declares.
	command = Quoted(gFso.BuildPath(antHome, "bin\ant.bat")) & " -Dbuild=" & Quoted(gBuildDir) & " -buildfile " & Quoted(gBuildFile) & " > " & Quoted(logFile) & " 2>&1"
	RunAnt = gShell.Run("cmd /c " & Quoted(command), 0, True)
End Function

Sub ProgressStart()
	Dim tempDir
	If gProgressOpen Then Exit Sub

	tempDir = gFso.GetSpecialFolder(2).Path
	gStateFile = gFso.BuildPath(tempDir, "l2jmobius_build_state.txt")
	gHtaFile = gFso.BuildPath(tempDir, "l2jmobius_build_progress.hta")

	ProgressSet 0, "Starting ..."
	WriteProgressWindow()
	gShell.Run "mshta.exe " & Quoted(gHtaFile), 1, False
	gProgressOpen = True
End Sub

Sub ProgressWrite(line)
	Dim stream
	On Error Resume Next
	Set stream = gFso.CreateTextFile(gStateFile, True)
	stream.WriteLine line
	stream.Close
	On Error GoTo 0
End Sub

Sub ProgressSet(percent, label)
	Dim value
	value = percent
	If value < 0 Then value = 0
	If value > 100 Then value = 100
	ProgressWrite CStr(value) & "|" & label
End Sub

Sub ProgressStop()
	If Not gProgressOpen Then Exit Sub

	ProgressWrite "-1|done"
	WScript.Sleep 500
	gProgressOpen = False

	On Error Resume Next
	gFso.DeleteFile gStateFile
	gFso.DeleteFile gHtaFile
	On Error GoTo 0
End Sub

' The window polls the state file, so it keeps updating while this script works.
' A tilde stands in for a double quote, the whole page is written with one Replace.
Sub WriteProgressWindow()
	Dim lines, stream, index
	lines = Array( _
		"<html>", _
		"<head>", _
		"<title>" & TITLE & "</title>", _
		"<HTA:APPLICATION id=~app~ border=~thin~ innerBorder=~no~ maximizeButton=~no~ minimizeButton=~no~ scroll=~no~ sysMenu=~no~ showInTaskBar=~yes~ />", _
		"<style>", _
		"body { font-family: Segoe UI, Tahoma, sans-serif; font-size: 12px; background: #f0f0f0; margin: 16px; }", _
		"#lbl { margin-bottom: 10px; }", _
		"#track { border: 1px solid #808080; background: #ffffff; height: 18px; }", _
		"#bar { background: #2d7dd2; height: 18px; width: 0%; }", _
		"</style>", _
		"</head>", _
		"<body>", _
		"<div id=~lbl~>Starting ...</div>", _
		"<div id=~track~><div id=~bar~></div></div>", _
		"<script language=~VBScript~>", _
		"Const STATE = ~" & gStateFile & "~", _
		"Sub Tick()", _
		"	Dim fso, stream, line, parts, percent", _
		"	On Error Resume Next", _
		"	Set fso = CreateObject(~Scripting.FileSystemObject~)", _
		"	If Not fso.FileExists(STATE) Then Exit Sub", _
		"	Set stream = fso.OpenTextFile(STATE, 1)", _
		"	line = stream.ReadLine()", _
		"	stream.Close", _
		"	If Err.Number <> 0 Then Exit Sub", _
		"	parts = Split(line, ~|~)", _
		"	percent = CLng(parts(0))", _
		"	If percent < 0 Then", _
		"		window.close", _
		"		Exit Sub", _
		"	End If", _
		"	document.getElementById(~bar~).style.width = percent & ~%~", _
		"	document.getElementById(~lbl~).innerText = parts(1)", _
		"End Sub", _
		"Sub Window_OnLoad()", _
		"	window.resizeTo 470, 175", _
		"	window.moveTo (screen.availWidth - 470) \ 2, (screen.availHeight - 175) \ 2", _
		"	window.setInterval ~Tick()~, 200, ~VBScript~", _
		"End Sub", _
		"</script>", _
		"</body>", _
		"</html>")

	Set stream = gFso.CreateTextFile(gHtaFile, True)
	For index = 0 To UBound(lines)
		stream.WriteLine Replace(lines(index), "~", """")
	Next
	stream.Close
End Sub
