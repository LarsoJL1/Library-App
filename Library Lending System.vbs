Option Explicit

Dim shell, fileSystem, projectFolder, command, result
Set shell = CreateObject("WScript.Shell")
Set fileSystem = CreateObject("Scripting.FileSystemObject")

projectFolder = fileSystem.GetParentFolderName(WScript.ScriptFullName)
shell.CurrentDirectory = projectFolder
command = "cmd.exe /c """ & projectFolder & "\run.bat"""
result = shell.Run(command, 0, True)

If result <> 0 Then
    MsgBox "The library app could not start. Check that Java is installed and " & _
           "lib\sqlite-jdbc.jar exists in the project folder, then try again.", _
           vbExclamation, "Library Lending System"
End If