# 1. Weg: Run configuration von IDE
![[Pasted image 20240527215737.png]]
-> in der jeweiligen Klasse: Modify Run Configuration

![[Pasted image 20240527215819.png]]
-> in Program arguments die Parameter von der Klasse eingeben
-> auf Run klicken

# 2. Weg: Konsole
1. sbt (baue das Projekt)
2. project Tools (navigiere zu dem Ordner, wo die auszuführende Klasse ist)
3. runMain org.opalj.support.info.Purity -a -b -c (mit runMain die Klasse ausfürhren)
![[Pasted image 20240527220147.png]]
