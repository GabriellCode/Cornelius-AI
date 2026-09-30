Set WshShell = CreateObject("WScript.Shell")
WshShell.Run "javaw -Xms1024m -Xmx4096m -Dfile.encoding=UTF-8 -jar """ & Replace(WScript.ScriptFullName, WScript.ScriptName, "") & "cornelius.jar""", 0, False
